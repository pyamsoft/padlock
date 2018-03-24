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

package com.pyamsoft.padlock.lock

import com.pyamsoft.padlock.api.LockEntryInteractor
import com.pyamsoft.pydroid.presenter.SchedulerPresenter
import io.reactivex.Scheduler
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

class LockEntryPresenter @Inject internal constructor(
  @param:Named(
      "package_name"
  ) private val packageName: String, @param:Named(
      "activity_name"
  ) private val activityName: String, @param:Named(
      "real_name"
  ) private val realName: String,
  private val interactor: LockEntryInteractor,
  @Named("computation") computationScheduler: Scheduler, @Named("io") ioScheduler: Scheduler,
  @Named("main") mainScheduler: Scheduler
) : SchedulerPresenter<LockEntryPresenter.View>(computationScheduler, ioScheduler, mainScheduler) {

  fun displayLockedHint() {
    dispose {
      interactor.getHint()
          .subscribeOn(ioScheduler)
          .observeOn(mainThreadScheduler)
          .subscribe({ view?.onDisplayHint(it) },
              { Timber.e(it, "onError displayLockedHint") })
    }
  }

  fun submit(
    lockCode: String?,
    currentAttempt: String
  ) {
    dispose {
      interactor.submitPin(packageName, activityName, lockCode, currentAttempt)
          .subscribeOn(ioScheduler)
          .observeOn(mainThreadScheduler)
          .subscribe({
            Timber.d("Received unlock entry result")
            if (it) {
              view?.onSubmitSuccess()
            } else {
              view?.onSubmitFailure()
            }
          }, {
            Timber.e(it, "unlockEntry onError")
            view?.onSubmitError(it)
          })
    }
  }

  fun lockEntry() {
    dispose {
      interactor.lockEntryOnFail(packageName, activityName)
          .subscribeOn(ioScheduler)
          .observeOn(mainThreadScheduler)
          .subscribe({
            if (System.currentTimeMillis() < it) {
              Timber.w("Lock em up")
              view?.onLocked()
            }
          }, {
            Timber.e(it, "lockEntry onError")
            view?.onLockedError(it)
          })
    }
  }

  fun postUnlock(
    lockCode: String?,
    isSystem: Boolean,
    shouldExclude: Boolean,
    ignoreTime: Long
  ) {
    dispose {
      interactor.postUnlock(
          packageName, activityName, realName, lockCode, isSystem,
          shouldExclude, ignoreTime
      )
          .subscribeOn(ioScheduler)
          .observeOn(mainThreadScheduler)
          .subscribe({
            Timber.d("onPostUnlock complete")
            view?.onPostUnlocked()
          }, {
            Timber.e(it, "Error postunlock")
            view?.onUnlockError(it)
          })
    }
  }

  interface View : HintCallback, LockCallack, PostUnlockCallback, SubmitCallback

  interface SubmitCallback {
    fun onSubmitSuccess()
    fun onSubmitFailure()
    fun onSubmitError(throwable: Throwable)
  }

  interface PostUnlockCallback {

    fun onPostUnlocked()

    fun onUnlockError(throwable: Throwable)
  }

  interface LockCallack {

    fun onLocked()

    fun onLockedError(throwable: Throwable)
  }

  interface HintCallback {

    fun onDisplayHint(hint: String)
  }
}
