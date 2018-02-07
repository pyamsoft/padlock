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

package com.pyamsoft.padlock.lock.screen

import com.pyamsoft.padlock.api.LockScreenInteractor
import com.pyamsoft.padlock.lock.screen.LockScreenPresenter.View
import com.pyamsoft.padlock.model.ForegroundEvent
import com.pyamsoft.pydroid.bus.EventBus
import com.pyamsoft.pydroid.presenter.SchedulerPresenter
import io.reactivex.Scheduler
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

class LockScreenPresenter @Inject internal constructor(
    private val foregroundEventBus: EventBus<ForegroundEvent>, @param:Named(
        "package_name"
    ) private val packageName: String,
    @param:Named("activity_name") private val activityName: String,
    @param:Named("real_name") private val realName: String,
    private val bus: EventBus<CloseOldEvent>,
    private val interactor: LockScreenInteractor, @Named(
        "computation"
    ) computationScheduler: Scheduler,
    @Named("io") ioScheduler: Scheduler, @Named(
        "main"
    ) mainScheduler: Scheduler
) : SchedulerPresenter<View>(
    computationScheduler, ioScheduler, mainScheduler
) {

  override fun onCreate() {
    super.onCreate()
    loadDisplayNameFromPackage()
    closeOldAndAwaitSignal()
  }

  override fun onPause() {
    super.onPause()
    clearMatchingForegroundEvent()
  }

  override fun onResume() {
    super.onResume()
    checkIfAlreadyUnlocked()
  }

  private fun checkIfAlreadyUnlocked() {
    Timber.d("Check if $packageName $activityName already unlocked")
    dispose {
      interactor.isAlreadyUnlocked(packageName, activityName)
          .subscribeOn(ioScheduler)
          .observeOn(mainThreadScheduler)
          .subscribe({
            if (it) {
              view?.onAlreadyUnlocked()
            }
          }, {
            Timber.e(it, "Error checking already unlocked state")
          })
    }
  }

  private fun clearMatchingForegroundEvent() {
    Timber.d("Publish foreground clear event for $packageName, $realName")
    foregroundEventBus.publish(
        ForegroundEvent(packageName, realName)
    )
  }

  private fun loadDisplayNameFromPackage() {
    dispose {
      interactor.getDisplayName(packageName)
          .subscribeOn(ioScheduler)
          .observeOn(mainThreadScheduler)
          .subscribe({ view?.onSetDisplayName(it) }, {
            Timber.e(it, "Error loading display name from package")
            view?.onSetDisplayName("")
          })
    }
  }

  private fun closeOldAndAwaitSignal() {
    // Send bus event first before we register or we may catch our own event.
    bus.publish(CloseOldEvent(packageName, activityName))

    dispose {
      bus.listen()
          .filter { it.packageName == packageName && it.activityName == activityName }
          .subscribeOn(ioScheduler)
          .observeOn(mainThreadScheduler)
          .subscribe({
            Timber.w(
                "Received a close old event: %s %s", it.packageName,
                it.activityName
            )
            view?.onCloseOldReceived()
          }, {
            Timber.e(it, "Error bus close old")
          })
    }
  }

  fun createWithDefaultIgnoreTime() {
    dispose {
      interactor.getDefaultIgnoreTime()
          .subscribeOn(ioScheduler)
          .observeOn(mainThreadScheduler)
          .subscribe({ view?.onInitializeWithIgnoreTime(it) }, {
            Timber.e(it, "onError createWithDefaultIgnoreTime")
          })
    }
  }

  interface View : NameCallback, OldCallback, IgnoreTimeCallback, AlreadyUnlockedCallback

  interface IgnoreTimeCallback {

    fun onInitializeWithIgnoreTime(time: Long)
  }

  interface NameCallback {

    fun onSetDisplayName(name: String)
  }

  interface OldCallback {

    fun onCloseOldReceived()
  }

  interface AlreadyUnlockedCallback {

    fun onAlreadyUnlocked()
  }
}
