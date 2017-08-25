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

import com.pyamsoft.padlock.lock.LockEntryPresenter.Callback
import com.pyamsoft.pydroid.bus.EventBus
import com.pyamsoft.pydroid.presenter.SchedulerPresenter
import io.reactivex.Scheduler
import timber.log.Timber
import javax.inject.Inject

class LockEntryPresenter @Inject internal constructor(private val bus: EventBus<LockPassEvent>,
    private val packageName: String, private val activityName: String, private val realName: String,
    private val interactor: LockEntryInteractor,
    computationScheduler: Scheduler, ioScheduler: Scheduler,
    mainScheduler: Scheduler) : SchedulerPresenter<Unit, Callback>(computationScheduler,
    ioScheduler, mainScheduler) {

  override fun onStart(bound: Callback) {
    super.onStart(bound)
    displayLockedHint(bound::onDisplayHint)
  }

  private fun displayLockedHint(setDisplayHint: (String) -> Unit) {
    disposeOnStop {
      interactor.getHint()
          .subscribeOn(ioScheduler)
          .observeOn(mainThreadScheduler)
          .subscribe({ setDisplayHint(it) }, { Timber.e(it, "onError displayLockedHint") })
    }
  }


  fun passLockScreen() {
    bus.publish(LockPassEvent(packageName, activityName))
  }

  fun submit(lockCode: String?, currentAttempt: String,
      onSubmitSuccess: () -> Unit, onSubmitFailure: () -> Unit,
      onSubmitError: (Throwable) -> Unit) {
    disposeOnStop {
      interactor.submitPin(packageName, activityName, lockCode, currentAttempt)
          .subscribeOn(ioScheduler)
          .observeOn(mainThreadScheduler)
          .subscribe({
            Timber.d("Received unlock entry result")
            if (it) {
              onSubmitSuccess()
            } else {
              onSubmitFailure()
            }
          }, {
            Timber.e(it, "unlockEntry onError")
            onSubmitError(it)
          })
    }
  }

  fun lockEntry(onLocked: () -> Unit, onLockedError: (Throwable) -> Unit) {
    disposeOnStop {
      interactor.lockEntryOnFail(packageName, activityName)
          .subscribeOn(ioScheduler)
          .observeOn(mainThreadScheduler)
          .subscribe({
            if (System.currentTimeMillis() < it) {
              Timber.w("Lock em up")
              onLocked()
            }
          }, {
            Timber.e(it, "lockEntry onError")
            onLockedError(it)
          })
    }
  }

  fun postUnlock(lockCode: String?, isSystem: Boolean, shouldExclude: Boolean, ignoreTime: Long,
      onPostUnlocked: () -> Unit, onUnlockError: (Throwable) -> Unit) {
    disposeOnStop {
      interactor.postUnlock(packageName, activityName, realName, lockCode, isSystem,
          shouldExclude, ignoreTime)
          .subscribeOn(ioScheduler)
          .observeOn(mainThreadScheduler)
          .subscribe({
            Timber.d("onPostUnlock complete")
            onPostUnlocked()
          }, {
            Timber.e(it, "Error postunlock")
            onUnlockError(it)
          })
    }
  }

  interface Callback {

    fun onDisplayHint(hint: String)
  }
}
