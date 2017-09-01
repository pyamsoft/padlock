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

package com.pyamsoft.padlock.lock.screen

import com.pyamsoft.padlock.lock.screen.LockScreenPresenter.FullCallback
import com.pyamsoft.pydroid.bus.EventBus
import com.pyamsoft.pydroid.presenter.SchedulerPresenter
import io.reactivex.Scheduler
import timber.log.Timber
import javax.inject.Inject

class LockScreenPresenter @Inject internal constructor(
    private val lockScreenInputPresenter: LockScreenInputPresenter, private val packageName: String,
    private val activityName: String, private val bus: EventBus<CloseOldEvent>,
    private val interactor: LockScreenInteractor, computationScheduler: Scheduler,
    ioScheduler: Scheduler, mainScheduler: Scheduler) : SchedulerPresenter<FullCallback>(
    computationScheduler, ioScheduler, mainScheduler) {

  override fun onBind(v: FullCallback) {
    super.onBind(v)
    lockScreenInputPresenter.bind(v)
    loadDisplayNameFromPackage(v::setDisplayName)
    closeOldAndAwaitSignal(v::onCloseOldReceived)
  }

  override fun onUnbind() {
    super.onUnbind()
    lockScreenInputPresenter.unbind()
  }

  private fun loadDisplayNameFromPackage(setDisplayName: (String) -> Unit) {
    dispose {
      interactor.getDisplayName(packageName)
          .subscribeOn(ioScheduler)
          .observeOn(mainThreadScheduler)
          .subscribe({ setDisplayName(it) }, { throwable ->
            Timber.e(throwable, "Error loading display name from package")
            setDisplayName("")
          })
    }
  }

  private fun closeOldAndAwaitSignal(onCloseOldReceived: () -> Unit) {
    // Send bus event first before we register or we may catch our own event.
    bus.publish(CloseOldEvent(packageName, activityName))

    dispose {
      bus.listen()
          .filter { it.packageName == packageName && it.activityName == activityName }
          .subscribeOn(ioScheduler)
          .observeOn(mainThreadScheduler)
          .subscribe({
            Timber.w("Received a close old event: %s %s", it.packageName, it.activityName)
            onCloseOldReceived()
          }, {
            Timber.e(it, "Error bus close old")
          })
    }
  }

  fun createWithDefaultIgnoreTime(onInitializeWithIgnoreTime: (Long) -> Unit) {
    dispose {
      interactor.getDefaultIgnoreTime()
          .subscribeOn(ioScheduler)
          .observeOn(mainThreadScheduler)
          .subscribe({ onInitializeWithIgnoreTime(it) }, {
            Timber.e(it, "onError createWithDefaultIgnoreTime")
          })
    }
  }


  interface FullCallback : NameCallback, LockScreenInputPresenter.Callback, OldCallback

  interface NameCallback {

    fun setDisplayName(name: String)
  }

  interface OldCallback {

    fun onCloseOldReceived()
  }
}
