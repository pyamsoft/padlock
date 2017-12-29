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

package com.pyamsoft.padlock.service

import android.app.IntentService
import android.app.KeyguardManager
import android.app.usage.UsageEvents
import android.app.usage.UsageEvents.Event
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.support.annotation.CheckResult
import com.pyamsoft.padlock.base.Excludes
import com.pyamsoft.padlock.base.db.PadLockDBQuery
import com.pyamsoft.padlock.base.db.PadLockEntry
import com.pyamsoft.padlock.base.preference.LockScreenPreferences
import com.pyamsoft.padlock.base.wrapper.JobSchedulerCompat
import com.pyamsoft.padlock.base.wrapper.PackageActivityManager
import com.pyamsoft.padlock.lock.ForegroundEvent
import com.pyamsoft.padlock.lock.passed.LockPassed
import com.pyamsoft.padlock.service.RecheckStatus.FORCE
import com.pyamsoft.pydroid.data.Optional
import com.pyamsoft.pydroid.data.Optional.Present
import com.pyamsoft.pydroid.helper.asOptional
import io.reactivex.Flowable
import io.reactivex.Maybe
import io.reactivex.MaybeTransformer
import io.reactivex.Single
import io.reactivex.SingleTransformer
import timber.log.Timber
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit.MILLISECONDS
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton internal class LockServiceInteractorImpl @Inject internal constructor(
        context: Context,
        private val lockPassed: LockPassed,
        private val preferences: LockScreenPreferences,
        private val jobSchedulerCompat: JobSchedulerCompat,
        private val packageActivityManager: PackageActivityManager,
        private val padLockDBQuery: PadLockDBQuery,
        @param:Named("recheck") private val recheckServiceClass: Class<out IntentService>,
        private val stateInteractor: LockServiceStateInteractor) : LockServiceInteractor {

    private val appContext = context.applicationContext
    private val keyguardManager = appContext.getSystemService(
            Context.KEYGUARD_SERVICE) as KeyguardManager
    private var activePackageName = ""
    private var activeClassName = ""
    private val usageManager: UsageStatsManager = appContext.getSystemService(
            Context.USAGE_STATS_SERVICE) as UsageStatsManager
    private var lastForegroundEvent = ForegroundEvent.EMPTY

    override fun reset() {
        resetState()

        // Also reset last foreground
        lastForegroundEvent = ForegroundEvent.EMPTY
    }

    override fun clearMatchingForegroundEvent(event: ForegroundEvent) {
        Timber.d("Received foreground event: $event")
        if (lastForegroundEvent == event) {
            Timber.d("LockScreen reported last foreground event was cleared.")
            lastForegroundEvent = ForegroundEvent.EMPTY
        }
    }

    private fun resetState() {
        Timber.i("Reset name state")
        activeClassName = ""
        activePackageName = ""
    }

    /**
     * Take care to avoid any calls to logging methods as it will run every 200 ms and flood
     */
    override fun listenForForegroundEvents(): Flowable<ForegroundEvent> {
        return Flowable.interval(LISTEN_INTERVAL, MILLISECONDS).map {
            val now: Long = System.currentTimeMillis()
            val beginTime = now - TEN_SECONDS_MILLIS
            val endTime = now + TEN_SECONDS_MILLIS
            return@map usageManager.queryEvents(beginTime, endTime).asOptional()
        }.onBackpressureDrop()
                .map {
                    val event: UsageEvents.Event = Event()
                    if (it is Present) {
                        // We have usage events
                        val events = it.value
                        if (events.hasNextEvent()) {
                            events.getNextEvent(event)
                            while (events.hasNextEvent()) {
                                events.getNextEvent(event)
                            }

                            if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                                return@map ForegroundEvent(
                                        event.packageName ?: "",
                                        event.className ?: "").asOptional()
                            }
                        }
                    }

                    return@map Optional.ofNullable(null)
                }.filter { it is Present }.map { it as Present }.map { it.value }
                .filter { !Excludes.isLockScreen(it.packageName, it.className) }
                .filter { !Excludes.isPackageExcluded(it.packageName) }
                .filter { !Excludes.isClassExcluded(it.className) }
                .filter { it != lastForegroundEvent }
                .doOnNext { lastForegroundEvent = it }
    }

    override fun isActiveMatching(packageName: String, className: String): Single<Boolean> {
        return Single.fromCallable {
            Timber.d("Check against current window values: %s, %s", activePackageName,
                    activeClassName)
            // We can replace the actual passed classname with the stored classname because:
            // either it is equal to the passed name or the passed name is PACKAGE
            // which will respond to any class name
            return@fromCallable (activePackageName == packageName)
                    && (activeClassName == className || className == PadLockEntry.PACKAGE_ACTIVITY_NAME)
        }
    }

    override fun cleanup() {
        Timber.d("Cleanup LockService")
        val intent = Intent(appContext, recheckServiceClass)
        jobSchedulerCompat.cancel(intent)
    }

    @CheckResult private fun prepareLockScreen(packageName: String,
            activityName: String): MaybeTransformer<Boolean, PadLockEntry> {
        return MaybeTransformer {
            it.flatMap {
                Timber.d("Get list of locked classes with package: %s, class: %s", packageName,
                        activityName)
                return@flatMap padLockDBQuery.queryWithPackageActivityNameDefault(packageName,
                        activityName).filter { !PadLockEntry.isEmpty(it) }
            }
        }
    }

    @CheckResult private fun filterOutInvalidEntries(): MaybeTransformer<PadLockEntry, PadLockEntry> {
        return MaybeTransformer {
            it.filter {
                val ignoreUntilTime: Long = it.ignoreUntilTime()
                val currentTime: Long = System.currentTimeMillis()
                Timber.d("Ignore until time: %d", ignoreUntilTime)
                Timber.d("Current time: %d", currentTime)
                return@filter currentTime >= ignoreUntilTime
            }.filter {
                if (PadLockEntry.PACKAGE_ACTIVITY_NAME == it.activityName() && it.whitelist()) {
                    throw RuntimeException(
                            "PACKAGE entry for package: ${it.packageName()} cannot be whitelisted")
                }

                Timber.d("Filter out whitelisted packages")
                return@filter !it.whitelist()
            }
        }
    }

    @CheckResult private fun getEntry(packageName: String,
            activityName: String): SingleTransformer<Boolean, PadLockEntry> {
        return SingleTransformer {
            it.filter { it }
                    .compose(prepareLockScreen(packageName, activityName))
                    .compose(filterOutInvalidEntries()).toSingle(PadLockEntry.EMPTY)
        }
    }

    @CheckResult private fun isDeviceLocked(): Boolean {
        return keyguardManager.inKeyguardRestrictedInputMode()
                || keyguardManager.isKeyguardLocked
    }

    @CheckResult private fun isServiceEnabled(): Maybe<Boolean> {
        return stateInteractor.isServiceEnabled()
                .filter {
                    if (!it) {
                        Timber.e("Service is not user enabled, ignore event")
                        resetState()
                    }
                    return@filter it
                }.doOnSuccess {
            if (isDeviceLocked()) {
                Timber.w("Device is locked, reset last")
                reset()
            }
        }
    }

    @CheckResult private fun isEventFromActivity(packageName: String,
            className: String): MaybeTransformer<Boolean, Boolean> {
        return MaybeTransformer {
            it.isEmpty.filter {
                Timber.d("Filter if empty: $it")
                return@filter !it
            }.flatMap {
                Timber.d("Check event from activity: %s %s", packageName, className)
                return@flatMap packageActivityManager.isValidActivity(packageName,
                        className).filter {
                    if (!it) {
                        Timber.w("Event not caused by activity.")
                        Timber.w("P: %s, C: %s", packageName, className)
                        Timber.w("Ignore")
                    }

                    return@filter it
                }
            }
        }
    }

    @CheckResult private fun isEventRestricted(packageName: String,
            className: String): MaybeTransformer<Boolean, Boolean> {
        return MaybeTransformer {
            it.filter {
                val restrict: Boolean = preferences.isIgnoreInKeyguard() && isDeviceLocked()
                if (restrict) {
                    Timber.w("Locking is restricted while device in keyguard.")
                    Timber.w("P: %s, C: %s", packageName, className)
                    Timber.w("Ignore")
                }
                return@filter !restrict
            }
        }
    }

    override fun processEvent(packageName: String, className: String,
            forcedRecheck: RecheckStatus): Single<PadLockEntry> {
        val windowEventObservable: Single<Boolean> = isServiceEnabled().compose(
                isEventFromActivity(packageName, className)).compose(
                isEventRestricted(packageName, className))
                .doOnSuccess {
                    activePackageName = packageName
                    activeClassName = className
                }.toSingle(false)

        return windowEventObservable.map {
            if (!it) {
                Timber.e("Failed to pass window checking")
                return@map false
            }

            if (forcedRecheck === FORCE) {
                Timber.d("Pass filter via forced recheck")
            }

            return@map true
        }.compose(getEntry(packageName, className)).doOnSuccess {
            if (!PadLockEntry.isEmpty(it)) {
                lockPassed.remove(it.packageName(), it.activityName())
            }
        }
    }

    companion object {
        private const val LISTEN_INTERVAL = 300L
        private val TEN_SECONDS_MILLIS = TimeUnit.SECONDS.toMillis(10L)
    }
}