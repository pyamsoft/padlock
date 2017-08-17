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

import com.pyamsoft.padlock.lock.screen.LockScreenPresenter.Callback
import com.pyamsoft.padlock.model.LockScreenType.TYPE_PATTERN
import com.pyamsoft.padlock.model.LockScreenType.TYPE_TEXT
import com.pyamsoft.pydroid.bus.EventBus
import com.pyamsoft.pydroid.presenter.SchedulerPresenter
import io.reactivex.Scheduler
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

internal class LockScreenPresenter @Inject internal constructor(
    private val packageName: String,
    private val activityName: String,
    private val bus: EventBus<CloseOldEvent>,
    private val interactor: LockScreenInteractor,
    @Named("computation") computationScheduler: Scheduler,
    @Named("main") mainScheduler: Scheduler,
    @Named("io") ioScheduler: Scheduler) : SchedulerPresenter<Callback>(computationScheduler,
    ioScheduler, mainScheduler) {

  override fun onStart(bound: Callback) {
    super.onStart(bound)
    initializeLockScreenType(bound::onTypePattern, bound::onTypeText)
    createWithDefaultIgnoreTime(bound::onInitializeIgnoreTime)
    loadDisplayNameFromPackage(bound::setDisplayName)
    closeOldAndAwaitSignal(bound::onCloseOldReceived)
  }

  private fun initializeLockScreenType(onTypePattern: () -> Unit, onTypeText: () -> Unit) {
    disposeOnStop {
      interactor.getLockScreenType()
          .subscribeOn(ioScheduler)
          .observeOn(mainThreadScheduler)
          .subscribe({
            when (it) {
              TYPE_PATTERN -> onTypePattern()
              TYPE_TEXT -> onTypeText()
              else -> throw IllegalArgumentException("Invalid enum: $it")
            }
          }, {
            Timber.e(it, "Error initializing lock screen type")
          })
    }
  }

  private fun createWithDefaultIgnoreTime(onInitializeWithIgnoreTime: (Long) -> Unit) {
    disposeOnStop {
      interactor.getDefaultIgnoreTime()
          .subscribeOn(ioScheduler)
          .observeOn(mainThreadScheduler)
          .subscribe({ onInitializeWithIgnoreTime(it) }, {
            Timber.e(it, "onError createWithDefaultIgnoreTime")
          })
    }
  }

  private fun loadDisplayNameFromPackage(setDisplayName: (String) -> Unit) {
    disposeOnStop {
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

    disposeOnStop {
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

  interface Callback {

    fun onTypePattern()
    fun onTypeText()

    fun onInitializeIgnoreTime(time: Long)

    fun setDisplayName(name: String)

    fun onCloseOldReceived()
  }
}
