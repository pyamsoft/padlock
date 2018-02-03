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
