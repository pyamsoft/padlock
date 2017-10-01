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
