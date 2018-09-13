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

import androidx.lifecycle.LifecycleOwner
import com.pyamsoft.padlock.api.lockscreen.LockScreenInteractor
import com.pyamsoft.padlock.model.ForegroundEvent
import com.pyamsoft.pydroid.core.bus.EventBus
import com.pyamsoft.pydroid.core.bus.Publisher
import com.pyamsoft.pydroid.core.bus.RxBus
import com.pyamsoft.pydroid.core.viewmodel.BaseViewModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

class LockScreenViewModel @Inject internal constructor(
  owner: LifecycleOwner,
  private val foregroundEventBus: Publisher<ForegroundEvent>,
  private val bus: EventBus<CloseOldEvent>,
  private val interactor: LockScreenInteractor,
  @param:Named("package_name") private val packageName: String,
  @param:Named("activity_name") private val activityName: String,
  @param:Named("real_name") private val realName: String
) : BaseViewModel(owner) {

  private val alreadyUnlockedBus = RxBus.create<Boolean>()
  private val displayNameBus = RxBus.create<String>()
  private val closeOldBus = RxBus.create<CloseOldEvent>()
  private val ignoreTimeBus = RxBus.create<Long>()

  private var alreadyUnlockedDisposable by disposable()
  private var displayNameDisposable by disposable()
  private var closeOldDisposable by disposable()
  private var ignoreTimeDisposable by disposable()

  override fun onCleared() {
    super.onCleared()
    alreadyUnlockedDisposable.tryDispose()
    displayNameDisposable.tryDispose()
    closeOldDisposable.tryDispose()
    ignoreTimeDisposable.tryDispose()
  }

  fun onAlreadyUnlockedEvent(func: () -> Unit) {
    dispose {
      alreadyUnlockedBus.listen()
          .filter { it }
          .subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread())
          .subscribe { func() }
    }
  }

  fun onCloseOldEvent(func: (CloseOldEvent) -> Unit) {
    dispose {
      closeOldBus.listen()
          .filter { it.packageName == packageName && it.activityName == activityName }
          .subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread())
          .subscribe(func)
    }
  }

  fun onDisplayName(func: (String) -> Unit) {
    dispose {
      displayNameBus.listen()
          .subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread())
          .subscribe(func)
    }
  }

  fun onIgnoreTimesLoaded(func: (Long) -> Unit) {
    dispose {
      ignoreTimeBus.listen()
          .subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread())
          .subscribe(func)
    }
  }

  fun clearMatchingForegroundEvent() {
    Timber.d("Publish foreground clear event for $packageName, $realName")
    foregroundEventBus.publish(ForegroundEvent(packageName, realName))
  }

  fun checkIfAlreadyUnlocked() {
    Timber.d("Check if $packageName $activityName already unlocked")
    alreadyUnlockedDisposable = interactor.isAlreadyUnlocked(packageName, activityName)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe({ alreadyUnlockedBus.publish(it) }, {
          Timber.e(it, "Error checking already unlocked state")
        })
  }

  fun loadDisplayNameFromPackage() {
    displayNameDisposable = interactor.getDisplayName(packageName)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe({ displayNameBus.publish(it) }, {
          Timber.e(it, "Error loading display name from package")
          displayNameBus.publish("")
        })
  }

  fun closeOld() {
    // Send bus event first before we register or we may catch our own event.
    bus.publish(CloseOldEvent(packageName, activityName))

    closeOldDisposable = bus.listen()
        .filter { it.packageName == packageName && it.activityName == activityName }
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe {
          Timber.w("Received a close old event: %s %s", it.packageName, it.activityName)
          closeOldBus.publish(it)
        }
  }

  fun createWithDefaultIgnoreTime() {
    ignoreTimeDisposable = interactor.getDefaultIgnoreTime()
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe({ ignoreTimeBus.publish(it) }, {
          Timber.e(it, "onError createWithDefaultIgnoreTime")
        })
  }

}
