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

package com.pyamsoft.padlock.list.info

import com.pyamsoft.padlock.list.info.LockInfoPresenter.Callback
import com.pyamsoft.padlock.model.ActivityEntry
import com.pyamsoft.pydroid.presenter.SchedulerPresenter
import io.reactivex.Scheduler
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

class LockInfoPresenter @Inject internal constructor(
    private val packageName: String,
    private val lockInfoInteractor: LockInfoInteractor,
    @Named("computation") compScheduler: Scheduler,
    @Named("main") mainScheduler: Scheduler,
    @Named("io") ioScheduler: Scheduler) : SchedulerPresenter<Callback>(compScheduler, ioScheduler,
    mainScheduler) {

  override fun onStart(bound: Callback) {
    super.onStart(bound)
    populateList(false, bound::onBegin, bound::onAdd, bound::onError, bound::onPopulated)
  }

  fun populateList(forceRefresh: Boolean, onListPopulateBegin: () -> Unit,
      onEntryAddedToList: (ActivityEntry) -> Unit, onListPopulateError: (Throwable) -> Unit,
      onListPopulated: () -> Unit) {
    disposeOnStop {
      lockInfoInteractor.populateList(packageName, forceRefresh)
          .subscribeOn(ioScheduler)
          .observeOn(mainThreadScheduler)
          .doAfterTerminate { onListPopulated() }
          .doOnSubscribe { onListPopulateBegin() }
          .subscribe({ onEntryAddedToList(it) }, {
            Timber.e(it, "LockInfoPresenterImpl populateList onError")
            onListPopulateError(it)
          })
    }
  }

  fun showOnBoarding(onShowOnboarding: () -> Unit, onOnboardingComplete: () -> Unit) {
    disposeOnStop {
      lockInfoInteractor.hasShownOnBoarding()
          .subscribeOn(ioScheduler)
          .observeOn(mainThreadScheduler)
          .subscribe({
            if (it) {
              onOnboardingComplete()
            } else {
              onShowOnboarding()
            }
          }, { Timber.e(it, "onError") })
    }
  }

  interface Callback {

    fun onBegin()

    fun onAdd(entry: ActivityEntry)

    fun onError(throwable: Throwable)

    fun onPopulated()
  }
}
