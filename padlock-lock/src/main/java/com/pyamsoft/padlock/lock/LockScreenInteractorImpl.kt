/*
 * Copyright 2019 Peter Kenji Yamanaka
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.pyamsoft.padlock.lock

import androidx.annotation.CheckResult
import com.pyamsoft.padlock.api.MasterPinInteractor
import com.pyamsoft.padlock.api.database.EntryInsertDao
import com.pyamsoft.padlock.api.database.EntryQueryDao
import com.pyamsoft.padlock.api.database.EntryUpdateDao
import com.pyamsoft.padlock.api.lockscreen.LockHelper
import com.pyamsoft.padlock.api.lockscreen.LockPassed
import com.pyamsoft.padlock.api.lockscreen.LockScreenInteractor
import com.pyamsoft.padlock.api.packagemanager.PackageLabelManager
import com.pyamsoft.padlock.api.preferences.LockScreenPreferences
import com.pyamsoft.padlock.api.service.JobSchedulerCompat
import com.pyamsoft.padlock.api.service.JobSchedulerCompat.JobType.RECHECK
import com.pyamsoft.padlock.model.LockScreenType
import com.pyamsoft.padlock.model.LockWhitelistedEvent
import com.pyamsoft.padlock.model.service.Recheck
import com.pyamsoft.pydroid.core.bus.Publisher
import com.pyamsoft.pydroid.core.optional.Optional
import com.pyamsoft.pydroid.core.optional.Optional.Present
import com.pyamsoft.pydroid.core.optional.asOptional
import com.pyamsoft.pydroid.core.threads.Enforcer
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Single
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class LockScreenInteractorImpl @Inject internal constructor(
  private val enforcer: Enforcer,
  private val lockWhitelistedBus: Publisher<LockWhitelistedEvent>,
  private val lockPassed: LockPassed,
  private val lockHelper: LockHelper,
  private val preferences: LockScreenPreferences,
  private val jobSchedulerCompat: JobSchedulerCompat,
  private val masterPinInteractor: MasterPinInteractor,
  private val insertDao: EntryInsertDao,
  private val updateDao: EntryUpdateDao,
  private val queryDao: EntryQueryDao,
  private val labelManager: PackageLabelManager
) : LockScreenInteractor {

  private val failCount: MutableMap<String, Int> = HashMap()

  override fun submitPin(
    packageName: String,
    activityName: String,
    lockCode: String?,
    currentAttempt: String
  ): Single<Boolean> {
    return queryDao.queryWithPackageActivityName(packageName, activityName)
        .flatMap { model ->
          enforcer.assertNotOnMainThread()
          val lockUntilTime = model.lockUntilTime()
          return@flatMap masterPinInteractor.getMasterPin()
              .map { masterPinOptional ->
                Timber.d("Attempt unlock: %s %s", packageName, activityName)
                Timber.d("Check entry is not locked: %d", lockUntilTime)
                if (System.currentTimeMillis() < lockUntilTime) {
                  Timber.e("Entry is still locked. Fail unlock")
                  return@map Optional.ofNullable(null)
                }

                return@map when (lockCode) {
                  null -> masterPinOptional
                  else -> lockCode.asOptional()
                }
              }
        }
        .flatMap { pinOptional ->
          enforcer.assertNotOnMainThread()

          if (pinOptional is Present) {
            Timber.d("Check submission attempt")
            return@flatMap lockHelper.checkSubmissionAttempt(currentAttempt, pinOptional.value)
          } else {
            Timber.e("No pin available, always fail")
            return@flatMap Single.just(false)
          }
        }
  }

  @CheckResult
  private fun whitelistEntry(
    packageName: String,
    activityName: String,
    realName: String,
    lockCode: String?,
    isSystem: Boolean
  ): Completable {
    enforcer.assertNotOnMainThread()
    Timber.d("Whitelist entry for %s %s (real %s)", packageName, activityName, realName)
    return insertDao.insert(packageName, realName, lockCode, 0, 0, isSystem, true)
  }

  @CheckResult
  private fun queueRecheckJob(
    packageName: String,
    realName: String,
    recheckTime: Long
  ): Completable {
    return Completable.fromAction {
      enforcer.assertNotOnMainThread()

      val params: Map<String, String> = mapOf(
          Recheck.EXTRA_PACKAGE_NAME to packageName,
          Recheck.EXTRA_CLASS_NAME to realName
      )

      // Cancel any old recheck job for the class, but not the package
      jobSchedulerCompat.cancel(RECHECK, params)

      // Queue up a new recheck job
      // Since alarms are inexact, buffer by an extra minute
      val triggerTime = recheckTime + ONE_MINUTE_MILLIS

      jobSchedulerCompat.queue(RECHECK, triggerTime, params)
    }
  }

  @CheckResult
  private fun ignoreEntryForTime(
    ignoreMinutesInMillis: Long,
    packageName: String,
    activityName: String
  ): Completable {
    return Completable.defer {
      enforcer.assertNotOnMainThread()
      val newIgnoreTime = System.currentTimeMillis() + ignoreMinutesInMillis
      Timber.d(
          "Ignore %s %s until %d (for %d)", packageName, activityName, newIgnoreTime,
          ignoreMinutesInMillis
      )

      // Add an extra second here to artificially de-bounce quick requests, like those commonly in multi window mode
      return@defer updateDao.updateIgnoreUntilTime(packageName, activityName, newIgnoreTime + 1000L)
    }
  }

  override fun lockOnFailure(
    packageName: String,
    activityName: String
  ): Single<Boolean> {
    return Single.fromCallable {
      enforcer.assertNotOnMainThread()
      val failId: String = getFailId(packageName, activityName)
      val newFailCount: Int = failCount.getOrPut(failId) { 0 } + 1
      failCount[failId] = newFailCount
      return@fromCallable newFailCount
    }
        .flatMap { count ->
          enforcer.assertNotOnMainThread()
          return@flatMap getTimeoutPeriodMinutesInMillis()
              .map { count to it }
        }
        .flatMap { (count, timeoutInMillis) ->
          enforcer.assertNotOnMainThread()
          return@flatMap lockEntry(count, timeoutInMillis, packageName, activityName)
        }
  }

  @CheckResult
  private fun getTimeoutPeriodMinutesInMillis(): Single<Long> {
    return Single.fromCallable {
      enforcer.assertNotOnMainThread()
      return@fromCallable preferences.getTimeoutPeriod()
    }
        .map { TimeUnit.MINUTES.toMillis(it) }
        .doOnSuccess { Timber.d("Current timeout period: $it") }
  }

  @CheckResult
  private fun lockEntry(
    currentFailCount: Int,
    timeOutMinutesInMillis: Long,
    packageName: String,
    activityName: String
  ): Single<Boolean> {
    return Single.defer {
      enforcer.assertNotOnMainThread()

      val shouldTimeOut = timeOutMinutesInMillis > 0
      val tooManyFailures = currentFailCount > DEFAULT_MAX_FAIL_COUNT
      val shouldLock = shouldTimeOut && tooManyFailures

      if (shouldLock) {
        val currentTime = System.currentTimeMillis()
        val newLockUntilTime = currentTime + timeOutMinutesInMillis
        Timber.d(
            "Lock $packageName $activityName until $newLockUntilTime ($timeOutMinutesInMillis)"
        )
        return@defer updateDao.updateLockUntilTime(packageName, activityName, newLockUntilTime)
            .andThen(Single.just(System.currentTimeMillis() < newLockUntilTime))
      } else {
        return@defer Single.just(false)
      }
    }
  }

  override fun getHint(): Single<String> {
    return masterPinInteractor.getHint()
        .map {
          enforcer.assertNotOnMainThread()
          if (it is Present) {
            return@map it.value
          } else {
            return@map ""
          }
        }
  }

  override fun clearFailCount() {
    failCount.clear()
  }

  override fun postUnlock(
    packageName: String,
    activityName: String,
    realName: String,
    lockCode: String?,
    isSystem: Boolean,
    whitelist: Boolean,
    ignoreTime: Long
  ): Completable {
    return Completable.defer {
      enforcer.assertNotOnMainThread()
      val ignoreMillis = TimeUnit.MINUTES.toMillis(ignoreTime)
      val whitelistObservable: Completable
      val ignoreObservable: Completable
      val recheckObservable: Completable

      // Whitelist
      if (whitelist) {
        whitelistObservable = whitelistEntry(
            packageName, activityName, realName, lockCode,
            isSystem
        )
      } else {
        whitelistObservable = Completable.complete()
      }

      // If time > 0, mark as ignored
      if (ignoreTime > 0) {
        ignoreObservable = ignoreEntryForTime(ignoreMillis, packageName, activityName)

        // If we are not whitelisting
        if (whitelist) {
          recheckObservable = Completable.complete()
        } else {
          recheckObservable = queueRecheckJob(packageName, realName, ignoreMillis)
        }
      } else {
        ignoreObservable = Completable.complete()
        recheckObservable = Completable.complete()
      }

      return@defer ignoreObservable.andThen(recheckObservable)
          .andThen(whitelistObservable)
    }
        .andThen(Completable.fromAction {
          enforcer.assertNotOnMainThread()
          failCount[getFailId(packageName, activityName)] = 0
        })
        .andThen(Completable.fromAction {
          enforcer.assertNotOnMainThread()
          lockPassed.add(packageName, activityName)
        })
        .doOnComplete {
          if (whitelist) {
            lockWhitelistedBus.publish(LockWhitelistedEvent(packageName, activityName))
          }
        }
  }

  @CheckResult
  private fun getFailId(
    packageName: String,
    activityName: String
  ): String = "$packageName|$activityName"

  override fun getLockScreenType(): Single<LockScreenType> =
    Single.fromCallable {
      enforcer.assertNotOnMainThread()
      return@fromCallable preferences.getCurrentLockType()
    }

  override fun getDefaultIgnoreTime(): Single<Long> =
    Single.fromCallable {
      enforcer.assertNotOnMainThread()
      return@fromCallable preferences.getDefaultIgnoreTime()
    }

  override fun getDisplayName(packageName: String): Single<String> = Single.defer {
    enforcer.assertNotOnMainThread()
    return@defer labelManager.loadPackageLabel(packageName)
  }

  override fun isAlreadyUnlocked(
    packageName: String,
    activityName: String
  ): Maybe<Unit> {
    return Maybe.defer<Unit> {
      enforcer.assertNotOnMainThread()
      if (lockPassed.check(packageName, activityName)) {
        return@defer Maybe.just(Unit)
      } else {
        return@defer Maybe.empty()
      }
    }
  }

  companion object {
    const val DEFAULT_MAX_FAIL_COUNT: Int = 2
    private val ONE_MINUTE_MILLIS = TimeUnit.MINUTES.toMillis(1L)
  }
}
