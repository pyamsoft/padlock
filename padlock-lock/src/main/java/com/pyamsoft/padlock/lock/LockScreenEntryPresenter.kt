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

import com.pyamsoft.pydroid.presenter.SchedulerPresenter
import io.reactivex.Scheduler
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

internal class LockScreenEntryPresenter @Inject constructor(
    private val interactor: LockScreenEntryInteractor,
    @Named("obs") obsScheduler: Scheduler,
    @Named("io") subScheduler: Scheduler) : SchedulerPresenter(obsScheduler, subScheduler) {

  override fun onDestroy() {
    super.onDestroy()
    interactor.resetFailCount()
  }

  fun displayLockedHint(setDisplayHint: (String) -> Unit) {
    disposeOnStop {
      interactor.getHint()
          .subscribeOn(backgroundScheduler)
          .observeOn(foregroundScheduler)
          .subscribe({ setDisplayHint(it) }
              , { Timber.e(it, "onError displayLockedHint") })
    }
  }

  fun lockEntry(packageName: String, activityName: String, onLocked: (Long) -> Unit,
      onLockedError: (Throwable) -> Unit) {
    disposeOnStop {
      interactor.lockEntryOnFail(packageName, activityName)
          .subscribeOn(backgroundScheduler)
          .observeOn(foregroundScheduler)
          .subscribe({
            if (it.currentTime < it.lockUntilTime) {
              Timber.d("Received lock entry result")
              onLocked(it.lockUntilTime)
            } else {
              Timber.w("No timeout period set, entry not locked")
            }
          }, {
            Timber.e(it, "lockEntry onError")
            onLockedError(it)
          })
    }
  }

  fun submit(packageName: String, activityName: String, lockCode: String?,
      lockUntilTime: Long, currentAttempt: String, onSubmitSuccess: () -> Unit,
      onSubmitFailure: () -> Unit, onSubmitError: (Throwable) -> Unit) {
    disposeOnStop {
      interactor.submitPin(packageName, activityName, lockCode, lockUntilTime, currentAttempt)
          .subscribeOn(backgroundScheduler)
          .observeOn(foregroundScheduler)
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

  fun postUnlock(packageName: String, activityName: String,
      realName: String, lockCode: String?, isSystem: Boolean, shouldExclude: Boolean,
      ignoreTime: Long, onPostUnlock: () -> Unit, onLockedError: (Throwable) -> Unit) {
    disposeOnStop {
      interactor.postUnlock(packageName, activityName, realName, lockCode, isSystem,
          shouldExclude, ignoreTime)
          .subscribeOn(backgroundScheduler)
          .observeOn(foregroundScheduler)
          .subscribe({
            Timber.d("onPostUnlock")
            onPostUnlock()
          }, {
            Timber.e(it, "Error postunlock")
            onLockedError(it)
          })
    }
  }
}
