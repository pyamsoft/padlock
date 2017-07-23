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

package com.pyamsoft.padlock.list

import com.pyamsoft.padlock.model.ActivityEntry
import com.pyamsoft.pydroid.presenter.SchedulerPresenter
import io.reactivex.Scheduler
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

internal class LockInfoPresenter @Inject constructor(
    private val lockInfoInteractor: LockInfoInteractor,
    @Named("obs") obsScheduler: Scheduler,
    @Named("io") subScheduler: Scheduler) : SchedulerPresenter(obsScheduler, subScheduler) {

  fun populateList(packageName: String, forceRefresh: Boolean, onListPopulateBegin: () -> Unit,
      onEntryAddedToList: (ActivityEntry) -> Unit, onListPopulateError: (Throwable) -> Unit,
      onListPopulated: () -> Unit) {
    disposeOnStop {
      lockInfoInteractor.populateList(packageName, forceRefresh)
          .subscribeOn(backgroundScheduler)
          .observeOn(foregroundScheduler)
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
          .subscribeOn(backgroundScheduler)
          .observeOn(foregroundScheduler)
          .subscribe({
            if (it) {
              onOnboardingComplete()
            } else {
              onShowOnboarding()
            }
          }, { Timber.e(it, "onError") })
    }
  }
}
