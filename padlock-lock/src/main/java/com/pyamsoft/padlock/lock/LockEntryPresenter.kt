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

import com.pyamsoft.padlock.base.queue.ActionQueue
import com.pyamsoft.pydroid.bus.EventBus
import com.pyamsoft.pydroid.presenter.SchedulerPresenter
import io.reactivex.Scheduler
import timber.log.Timber
import java.lang.ref.WeakReference
import javax.inject.Inject

class LockEntryPresenter @Inject internal constructor(private val bus: EventBus<LockPassEvent>,
    private val packageName: String, private val activityName: String, private val realName: String,
    private val actionQueue: ActionQueue, private val interactor: LockEntryInteractor,
    computationScheduler: Scheduler, ioScheduler: Scheduler,
    mainScheduler: Scheduler) : SchedulerPresenter<Unit>(computationScheduler,
    ioScheduler, mainScheduler) {

  fun passLockScreen() {
    bus.publish(LockPassEvent(packageName, activityName))
  }

  fun displayLockedHint(setDisplayHint: (String) -> Unit) {
    disposeOnStop {
      interactor.getHint()
          .subscribeOn(ioScheduler)
          .observeOn(mainThreadScheduler)
          .subscribe({ setDisplayHint(it) }, { Timber.e(it, "onError displayLockedHint") })
    }
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
    actionQueue.queue {
      val weakOnLocked: WeakReference<() -> Unit> = WeakReference(onLocked)
      val weakOnLockedError: WeakReference<(Throwable) -> Unit> = WeakReference(onLockedError)
      interactor.lockEntryOnFail(packageName, activityName)
          .subscribeOn(ioScheduler)
          .observeOn(mainThreadScheduler)
          .subscribe({
            if (System.currentTimeMillis() < it) {
              Timber.w("Lock em up")
              weakOnLocked.get()?.invoke()
            }
          }, {
            Timber.e(it, "lockEntry onError")
            weakOnLockedError.get()?.invoke(it)
          })
    }
  }

  fun postUnlock(lockCode: String?, isSystem: Boolean, shouldExclude: Boolean, ignoreTime: Long,
      onPostUnlocked: () -> Unit, onUnlockError: (Throwable) -> Unit) {
    actionQueue.queue {
      val weakOnPostUnlocked: WeakReference<() -> Unit> = WeakReference(onPostUnlocked)
      val weakOnUnlockError: WeakReference<(Throwable) -> Unit> = WeakReference(onUnlockError)
      interactor.postUnlock(packageName, activityName, realName, lockCode, isSystem,
          shouldExclude, ignoreTime)
          .subscribeOn(ioScheduler)
          .observeOn(mainThreadScheduler)
          .subscribe({
            Timber.d("onPostUnlock complete")
            weakOnPostUnlocked.get()?.invoke()
          }, {
            Timber.e(it, "Error postunlock")
            weakOnUnlockError.get()?.invoke(it)
          })
    }
  }
}
