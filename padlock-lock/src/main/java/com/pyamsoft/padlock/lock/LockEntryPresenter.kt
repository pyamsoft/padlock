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

package com.pyamsoft.padlock.lock

import com.pyamsoft.pydroid.bus.EventBus
import com.pyamsoft.pydroid.presenter.SchedulerPresenter
import io.reactivex.Scheduler
import timber.log.Timber
import javax.inject.Inject

class LockEntryPresenter @Inject internal constructor(private val bus: EventBus<LockPassEvent>,
    private val packageName: String, private val activityName: String, private val realName: String,
    private val interactor: LockEntryInteractor,
    computationScheduler: Scheduler, ioScheduler: Scheduler,
    mainScheduler: Scheduler) : SchedulerPresenter<Unit>(computationScheduler,
    ioScheduler, mainScheduler) {

  fun displayLockedHint(setDisplayHint: (String) -> Unit) {
    dispose {
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
    dispose {
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
    dispose {
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
    dispose {
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
}
