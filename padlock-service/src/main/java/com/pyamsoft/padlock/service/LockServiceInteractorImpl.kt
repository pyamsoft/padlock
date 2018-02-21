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

package com.pyamsoft.padlock.service

import android.app.IntentService
import android.support.annotation.CheckResult
import com.pyamsoft.padlock.api.*
import com.pyamsoft.padlock.model.Excludes
import com.pyamsoft.padlock.model.ForegroundEvent
import com.pyamsoft.padlock.model.PadLockEntry
import com.pyamsoft.padlock.model.RecheckStatus
import com.pyamsoft.padlock.model.RecheckStatus.FORCE
import com.pyamsoft.padlock.model.db.PadLockEntryModel
import com.pyamsoft.pydroid.optional.Optional.Present
import com.pyamsoft.pydroid.optional.Optionals
import com.pyamsoft.pydroid.optional.asOptional
import io.reactivex.*
import timber.log.Timber
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit.MILLISECONDS
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
internal class LockServiceInteractorImpl @Inject internal constructor(
    private val usageEventProvider: UsageEventProvider,
    private val deviceLockStateProvider: DeviceLockStateProvider,
    private val lockPassed: LockPassed,
    private val preferences: LockScreenPreferences,
    private val jobSchedulerCompat: JobSchedulerCompat,
    private val packageActivityManager: PackageActivityManager,
    private val padLockDBQuery: PadLockDBQuery,
    @param:Named("recheck") private val recheckServiceClass: Class<out IntentService>,
    private val stateInteractor: LockServiceStateInteractor
) :
    LockServiceInteractor {

  private var activePackageName = ""
  private var activeClassName = ""
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
    return Flowable.interval(LISTEN_INTERVAL, MILLISECONDS)
        .map {
          val now: Long = System.currentTimeMillis()
          val beginTime = now - TEN_SECONDS_MILLIS
          val endTime = now + TEN_SECONDS_MILLIS
          return@map usageEventProvider.queryEvents(beginTime, endTime)
              .asOptional()
        }
        .onBackpressureDrop()
        .map {
          if (it is Present) {
            return@map it.value.createForegroundEvent { packageName, className ->
              ForegroundEvent(packageName, className)
            }
                .asOptional()
          }

          return@map Optionals.ofNullable(null)
        }
        .filter { it is Present }
        .map { it as Present }
        .map { it.value }
        .filter { !Excludes.isLockScreen(it.packageName, it.className) }
        .filter { !Excludes.isPackageExcluded(it.packageName) }
        .filter { !Excludes.isClassExcluded(it.className) }
        .filter { it != lastForegroundEvent }
        .doOnNext { lastForegroundEvent = it }
  }

  override fun isActiveMatching(
      packageName: String,
      className: String
  ): Single<Boolean> {
    return Single.fromCallable {
      Timber.d(
          "Check against current window values: %s, %s", activePackageName,
          activeClassName
      )
      // We can replace the actual passed classname with the stored classname because:
      // either it is equal to the passed name or the passed name is PACKAGE
      // which will respond to any class name
      return@fromCallable (activePackageName == packageName)
          && (activeClassName == className || className == PadLockEntry.PACKAGE_ACTIVITY_NAME)
    }
  }

  override fun cleanup() {
    Timber.d("Cleanup LockService")
    jobSchedulerCompat.cancel(recheckServiceClass)
  }

  @CheckResult
  private fun prepareLockScreen(
      packageName: String,
      activityName: String
  ): MaybeTransformer<Boolean, PadLockEntryModel> {
    return MaybeTransformer {
      it.flatMap {
        Timber.d(
            "Get list of locked classes with package: %s, class: %s", packageName,
            activityName
        )
        return@flatMap padLockDBQuery.queryWithPackageActivityNameDefault(
            packageName,
            activityName
        )
            .filter { !PadLockEntry.isEmpty(it) }
      }
    }
  }

  @CheckResult
  private fun filterOutInvalidEntries(): MaybeTransformer<PadLockEntryModel, PadLockEntryModel> {
    return MaybeTransformer {
      it.filter {
        val ignoreUntilTime: Long = it.ignoreUntilTime()
        val currentTime: Long = System.currentTimeMillis()
        Timber.d("Ignore until time: %d", ignoreUntilTime)
        Timber.d("Current time: %d", currentTime)
        return@filter currentTime >= ignoreUntilTime
      }
          .filter {
            if (PadLockEntry.PACKAGE_ACTIVITY_NAME == it.activityName() && it.whitelist()) {
              throw RuntimeException(
                  "PACKAGE entry for package: ${it.packageName()} cannot be whitelisted"
              )
            }

            Timber.d("Filter out whitelisted packages")
            return@filter !it.whitelist()
          }
    }
  }

  @CheckResult
  private fun getEntry(
      packageName: String,
      activityName: String
  ): SingleTransformer<Boolean, PadLockEntryModel> {
    return SingleTransformer {
      it.filter { it }
          .compose(prepareLockScreen(packageName, activityName))
          .compose(filterOutInvalidEntries())
          .toSingle(
              PadLockEntry.EMPTY
          )
    }
  }

  @CheckResult
  private fun isDeviceLocked(): Boolean = deviceLockStateProvider.isLocked()

  @CheckResult
  private fun isServiceEnabled(): Maybe<Boolean> {
    return stateInteractor.isServiceEnabled()
        .filter {
          if (!it) {
            Timber.e("Service is not user enabled, ignore event")
            resetState()
          }
          return@filter it
        }
        .doOnSuccess {
          if (isDeviceLocked()) {
            Timber.w("Device is locked, reset last")
            reset()
          }
        }
  }

  @CheckResult
  private fun isEventFromActivity(
      packageName: String,
      className: String
  ): MaybeTransformer<Boolean, Boolean> {
    return MaybeTransformer {
      it.isEmpty.filter {
        Timber.d("Filter if empty: $it")
        return@filter !it
      }
          .flatMap {
            Timber.d("Check event from activity: %s %s", packageName, className)
            return@flatMap packageActivityManager.isValidActivity(
                packageName,
                className
            )
                .filter {
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

  @CheckResult
  private fun isEventRestricted(
      packageName: String,
      className: String
  ): MaybeTransformer<Boolean, Boolean> {
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

  override fun processEvent(
      packageName: String,
      className: String,
      forcedRecheck: RecheckStatus
  ): Single<PadLockEntryModel> {
    val windowEventObservable: Single<Boolean> = isServiceEnabled().compose(
        isEventFromActivity(packageName, className)
    )
        .compose(
            isEventRestricted(packageName, className)
        )
        .doOnSuccess {
          activePackageName = packageName
          activeClassName = className
        }
        .toSingle(false)

    return windowEventObservable.map {
      if (!it) {
        Timber.e("Failed to pass window checking")
        return@map false
      }

      if (forcedRecheck === FORCE) {
        Timber.d("Pass filter via forced recheck")
      }

      return@map true
    }
        .compose(getEntry(packageName, className))
        .doOnSuccess {
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
