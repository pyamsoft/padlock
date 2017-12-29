/*
 *     Copyright (C) 2017 Peter Kenji Yamanaka
 *
 *     This program is free software; you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation; either version 2 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License along
 *     with this program; if not, write to the Free Software Foundation, Inc.,
 *     51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
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
import com.pyamsoft.padlock.lock.passed.LockPassed
import com.pyamsoft.pydroid.data.Optional
import com.pyamsoft.pydroid.data.Optional.Present
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
        private val appContext: Context,
        private val lockPassed: LockPassed,
        private val lockHelper: LockHelper,
        private val preferences: LockScreenPreferences,
        private val jobSchedulerCompat: JobSchedulerCompat,
        private val masterPinInteractor: MasterPinInteractor, private val dbInsert: PadLockDBInsert,
        private val dbUpdate: PadLockDBUpdate,
        private val dbQuery: PadLockDBQuery,
        @param:Named(
                "recheck") private val recheckServiceClass: Class<out IntentService>) :
        LockEntryInteractor {

    private val failCount: MutableMap<String, Int> = HashMap()

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
                            return@map Optional.ofNullable(null)
                        }

                        if (lockCode == null) {
                            Timber.d("No app specific code, use Master PIN")
                            return@map it
                        } else {
                            Timber.d("App specific code present, compare attempt")
                            return@map lockCode.asOptional()
                        }
                    }.flatMap innerFlat@ {
                        if (it is Present) {
                            return@innerFlat lockHelper.checkSubmissionAttempt(currentAttempt,
                                    it.value)
                        } else {
                            Timber.e("Cannot submit against PIN which is NULL")
                            return@innerFlat Single.just(false)
                        }
                    }
                }

    }

    @CheckResult private fun whitelistEntry(packageName: String, activityName: String,
            realName: String, lockCode: String?, isSystem: Boolean): Completable {
        Timber.d("Whitelist entry for %s %s (real %s)", packageName, activityName, realName)
        return dbInsert.insert(packageName, realName, lockCode, 0, 0, isSystem, true)
    }

    @CheckResult private fun queueRecheckJob(
            packageName: String, realName: String, recheckTime: Long): Completable {
        return Completable.fromAction {
            // Cancel any old recheck job for the class, but not the package
            val intent = Intent(appContext, recheckServiceClass)
            intent.putExtra(Recheck.EXTRA_PACKAGE_NAME, packageName)
            intent.putExtra(Recheck.EXTRA_CLASS_NAME, realName)
            jobSchedulerCompat.cancel(intent)

            // Queue up a new recheck job
            // Since alarms are inexact, buffer by an extra minute
            jobSchedulerCompat.queue(intent,
                    System.currentTimeMillis() + recheckTime + ONE_MINUTE_MILLIS)
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

    override fun postUnlock(packageName: String,
            activityName: String, realName: String, lockCode: String?,
            isSystem: Boolean, whitelist: Boolean, ignoreTime: Long): Completable {
        return Completable.defer {
            val ignoreMillis = TimeUnit.MINUTES.toMillis(ignoreTime)
            val whitelistObservable: Completable
            val ignoreObservable: Completable
            val recheckObservable: Completable

            // Whitelist
            if (whitelist) {
                whitelistObservable = whitelistEntry(packageName, activityName, realName, lockCode,
                        isSystem)
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

            return@defer ignoreObservable.andThen(recheckObservable).andThen(whitelistObservable)
        }.andThen(Completable.fromAction {
            failCount[getFailId(packageName, activityName)] = 0
        }).andThen(Completable.fromAction {
            lockPassed.add(packageName, activityName)
        })
    }

    @CheckResult private fun getFailId(packageName: String, activityName: String): String =
            "$packageName|$activityName"

    companion object {
        const val DEFAULT_MAX_FAIL_COUNT: Int = 2
        private val ONE_MINUTE_MILLIS = TimeUnit.MINUTES.toMillis(1L)
    }
}

