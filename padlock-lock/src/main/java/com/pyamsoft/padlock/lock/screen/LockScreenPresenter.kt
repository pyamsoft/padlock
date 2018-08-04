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

import androidx.lifecycle.Lifecycle.Event.ON_PAUSE
import com.pyamsoft.padlock.api.lockscreen.LockScreenInteractor
import com.pyamsoft.padlock.model.ForegroundEvent
import com.pyamsoft.pydroid.core.bus.EventBus
import com.pyamsoft.pydroid.core.presenter.Presenter
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

class LockScreenPresenter @Inject internal constructor(
  private val foregroundEventBus: EventBus<ForegroundEvent>,
  private val bus: EventBus<CloseOldEvent>,
  private val interactor: LockScreenInteractor,
  @param:Named("package_name") private val packageName: String,
  @param:Named("activity_name") private val activityName: String,
  @param:Named("real_name") private val realName: String
) : Presenter<LockScreenPresenter.View>() {

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
    dispose(ON_PAUSE) {
      interactor.isAlreadyUnlocked(packageName, activityName)
          .subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread())
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
    foregroundEventBus.publish(ForegroundEvent(packageName, realName))
  }

  private fun loadDisplayNameFromPackage() {
    dispose {
      interactor.getDisplayName(packageName)
          .subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread())
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
          .subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread())
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
          .subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread())
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
