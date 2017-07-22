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

package com.pyamsoft.padlock.service

import com.pyamsoft.padlock.base.db.PadLockEntry
import com.pyamsoft.pydroid.presenter.SchedulerPresenter
import io.reactivex.Scheduler
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

class LockServicePresenter @Inject internal constructor(
    private val lockPassBus: LockPassBus,
    private val serviceFinishBus: ServiceFinishBus,
    private val recheckEventBus: RecheckEventBus,
    private val interactor: LockServiceInteractor,
    @Named("obs") obsScheduler: Scheduler,
    @Named("sub") subScheduler: Scheduler) : SchedulerPresenter(obsScheduler, subScheduler) {

  init {
    interactor.reset()
  }

  override fun onDestroy() {
    super.onDestroy()
    interactor.cleanup()
  }

  fun registerOnBus(onRecheck: (String, String) -> Unit, onFinish: () -> Unit) {
    disposeOnDestroy {
      serviceFinishBus.listen()
          .subscribeOn(backgroundScheduler)
          .observeOn(foregroundScheduler)
          .subscribe({
            onFinish()
          }, { Timber.e(it, "onError service finish bus") })
    }

    disposeOnDestroy {
      lockPassBus.listen()
          .subscribeOn(backgroundScheduler)
          .observeOn(foregroundScheduler)
          .subscribe({
            setLockScreenPassed(it.packageName(), it.className())
          }, { Timber.e(it, "onError lock passed bus") })
    }

    disposeOnDestroy {
      recheckEventBus.listen()
          .subscribeOn(backgroundScheduler)
          .observeOn(foregroundScheduler)
          .subscribe({
            onRecheck(it.packageName(), it.className())
          }, { Timber.e(it, "onError recheck event bus") })
    }
  }

  fun setLockScreenPassed(packageName: String, className: String) {
    interactor.setLockScreenPassed(packageName, className, true)
  }

  fun processActiveApplicationIfMatching(packageName: String, className: String,
      startLockScreen: (PadLockEntry, String) -> Unit) {
    disposeOnStop {
      interactor.processActiveIfMatching(packageName, className)
          .subscribeOn(backgroundScheduler)
          .observeOn(foregroundScheduler)
          .subscribe({
            if (it) {
              processAccessibilityEvent(packageName, className, RecheckStatus.FORCE,
                  startLockScreen)
            }
          }, { Timber.e(it, "onError processActiveApplicationIfMatching") })
    }
  }

  private fun processAccessibilityEvent(packageName: String, className: String,
      forcedRecheck: RecheckStatus, startLockScreen: (PadLockEntry, String) -> Unit) {
    disposeOnStop {
      interactor.processEvent(packageName, className, forcedRecheck)
          .subscribeOn(backgroundScheduler)
          .observeOn(foregroundScheduler)
          .subscribe({
            if (PadLockEntry.isEmpty(it)) {
              Timber.w("PadLockEntry is EMPTY, ignore")
            } else {
              startLockScreen(it, className)
            }
          }, { Timber.e(it, "Error getting PadLockEntry for LockScreen") })
    }
  }
}
