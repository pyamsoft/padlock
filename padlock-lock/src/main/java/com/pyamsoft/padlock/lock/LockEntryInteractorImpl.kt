/*
 * Copyright (C) 2018 Peter Kenji Yamanaka
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pyamsoft.padlock.lock

import android.app.IntentService
import android.support.annotation.CheckResult
import com.pyamsoft.padlock.api.*
import com.pyamsoft.padlock.model.Recheck
import com.pyamsoft.pydroid.optional.Optional.Present
import com.pyamsoft.pydroid.optional.Optionals
import com.pyamsoft.pydroid.optional.asOptional
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Single
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
internal class LockEntryInteractorImpl @Inject internal constructor(
    private val lockPassed: LockPassed,
    private val lockHelper: LockHelper,
    private val preferences: LockScreenPreferences,
    private val jobSchedulerCompat: JobSchedulerCompat,
    private val masterPinInteractor: MasterPinInteractor,
    private val dbInsert: PadLockDBInsert,
    private val dbUpdate: PadLockDBUpdate,
    private val dbQuery: PadLockDBQuery,
    @param:Named("recheck") private val recheckServiceClass: Class<out IntentService>
) :
    LockEntryInteractor {

  private val failCount: MutableMap<String, Int> = HashMap()

  override fun submitPin(
      packageName: String,
      activityName: String,
      lockCode: String?,
      currentAttempt: String
  ): Single<Boolean> {
    return dbQuery.queryWithPackageActivityNameDefault(packageName, activityName)
        .flatMap {
          val lockUntilTime = it.lockUntilTime()
          return@flatMap masterPinInteractor.getMasterPin()
              .map {
                Timber.d("Attempt unlock: %s %s", packageName, activityName)
                Timber.d("Check entry is not locked: %d", lockUntilTime)
                if (System.currentTimeMillis() < lockUntilTime) {
                  Timber.e("Entry is still locked. Fail unlock")
                  return@map Optionals.ofNullable(null)
                }

                return@map when (lockCode) {
                  null -> it
                  else -> lockCode.asOptional()
                }
              }
        }
        .flatMap {
          return@flatMap when (it) {
            is Present -> lockHelper.checkSubmissionAttempt(
                currentAttempt,
                it.value
            )
            else -> Single.just(false)
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
    Timber.d("Whitelist entry for %s %s (real %s)", packageName, activityName, realName)
    return dbInsert.insert(packageName, realName, lockCode, 0, 0, isSystem, true)
  }

  @CheckResult
  private fun queueRecheckJob(
      packageName: String,
      realName: String,
      recheckTime: Long
  ): Completable {
    return Completable.fromAction {
      // Cancel any old recheck job for the class, but not the package
      val params: List<Pair<String, String>> = ArrayList<Pair<String, String>>().apply {
        add(Recheck.EXTRA_PACKAGE_NAME to packageName)
        add(Recheck.EXTRA_CLASS_NAME to realName)
      }
      jobSchedulerCompat.cancel(recheckServiceClass, params)

      // Queue up a new recheck job
      // Since alarms are inexact, buffer by an extra minute
      val triggerTime = System.currentTimeMillis() + recheckTime + ONE_MINUTE_MILLIS
      jobSchedulerCompat.queue(recheckServiceClass, params, triggerTime)
    }
  }

  @CheckResult
  private fun ignoreEntryForTime(
      ignoreMinutesInMillis: Long,
      packageName: String,
      activityName: String
  ): Completable {
    return Completable.defer {
      val newIgnoreTime = System.currentTimeMillis() + ignoreMinutesInMillis
      Timber.d(
          "Ignore %s %s until %d (for %d)", packageName, activityName, newIgnoreTime,
          ignoreMinutesInMillis
      )

      // Add an extra second here to artificially de-bounce quick requests, like those commonly in multi window mode
      return@defer dbUpdate.updateIgnoreTime(packageName, activityName, newIgnoreTime + 1000L)
    }
  }

  override fun lockEntryOnFail(
      packageName: String,
      activityName: String
  ): Maybe<Long> {
    return Single.fromCallable {
      val failId: String = getFailId(packageName, activityName)
      val newFailCount: Int = failCount.getOrPut(failId, { 0 }) + 1
      failCount[failId] = newFailCount
      return@fromCallable newFailCount
    }
        .filter { it > DEFAULT_MAX_FAIL_COUNT }
        .flatMap { getTimeoutPeriodMinutesInMillis() }
        .filter { it > 0 }
        .flatMap { lockEntry(it, packageName, activityName) }
  }

  @CheckResult
  private fun getTimeoutPeriodMinutesInMillis(): Maybe<Long> {
    return Maybe.fromCallable { preferences.getTimeoutPeriod() }
        .map {
          TimeUnit.MINUTES.toMillis(it)
        }
        .doOnSuccess {
          Timber.d("Current timeout period: $it")
        }
  }

  @CheckResult
  private fun lockEntry(
      timeOutMinutesInMillis: Long,
      packageName: String,
      activityName: String
  ): Maybe<Long> {
    return Maybe.defer {
      val currentTime = System.currentTimeMillis()
      val newLockUntilTime = currentTime + timeOutMinutesInMillis
      Timber.d(
          "Lock %s %s until %d (%d)", packageName, activityName, newLockUntilTime,
          timeOutMinutesInMillis
      )

      return@defer dbUpdate.updateLockTime(packageName, activityName, newLockUntilTime)
          .andThen(Maybe.just(newLockUntilTime))
    }
  }

  override fun getHint(): Single<String> {
    return masterPinInteractor.getHint()
        .map {
          return@map when (it) {
            is Present -> it.value
            else -> ""
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
          failCount[getFailId(packageName, activityName)] = 0
        })
        .andThen(Completable.fromAction {
          lockPassed.add(packageName, activityName)
        })
  }

  @CheckResult
  private fun getFailId(
      packageName: String,
      activityName: String
  ): String =
      "$packageName|$activityName"

  companion object {
    const val DEFAULT_MAX_FAIL_COUNT: Int = 2
    private val ONE_MINUTE_MILLIS = TimeUnit.MINUTES.toMillis(1L)
  }
}
