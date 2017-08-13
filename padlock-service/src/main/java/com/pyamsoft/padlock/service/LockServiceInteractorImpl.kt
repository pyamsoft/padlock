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

package com.pyamsoft.padlock.service

import android.app.Activity
import android.app.IntentService
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.support.annotation.CheckResult
import com.pyamsoft.padlock.base.db.PadLockDBQuery
import com.pyamsoft.padlock.base.db.PadLockEntry
import com.pyamsoft.padlock.base.preference.LockScreenPreferences
import com.pyamsoft.padlock.base.wrapper.JobSchedulerCompat
import com.pyamsoft.padlock.base.wrapper.PackageActivityManager
import com.pyamsoft.padlock.service.RecheckStatus.FORCE
import io.reactivex.Single
import io.reactivex.functions.Function4
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton internal class LockServiceInteractorImpl @Inject internal constructor(
    context: Context,
    private val preferences: LockScreenPreferences,
    private val jobSchedulerCompat: JobSchedulerCompat,
    private val packageActivityManager: PackageActivityManager,
    private val padLockDBQuery: PadLockDBQuery,
    private val lockScreenActivityClass: Class<out Activity>,
    private val recheckServiceClass: Class<out IntentService>,
    private val stateInteractor: LockServiceStateInteractor) : LockServiceInteractor {

  private val appContext = context.applicationContext
  private val keyguardManager = appContext.getSystemService(
      Context.KEYGUARD_SERVICE) as KeyguardManager
  private var lastPackageName = ""
  private var lastClassName = ""
  private var activePackageName = ""
  private var activeClassName = ""
  private val lockScreenPassed: MutableMap<String, Boolean> = HashMap()


  override fun reset() {
    Timber.i("Reset name state")
    lastPackageName = ""
    lastClassName = ""
    activeClassName = ""
    activePackageName = ""
    lockScreenPassed.clear()
  }

  override fun processActiveIfMatching(packageName: String, className: String): Single<Boolean> {
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
   * Return true if the window event is caused by a non-Activity
   */
  @CheckResult private fun isEventNotActivity(packageName: String,
      className: String): Single<Boolean> {
    Timber.d("Check event from activity: %s %s", packageName, className)
    return packageActivityManager.getActivityInfo(packageName, className).isEmpty
  }

  /**
   * If the screen has changed, update the last package.
   * This will prevent the lock screen from opening twice when the same
   * app opens multiple activities for example.
   */
  @CheckResult private fun hasNameChanged(name: String, oldName: String): Single<Boolean> {
    return Single.fromCallable { name != oldName }
  }

  @CheckResult private fun isWindowFromLockScreen(packageName: String,
      className: String): Single<Boolean> {
    return Single.fromCallable {
      val lockScreenPackageName = appContext.packageName
      val lockScreenClassName = lockScreenActivityClass.name
      Timber.d("Check if window is lock screen (%s %s)", lockScreenPackageName,
          lockScreenClassName)

      val isPackage = packageName == lockScreenPackageName
      return@fromCallable isPackage && className == lockScreenClassName
    }
  }

  @CheckResult private fun isOnlyLockOnPackageChange(): Single<Boolean> {
    return Single.fromCallable { preferences.isLockOnPackageChange() }
  }

  @CheckResult private fun getEntry(packageName: String,
      activityName: String): Single<PadLockEntry> {
    return padLockDBQuery.queryWithPackageActivityNameDefault(packageName, activityName)
  }

  @CheckResult private fun isRestrictedWhileLocked(): Single<Boolean> {
    return Single.fromCallable { preferences.isIgnoreInKeyguard() }
  }

  @CheckResult private fun isDeviceLocked(): Single<Boolean> {
    return Single.fromCallable {
      keyguardManager.inKeyguardRestrictedInputMode()
          || keyguardManager.isKeyguardLocked
    }
  }

  override fun processEvent(packageName: String, className: String,
      forcedRecheck: RecheckStatus): Single<PadLockEntry> {
    val windowEventObservable: Single<Boolean> = stateInteractor.isServiceEnabled()
        .filter {
          if (!it) {
            Timber.e("Service is not user enabled, ignore event")
            reset()
          }

          return@filter it
        }.flatMapSingle { enabled ->
      isDeviceLocked().map {
        if (it) {
          Timber.w("Device is locked, reset last")
          reset()
        }

        return@map enabled
      }
    }.flatMap { isEventNotActivity(packageName, className) }.filter {
      if (it) {
        Timber.w("Event not caused by activity.")
        Timber.w("P: %s, C: %s", packageName, className)
        Timber.w("Ignore")
      }

      return@filter !it
    }.flatMapSingle {
      isDeviceLocked().flatMap {
        if (it) isRestrictedWhileLocked() else Single.just(false)
      }
    }.filter {
      if (it) {
        Timber.w("Locking is restricted while device in keyguard.")
        Timber.w("P: %s, C: %s", packageName, className)
        Timber.w("Ignore")
      }
      return@filter !it
    }.flatMapSingle { isWindowFromLockScreen(packageName, className) }
        .filter {
          if (it) {
            Timber.w("Event is caused by lock screen")
            Timber.w("P: %s, C: %s", packageName, className)
            Timber.w("Ignore")
          }

          return@filter !it
        }.map {
      val passed: Boolean = !it
      activePackageName = packageName
      activeClassName = className
      return@map passed
    }.toSingle(false)


    return Single.zip(windowEventObservable, hasNameChanged(packageName, lastPackageName),
        hasNameChanged(className, lastClassName), isOnlyLockOnPackageChange(),
        Function4<Boolean, Boolean, Boolean, Boolean, Boolean> { windowEvent, packageChanged, classChanged, lockOnPackageChange ->
          if (!windowEvent) {
            Timber.e("Failed to pass window checking")
            return@Function4 false
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
          if (lockOnPackageChange) {
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

          return@Function4 windowHasChanged || !lockPassed
        }).filter { it }
        .flatMapSingle {
          Timber.d("Get list of locked classes with package: %s, class: %s", packageName, className)
          setLockScreenPassed(packageName, className, false)
          return@flatMapSingle getEntry(packageName, className)
        }.filter { !PadLockEntry.isEmpty(it) }
        .filter {
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
    }.toSingle(PadLockEntry.EMPTY)
  }
}