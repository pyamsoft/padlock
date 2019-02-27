/*
 * Copyright 2019 Peter Kenji Yamanaka
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
 *
 */

package com.pyamsoft.padlock.lock.screen

import androidx.annotation.CheckResult
import com.pyamsoft.padlock.api.lockscreen.LockScreenInteractor
import com.pyamsoft.padlock.model.ForegroundEvent
import com.pyamsoft.pydroid.core.bus.EventBus
import com.pyamsoft.pydroid.core.bus.Publisher
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.disposables.Disposables
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

class LockScreenViewModel @Inject internal constructor(
  private val foregroundEventBus: Publisher<ForegroundEvent>,
  private val bus: EventBus<CloseOldEvent>,
  private val interactor: LockScreenInteractor,
  @param:Named("locked_package_name") private val packageName: String,
  @param:Named("locked_activity_name") private val activityName: String,
  @param:Named("locked_real_name") private val realName: String
) {

  @CheckResult
  fun onRecreateEvent(func: () -> Unit): Disposable {
    return Disposables.empty()
  }

  fun clearMatchingForegroundEvent() {
    Timber.d("Publish foreground clear event for $packageName, $realName")
    foregroundEventBus.publish(ForegroundEvent(packageName, realName))
  }

  @CheckResult
  fun checkIfAlreadyUnlocked(onAlreadyUnlocked: () -> Unit): Disposable {
    return interactor.isAlreadyUnlocked(packageName, activityName)
        .filter { it }
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .doOnSubscribe { Timber.d("Check if $packageName $activityName already unlocked") }
        .subscribe { onAlreadyUnlocked() }
  }

  @CheckResult
  fun loadDisplayNameFromPackage(onLoadDisplayName: (name: String) -> Unit): Disposable {
    return interactor.getDisplayName(packageName)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(onLoadDisplayName)
  }

  @CheckResult
  fun closeOld(onCloseOldEvent: (event: CloseOldEvent) -> Unit): Disposable {
    // Send bus event first before we register or we may catch our own event.
    bus.publish(CloseOldEvent(packageName, activityName))

    return bus.listen()
        .filter { it.packageName == packageName && it.activityName == activityName }
        .subscribe(onCloseOldEvent)
  }

  @CheckResult
  fun createWithDefaultIgnoreTime(onIgnoreTimesLoaded: (time: Long) -> Unit): Disposable {
    return interactor.getDefaultIgnoreTime()
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(onIgnoreTimesLoaded)
  }

}
