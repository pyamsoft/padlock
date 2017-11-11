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

import android.app.Activity
import android.app.IntentService
import android.app.KeyguardManager
import android.app.usage.UsageEvents
import android.app.usage.UsageEvents.Event
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.support.annotation.CheckResult
import com.pyamsoft.padlock.base.db.PadLockDBQuery
import com.pyamsoft.padlock.base.db.PadLockEntry
import com.pyamsoft.padlock.base.preference.LockScreenPreferences
import com.pyamsoft.padlock.base.wrapper.JobSchedulerCompat
import com.pyamsoft.padlock.base.wrapper.PackageActivityManager
import com.pyamsoft.padlock.service.RecheckStatus.FORCE
import com.pyamsoft.pydroid.bus.EventBus
import com.pyamsoft.pydroid.helper.Optional
import com.pyamsoft.pydroid.helper.Optional.Present
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
    private val preferences: LockScreenPreferences,
    private val jobSchedulerCompat: JobSchedulerCompat,
    private val packageActivityManager: PackageActivityManager,
    private val padLockDBQuery: PadLockDBQuery,
    @param:Named("lockscreen") private val lockScreenActivityClass: Class<out Activity>,
    @param:Named("recheck") private val recheckServiceClass: Class<out IntentService>,
    private val serviceFinishBus: EventBus<ServiceFinishEvent>,
    private val stateInteractor: LockServiceStateInteractor) : LockServiceInteractor {

  private val appContext = context.applicationContext
  private val keyguardManager = appContext.getSystemService(
      Context.KEYGUARD_SERVICE) as KeyguardManager
  private var lastPackageName = ""
  private var lastClassName = ""
  private var activePackageName = ""
  private var activeClassName = ""
  private val lockScreenPassed: MutableMap<String, Boolean> = HashMap()
  private val usageManager: UsageStatsManager = appContext.getSystemService(
      Context.USAGE_STATS_SERVICE) as UsageStatsManager

  override fun reset() {
    Timber.i("Reset name state")
    lastPackageName = ""
    lastClassName = ""
    activeClassName = ""
    activePackageName = ""
    lockScreenPassed.clear()
  }

  override fun listenForForegroundEvents(): Flowable<ForegroundEvent> {
    return Flowable.interval(LISTEN_INTERVAL, MILLISECONDS).map {
      if (UsagePermissionChecker.missingUsageStatsPermission(appContext)) {
        Timber.e("We are missing permission to continue, stop listening for events")
        serviceFinishBus.publish(ServiceFinishEvent)
        return@map Optional.asOptional(null)
      } else {
        val now: Long = System.currentTimeMillis()
        val beginTime = now - TEN_SECONDS_MILLIS
        val endTime = now + TEN_SECONDS_MILLIS
        return@map usageManager.queryEvents(beginTime, endTime).asOptional()
      }
    }.onBackpressureDrop()
        .map {
          val foregroundEvent: Optional<ForegroundEvent>
          val event: UsageEvents.Event = Event()
          if (it is Present) {
            // We have usage events
            val events = it.value
            if (events.hasNextEvent()) {
              events.getNextEvent(event)
              while (events.hasNextEvent()) {
                events.getNextEvent(event)
              }

              Timber.d(
                  "Final Event: ${event.packageName} ${event.className} ${parseType(
                      event.eventType)}")
              if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                foregroundEvent = ForegroundEvent(event.packageName ?: "",
                    event.className ?: "").asOptional()
              } else {
                foregroundEvent = Optional.asOptional(null)
              }
            } else {
              foregroundEvent = Optional.asOptional(null)
            }
          } else {
            foregroundEvent = Optional.asOptional(null)
          }
          return@map foregroundEvent
        }.filter { it is Present }.map { it as Present }.map { it.value }
  }

  @CheckResult
  private fun parseType(type: Int): String = when (type) {
    UsageEvents.Event.MOVE_TO_FOREGROUND -> "MOVE_TO_FOREGROUND"
    UsageEvents.Event.MOVE_TO_BACKGROUND -> "MOVE_TO_BACKGROUND"
    UsageEvents.Event.CONFIGURATION_CHANGE -> "CONFIGURATION_CHANGE"
    UsageEvents.Event.SHORTCUT_INVOCATION -> "SHORTCUT_INVOCATION"
    UsageEvents.Event.USER_INTERACTION -> "USER_INTERACTION"
    UsageEvents.Event.NONE -> "NONE"
    else -> "INVALID"
  }

  override fun isActiveMatching(packageName: String, className: String): Single<Boolean> {
    return Single.fromCallable {
      Timber.d("Check against current window values: %s, %s", activePackageName, activeClassName)
      // We can replace the actual passed classname with the stored classname because:
      // either it is equal to the passed name or the passed name is PACKAGE
      // which will respond to any class name
      return@fromCallable (activePackageName == packageName)
          && (activeClassName == className || className == PadLockEntry.PACKAGE_ACTIVITY_NAME)
    }
  }

  override fun setLockScreenPassed(packageName: String, className: String, passed: Boolean) {
    Timber.d("Set lockScreenPassed: %s, %s, [%s]", packageName, className, passed)
    lockScreenPassed.put(packageName + className, passed)
  }

  override fun cleanup() {
    Timber.d("Cleanup LockService")
    val intent = Intent(appContext, recheckServiceClass)
    jobSchedulerCompat.cancel(intent)
  }

  /**
   * If the screen has changed, update the last package.
   * This will prevent the lock screen from opening twice when the same
   * app opens multiple activities for example.
   */
  @CheckResult private fun hasNameChanged(name: String, oldName: String): Boolean = name != oldName

  @CheckResult private fun isOnlyLockOnPackageChange(): Boolean =
      preferences.isLockOnPackageChange()

  @CheckResult private fun prepareLockScreen(windowPackage: String,
      windowActivity: String): MaybeTransformer<Boolean, PadLockEntry> {
    return MaybeTransformer {
      it.flatMap {
        Timber.d("Get list of locked classes with package: %s, class: %s", windowPackage,
            windowActivity)
        setLockScreenPassed(windowPackage, windowActivity, false)
        return@flatMap padLockDBQuery.queryWithPackageActivityNameDefault(windowPackage,
            windowActivity).filter { PadLockEntry.isEmpty(it).not() }
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
        return@filter it.whitelist().not()
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
          if (it.not()) {
            Timber.e("Service is not user enabled, ignore event")
            reset()
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
        return@filter it.not()
      }.flatMap {
        Timber.d("Check event from activity: %s %s", packageName, className)
        return@flatMap packageActivityManager.isValidActivity(packageName, className).filter {
          if (it.not()) {
            Timber.w("Event not caused by activity.")
            Timber.w("P: %s, C: %s", packageName, className)
            Timber.w("Ignore")
          }

          return@filter it
        }
      }
    }
  }

  @CheckResult private fun isWindowFromLockScreen(windowPackage: String,
      windowActivity: String): Boolean {
    val lockScreenPackage: String = appContext.packageName
    val lockScreenActivity: String = lockScreenActivityClass.name
    Timber.d("Check if window is lock screen (%s %s)", lockScreenPackage,
        lockScreenActivity)

    val isPackage = (windowPackage == lockScreenPackage)
    return isPackage && (windowActivity == lockScreenActivity)
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
        return@filter restrict.not()
      }.filter {
        val isLockScreen: Boolean = isWindowFromLockScreen(packageName, className)
        if (isLockScreen) {
          Timber.w("Event is caused by lock screen")
          Timber.w("P: %s, C: %s", packageName, className)
          Timber.w("Ignore")
        }
        return@filter isLockScreen.not()
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
      val packageChanged: Boolean = hasNameChanged(packageName, lastPackageName)
      val classChanged: Boolean = hasNameChanged(className, lastClassName)
      val lockOnPackageChanged: Boolean = isOnlyLockOnPackageChange()
      if (it.not()) {
        Timber.e("Failed to pass window checking")
        return@map false
      }

      if (packageChanged) {
        Timber.d("Last Package: %s - New Package: %s", lastPackageName, packageName)
        lastPackageName = packageName
      }

      if (classChanged) {
        Timber.d("Last Class: %s - New Class: %s", lastClassName, className)
        lastClassName = className
      }

      var windowHasChanged: Boolean = classChanged
      if (lockOnPackageChanged) {
        windowHasChanged = windowHasChanged && packageChanged
      }

      if (forcedRecheck === FORCE) {
        Timber.d("Pass filter via forced recheck")
        windowHasChanged = true
      }

      var lockPassed: Boolean? = lockScreenPassed[packageName + className]
      if (lockPassed == null) {
        Timber.w("No lock map entry exists for: %s, %s", packageName, className)
        Timber.w("default to False")
        lockPassed = false
      }

      return@map windowHasChanged || lockPassed.not()
    }.compose(getEntry(packageName, className))
  }

  companion object {
    private const val LISTEN_INTERVAL = 300L
    private val TEN_SECONDS_MILLIS = TimeUnit.SECONDS.toMillis(10L)
  }
}