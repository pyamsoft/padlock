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

package com.pyamsoft.padlock.lock

import com.pyamsoft.padlock.lock.common.LockTypePresenter
import io.reactivex.Scheduler
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

internal class LockScreenPresenter @Inject constructor(private val interactor: LockScreenInteractor,
    private val bus: CloseOldBus,
    @Named("obs") obsScheduler: Scheduler,
    @Named("io") subScheduler: Scheduler) : LockTypePresenter(interactor, obsScheduler,
    subScheduler) {

  fun createWithDefaultIgnoreTime(onInitializeWithIgnoreTime: (Long) -> Unit) {
    disposeOnStop {
      interactor.getDefaultIgnoreTime()
          .subscribeOn(backgroundScheduler)
          .observeOn(foregroundScheduler)
          .subscribe({ onInitializeWithIgnoreTime(it) }, {
            Timber.e(it, "onError createWithDefaultIgnoreTime")
          })
    }
  }

  fun loadDisplayNameFromPackage(packageName: String, setDisplayName: (String) -> Unit) {
    disposeOnStop {
      interactor.getDisplayName(packageName)
          .subscribeOn(backgroundScheduler)
          .observeOn(foregroundScheduler)
          .subscribe({ setDisplayName(it) }, { throwable ->
            Timber.e(throwable, "Error loading display name from package")
            setDisplayName("")
          })
    }
  }

  fun closeOldAndAwaitSignal(packageName: String, activityName: String,
      onCloseOldReceived: () -> Unit) {

    // Send bus event first before we register or we may catch our own event.
    bus.publish(CloseOldEvent.create(packageName, activityName))

    disposeOnStop {
      bus.listen()
          .filter { it.packageName() == packageName && it.activityName() == activityName }
          .subscribeOn(backgroundScheduler)
          .observeOn(foregroundScheduler)
          .subscribe({
            Timber.w("Received a close old event: %s %s", it.packageName(), it.activityName())
            onCloseOldReceived()
          }, {
            Timber.e(it, "Error bus close old")
          })
    }
  }
}
