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
import androidx.annotation.CheckResult
import com.pyamsoft.padlock.api.MasterPinInteractor
import com.pyamsoft.padlock.api.database.EntryQueryDao
import com.pyamsoft.padlock.api.lockscreen.LockPassed
import com.pyamsoft.padlock.api.packagemanager.PackageActivityManager
import com.pyamsoft.padlock.api.service.JobSchedulerCompat
import com.pyamsoft.padlock.api.service.LockServiceInteractor
import com.pyamsoft.padlock.api.service.ScreenStateObserver
import com.pyamsoft.padlock.api.service.UsageEventProvider
import com.pyamsoft.padlock.model.Excludes
import com.pyamsoft.padlock.model.ForegroundEvent
import com.pyamsoft.padlock.model.db.PadLockDbModels
import com.pyamsoft.padlock.model.db.PadLockEntryModel
import com.pyamsoft.padlock.model.service.RecheckStatus
import com.pyamsoft.padlock.model.service.RecheckStatus.FORCE
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
  private val screenStateObserver: ScreenStateObserver,
  private val usageEventProvider: UsageEventProvider,
  private val lockPassed: LockPassed,
  private val jobSchedulerCompat: JobSchedulerCompat,
  private val packageActivityManager: PackageActivityManager,
  private val queryDao: EntryQueryDao,
  @param:Named("recheck") private val recheckServiceClass: Class<out IntentService>,
  private val pinInteractor: MasterPinInteractor
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

  override fun isServiceEnabled(): Single<Boolean> = pinInteractor.getMasterPin()
      .map {
        enforcer.assertNotOnMainThread()
        return@map it is Present
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

  private fun emit(
    emitter: ObservableEmitter<Boolean>,
    screenOn: Boolean
  ) {
    if (!emitter.isDisposed) {
      emitter.onNext(screenOn)
    }
  }

  override fun observeScreenState(): Observable<Boolean> {
    return Observable.create { emitter ->
      emitter.setCancellable {
        screenStateObserver.unregister()
      }

      // Observe screen state changes
      screenStateObserver.register {
        emit(emitter, it)
      }

      // Start with screen as ON
      emit(emitter, true)
    }
  }

  /**
   * Take care to avoid any calls to logging methods as it will run every 200 ms and flood
   */
  override fun listenForForegroundEvents(): Flowable<ForegroundEvent> {
    return Flowable.interval(LISTEN_INTERVAL, MILLISECONDS)
        .map {
          enforcer.assertNotOnMainThread()
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
  ): Single<PadLockEntryModel> {
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
  }

  companion object {
    private const val LISTEN_INTERVAL = 300L
    private val TEN_SECONDS_MILLIS = TimeUnit.SECONDS.toMillis(10L)
  }
}
