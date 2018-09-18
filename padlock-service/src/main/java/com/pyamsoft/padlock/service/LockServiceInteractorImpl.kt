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
import android.content.Context
import androidx.annotation.CheckResult
import com.pyamsoft.padlock.api.database.EntryQueryDao
import com.pyamsoft.padlock.api.lockscreen.LockPassed
import com.pyamsoft.padlock.api.packagemanager.PackageActivityManager
import com.pyamsoft.padlock.api.packagemanager.PackageApplicationManager
import com.pyamsoft.padlock.api.preferences.MasterPinPreferences
import com.pyamsoft.padlock.api.preferences.ServicePreferences
import com.pyamsoft.padlock.api.service.JobSchedulerCompat
import com.pyamsoft.padlock.api.service.LockServiceInteractor
import com.pyamsoft.padlock.api.service.LockServiceInteractor.ServiceState
import com.pyamsoft.padlock.api.service.LockServiceInteractor.ServiceState.DISABLED
import com.pyamsoft.padlock.api.service.LockServiceInteractor.ServiceState.ENABLED
import com.pyamsoft.padlock.api.service.LockServiceInteractor.ServiceState.PAUSED
import com.pyamsoft.padlock.api.service.LockServiceInteractor.ServiceState.PERMISSION
import com.pyamsoft.padlock.api.service.ScreenStateObserver
import com.pyamsoft.padlock.api.service.UsageEventProvider
import com.pyamsoft.padlock.model.Excludes
import com.pyamsoft.padlock.model.ForegroundEvent
import com.pyamsoft.padlock.model.db.PadLockDbModels
import com.pyamsoft.padlock.model.db.PadLockEntryModel
import com.pyamsoft.padlock.model.service.RecheckStatus
import com.pyamsoft.padlock.model.service.RecheckStatus.FORCE
import com.pyamsoft.padlock.service.device.UsagePermissionChecker
import com.pyamsoft.pydroid.core.optional.Optional
import com.pyamsoft.pydroid.core.optional.Optional.Present
import com.pyamsoft.pydroid.core.optional.asOptional
import com.pyamsoft.pydroid.core.threads.Enforcer
import io.reactivex.Flowable
import io.reactivex.Maybe
import io.reactivex.MaybeTransformer
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import io.reactivex.Single
import io.reactivex.SingleTransformer
import timber.log.Timber
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit.MILLISECONDS
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
internal class LockServiceInteractorImpl @Inject internal constructor(
  private val enforcer: Enforcer,
  private val context: Context,
  private val screenStateObserver: ScreenStateObserver,
  private val usageEventProvider: UsageEventProvider,
  private val lockPassed: LockPassed,
  private val jobSchedulerCompat: JobSchedulerCompat,
  private val packageActivityManager: PackageActivityManager,
  private val packageApplicationManager: PackageApplicationManager,
  private val queryDao: EntryQueryDao,
  @param:Named("recheck") private val recheckServiceClass: Class<out IntentService>,
  private val pinPreferences: MasterPinPreferences,
  private val servicePreferences: ServicePreferences
) : LockServiceInteractor {

  private var activePackageName = ""
  private var activeClassName = ""
  private var lastForegroundEvent = ForegroundEvent.EMPTY

  override fun init() {
    reset()
  }

  override fun cleanup() {
    Timber.d("Cleanup LockService")
    jobSchedulerCompat.cancel(recheckServiceClass)
    reset()
  }

  private fun reset() {
    resetState()

    // Also reset last foreground
    lastForegroundEvent = ForegroundEvent.EMPTY
  }

  override fun pauseService(paused: Boolean) {
    servicePreferences.setPaused(paused)
  }

  @CheckResult
  private fun decideServiceEnabledState(): ServiceState {
    if (servicePreferences.isPaused()) {
      Timber.d("Service is paused")
      return PAUSED
    } else if (UsagePermissionChecker.hasPermission(context)) {
      if (pinPreferences.getMasterPassword().isNullOrEmpty()) {
        Timber.d("Service is disabled")
        return DISABLED
      } else {
        Timber.d("Service is enabled")
        return ENABLED
      }
    } else {
      Timber.d("Service lacks permission")
      return PERMISSION
    }
  }

  override fun observeServiceState(): Observable<ServiceState> {
    return Observable.defer<ServiceState> {
      enforcer.assertNotOnMainThread()
      return@defer Observable.create { emitter ->
        val usageWatcher = usageEventProvider.watchPermission {
          Timber.d("Usage permission changed")
          emit(emitter, decideServiceEnabledState())
        }

        val pausedState = servicePreferences.watchPausedState {
          Timber.d("Paused changed")
          emit(emitter, decideServiceEnabledState())
        }

        val masterPinPresence = pinPreferences.watchPinPresence {
          Timber.d("Pin presence changed")
          emit(emitter, decideServiceEnabledState())
        }

        emitter.setCancellable {
          usageWatcher.stopWatching()
          pausedState.stopWatching()
          masterPinPresence.stopWatching()
        }

        enforcer.assertNotOnMainThread()
        Timber.d("Watching service state")

        // Don't emit an event here or we receive double pause events
      }
    }
  }

  override fun isServiceEnabled(): Single<ServiceState> {
    return Single.fromCallable {
      enforcer.assertNotOnMainThread()
      return@fromCallable decideServiceEnabledState()
    }
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

  private fun <T : Any> emit(
    emitter: ObservableEmitter<T>,
    value: T
  ) {
    if (!emitter.isDisposed) {
      emitter.onNext(value)
    }
  }

  override fun observeScreenState(): Observable<Boolean> {
    return Observable.defer<Boolean> {
      enforcer.assertNotOnMainThread()
      return@defer Observable.create { emitter ->
        emitter.setCancellable {
          screenStateObserver.unregister()
        }

        // Observe screen state changes
        screenStateObserver.register {
          emit(emitter, it)
        }

        // Start with screen as ON
        enforcer.assertNotOnMainThread()
        emit(emitter, true)
      }
    }
  }

  /**
   * Take care to avoid any calls to logging methods as it will run every 200 ms and flood
   *
   * TODO this should watch an event bus where interval checks usage permission on interval
   */
  override fun listenForForegroundEvents(): Flowable<ForegroundEvent> {
    return Flowable.interval(LISTEN_INTERVAL_MILLIS, MILLISECONDS)
        .map {
          enforcer.assertNotOnMainThread()
          val now = System.currentTimeMillis()
          // Watch from a period of time before this exact millisecond
          val beginTime = now - QUERY_SPAN_MILLIS

          // End the query in the future - this will make sure that any
          // delays caused by threading or whatnot will be handled and
          // seems to speed up responsiveness.
          val endTime = now + QUERY_FUTURE_OFFSET_MILLIS
          return@map usageEventProvider.queryEvents(beginTime, endTime)
              .asOptional()
        }
        .onErrorReturn {
          Timber.e(it, "Error while querying usage events")
          return@onErrorReturn Optional.ofNullable(null)
        }
        .onBackpressureDrop()
        .map {
          if (it is Present) {
            return@map it.value.createForegroundEvent { packageName, className ->
              ForegroundEvent(packageName, className)
            }
                .asOptional()
          }

          return@map Optional.ofNullable(null)
        }
        .filter { it is Present }
        .map { it as Present }
        .map { it.value }
        .filter { !Excludes.isLockScreen(it.packageName, it.className) }
        .filter { !Excludes.isPackageExcluded(it.packageName) }
        .filter { !Excludes.isClassExcluded(it.className) }
        .filter { it != lastForegroundEvent }
        .doOnNext { lastForegroundEvent = it }
        .onErrorReturn {
          Timber.e(it, "Error listening to foreground events")
          return@onErrorReturn ForegroundEvent.EMPTY
        }
  }

  override fun ifActiveMatching(
    packageName: String,
    className: String
  ): Maybe<Unit> {
    return Single.fromCallable {
      enforcer.assertNotOnMainThread()
      Timber.d(
          "Check against current window values: %s, %s", activePackageName,
          activeClassName
      )
      // We can replace the actual passed classname with the stored classname because:
      // either it is equal to the passed name or the passed name is PACKAGE
      // which will respond to any class name
      return@fromCallable (activePackageName == packageName)
          && (activeClassName == className || className == PadLockDbModels.PACKAGE_ACTIVITY_NAME)
    }
        .filter { it }
        .map { Unit }
  }

  @CheckResult
  private fun prepareLockScreen(
    packageName: String,
    activityName: String
  ): MaybeTransformer<Boolean, PadLockEntryModel> {
    return MaybeTransformer { source ->
      return@MaybeTransformer source.flatMap { _ ->
        enforcer.assertNotOnMainThread()
        Timber.d("Get locked with package: %s, class: %s", packageName, activityName)
        return@flatMap queryDao.queryWithPackageActivityName(packageName, activityName)
            .filter { !PadLockDbModels.isEmpty(it) }
      }
    }
  }

  @CheckResult
  private fun filterOutInvalidEntries(): MaybeTransformer<PadLockEntryModel, PadLockEntryModel> {
    return MaybeTransformer { source ->
      return@MaybeTransformer source.filter {
        enforcer.assertNotOnMainThread()
        val ignoreUntilTime: Long = it.ignoreUntilTime()
        val currentTime: Long = System.currentTimeMillis()
        Timber.d("Ignore until time: %d", ignoreUntilTime)
        Timber.d("Current time: %d", currentTime)
        return@filter currentTime >= ignoreUntilTime
      }
          .filter {
            if (PadLockDbModels.PACKAGE_ACTIVITY_NAME == it.activityName() && it.whitelist()) {
              throw RuntimeException("PACKAGE entry: ${it.packageName()} cannot be whitelisted")
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
    return SingleTransformer { source ->
      return@SingleTransformer source.filter {
        enforcer.assertNotOnMainThread()
        return@filter it
      }
          .compose(prepareLockScreen(packageName, activityName))
          .compose(filterOutInvalidEntries())
          .toSingle(PadLockDbModels.EMPTY)
    }
  }

  @CheckResult
  private fun serviceEnabled(): Maybe<Boolean> {
    return Maybe.defer {
      enforcer.assertNotOnMainThread()
      return@defer isServiceEnabled()
          .map { it == ENABLED }
          .filter {
            enforcer.assertNotOnMainThread()
            if (!it) {
              Timber.e("Service is not user enabled, ignore event")
              resetState()
            }
            return@filter it
          }
    }
  }

  @CheckResult
  private fun isEventFromActivity(
    packageName: String,
    className: String
  ): MaybeTransformer<Boolean, Boolean> {
    return MaybeTransformer { source ->
      return@MaybeTransformer source.isEmpty
          .filter {
            enforcer.assertNotOnMainThread()
            return@filter !it
          }
          .flatMap { _ ->
            enforcer.assertNotOnMainThread()
            Timber.d("Check event from activity: %s %s", packageName, className)
            return@flatMap packageActivityManager.isValidActivity(packageName, className)
                .doOnSuccess {
                  if (!it) {
                    Timber.w("Event not caused by activity.")
                    Timber.w("P: %s, C: %s", packageName, className)
                    Timber.w("Ignore")
                  }
                }
                .filter { it }
          }
    }
  }

  override fun processEvent(
    packageName: String,
    className: String,
    forcedRecheck: RecheckStatus
  ): Single<Pair<PadLockEntryModel, Int>> {
    val windowEventObservable: Single<Boolean> = serviceEnabled()
        .compose(isEventFromActivity(packageName, className))
        .doOnSuccess {
          activePackageName = packageName
          activeClassName = className
        }
        .toSingle(false)

    return windowEventObservable.map {
      enforcer.assertNotOnMainThread()
      if (!it && forcedRecheck !== FORCE) {
        Timber.e("Failed to pass window checking")
        return@map false
      } else {
        if (forcedRecheck === FORCE) {
          Timber.d("Pass filter via forced recheck")
        }

        return@map true
      }
    }
        .compose(getEntry(packageName, className))
        .doOnSuccess {
          if (!PadLockDbModels.isEmpty(it)) {
            lockPassed.remove(it.packageName(), it.activityName())
          }
        }
        // Load the application info for the valid model so we can grab the icon id
        .flatMap { model ->
          enforcer.assertNotOnMainThread()
          return@flatMap packageApplicationManager.getApplicationInfo(packageName)
              .map { it.icon }
              .map { model to it }
        }
        .onErrorReturn {
          Timber.e(it, "Error getting padlock entry")
          return@onErrorReturn PadLockDbModels.EMPTY to 0
        }
  }

  companion object {
    private const val LISTEN_INTERVAL_MILLIS = 400L
    private val QUERY_SPAN_MILLIS = TimeUnit.SECONDS.toMillis(5L)
    private val QUERY_FUTURE_OFFSET_MILLIS = TimeUnit.SECONDS.toMillis(5L)
  }
}
