/*
 * Copyright 2017 Peter Kenji Yamanaka
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
 */

package com.pyamsoft.padlock.lock

import android.app.IntentService
import android.content.Context
import android.content.Intent
import android.support.annotation.CheckResult
import com.pyamsoft.padlock.base.db.PadLockDBInsert
import com.pyamsoft.padlock.base.db.PadLockDBQuery
import com.pyamsoft.padlock.base.db.PadLockDBUpdate
import com.pyamsoft.padlock.base.preference.LockScreenPreferences
import com.pyamsoft.padlock.base.wrapper.JobSchedulerCompat
import com.pyamsoft.padlock.lock.helper.LockHelper
import com.pyamsoft.padlock.lock.master.MasterPinInteractor
import com.pyamsoft.pydroid.helper.asOptional
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Single
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton internal class LockEntryInteractorImpl @Inject internal constructor(
    context: Context,
    private val lockHelper: LockHelper,
    private val preferences: LockScreenPreferences,
    private val jobSchedulerCompat: JobSchedulerCompat,
    private val masterPinInteractor: MasterPinInteractor, private val dbInsert: PadLockDBInsert,
    private val dbUpdate: PadLockDBUpdate,
    private val dbQuery: PadLockDBQuery,
    @param:Named(
        "recheck") private val recheckServiceClass: Class<out IntentService>) : LockEntryInteractor {

  private val appContext = context.applicationContext
  private var failCount: MutableMap<String, Int> = HashMap()

  override fun submitPin(packageName: String, activityName: String, lockCode: String?,
      currentAttempt: String): Single<Boolean> {
    return dbQuery.queryWithPackageActivityNameDefault(packageName, activityName)
        .flatMap {
          val lockUntilTime = it.lockUntilTime()
          masterPinInteractor.getMasterPin().map {
            Timber.d("Attempt unlock: %s %s", packageName, activityName)
            Timber.d("Check entry is not locked: %d", lockUntilTime)
            if (System.currentTimeMillis() < lockUntilTime) {
              Timber.e("Entry is still locked. Fail unlock")
              return@map null.asOptional()
            }

            if (lockCode == null) {
              Timber.d("No app specific code, use Master PIN")
              return@map it
            } else {
              Timber.d("App specific code present, compare attempt")
              return@map lockCode.asOptional()
            }
          }.flatMap innerFlat@ {
            val value = it.get()
            if (value == null) {
              Timber.e("Cannot submit against PIN which is NULL")
              return@innerFlat Single.just(false)
            } else {
              return@innerFlat lockHelper.checkSubmissionAttempt(currentAttempt, value)
            }
          }
        }

  }

  @CheckResult private fun whitelistEntry(
      packageName: String, activityName: String, realName: String,
      lockCode: String?, isSystem: Boolean): Completable {
    Timber.d("Whitelist entry for %s %s (real %s)", packageName, activityName, realName)
    return dbInsert.insert(packageName, realName, lockCode, 0, 0, isSystem, true)
  }

  @CheckResult private fun queueRecheckJob(
      packageName: String, activityName: String, recheckTime: Long): Completable {
    return Completable.fromAction {
      // Cancel any old recheck job for the class, but not the package
      val intent = Intent(appContext, recheckServiceClass)
      intent.putExtra(Recheck.EXTRA_PACKAGE_NAME, packageName)
      intent.putExtra(Recheck.EXTRA_CLASS_NAME, activityName)
      jobSchedulerCompat.cancel(intent)

      // Queue up a new recheck job
      jobSchedulerCompat.queue(intent, System.currentTimeMillis() + recheckTime)
    }
  }

  @CheckResult private fun ignoreEntryForTime(
      ignoreMinutesInMillis: Long, packageName: String, activityName: String): Completable {
    return Completable.defer {
      val newIgnoreTime = System.currentTimeMillis() + ignoreMinutesInMillis
      Timber.d("Ignore %s %s until %d (for %d)", packageName, activityName, newIgnoreTime,
          ignoreMinutesInMillis)

      // Add an extra second here to artificially de-bounce quick requests, like those commonly in multi window mode
      return@defer dbUpdate.updateIgnoreTime(newIgnoreTime + 1000L, packageName, activityName)
    }
  }

  override fun lockEntryOnFail(packageName: String, activityName: String): Maybe<Long> {
    return Single.fromCallable {
      val failId: String = getFailId(packageName, activityName)
      val newFailCount: Int = failCount.getOrPut(failId, { 0 }) + 1
      failCount[failId] = newFailCount
      return@fromCallable newFailCount
    }.filter { it > DEFAULT_MAX_FAIL_COUNT }.flatMap { getTimeoutPeriodMinutesInMillis() }
        .filter { it > 0 }.flatMap { lockEntry(it, packageName, activityName) }
  }

  @CheckResult
  private fun getTimeoutPeriodMinutesInMillis(): Maybe<Long> {
    return Maybe.fromCallable { preferences.getTimeoutPeriod() }.map {
      TimeUnit.MINUTES.toMillis(it)
    }.doOnSuccess {
      Timber.d("Current timeout period: $it")
    }
  }

  @CheckResult private fun lockEntry(
      timeOutMinutesInMillis: Long, packageName: String, activityName: String): Maybe<Long> {
    return Maybe.defer {
      val currentTime = System.currentTimeMillis()
      val newLockUntilTime = currentTime + timeOutMinutesInMillis
      Timber.d("Lock %s %s until %d (%d)", packageName, activityName, newLockUntilTime,
          timeOutMinutesInMillis)

      return@defer dbUpdate.updateLockTime(newLockUntilTime, packageName, activityName)
          .andThen(Maybe.just(newLockUntilTime))
    }
  }

  override fun getHint(): Single<String> {
    return masterPinInteractor.getHint().map {
      val value = it.get()
      if (value == null) {
        return@map ""
      } else {
        return@map value
      }
    }
  }

  override fun clearFailCount() {
    failCount.clear()
  }

  override fun postUnlock(packageName: String,
      activityName: String, realName: String, lockCode: String?,
      isSystem: Boolean, shouldExclude: Boolean, ignoreTime: Long): Completable {
    return Completable.defer {
      val ignoreMinutesInMillis = TimeUnit.MINUTES.toMillis(ignoreTime)
      val whitelistObservable: Completable
      val ignoreObservable: Completable
      val recheckObservable: Completable

      if (shouldExclude) {
        whitelistObservable = whitelistEntry(packageName, activityName, realName, lockCode,
            isSystem)
        ignoreObservable = Completable.complete()
        recheckObservable = Completable.complete()
      } else {
        whitelistObservable = Completable.complete()
        ignoreObservable = ignoreEntryForTime(ignoreMinutesInMillis, packageName, activityName)
        recheckObservable = queueRecheckJob(packageName, activityName, ignoreMinutesInMillis)
      }

      return@defer ignoreObservable.andThen(recheckObservable).andThen(whitelistObservable)
    }.andThen(Completable.fromAction {
      failCount[getFailId(packageName, activityName)] = 0
    })
  }

  @CheckResult private fun getFailId(packageName: String, activityName: String): String =
      "$packageName|$activityName"

  companion object {
    const val DEFAULT_MAX_FAIL_COUNT: Int = 2
  }
}

