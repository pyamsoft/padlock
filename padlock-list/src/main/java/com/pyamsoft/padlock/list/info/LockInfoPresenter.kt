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

import com.pyamsoft.padlock.list.info.LockInfoEvent.Callback.Created
import com.pyamsoft.padlock.list.info.LockInfoEvent.Callback.Deleted
import com.pyamsoft.padlock.list.info.LockInfoEvent.Callback.Error
import com.pyamsoft.padlock.list.info.LockInfoEvent.Callback.Whitelisted
import com.pyamsoft.padlock.list.info.LockInfoPresenter.Callback
import com.pyamsoft.padlock.list.modify.LockStateModifyInteractor
import com.pyamsoft.padlock.model.ActivityEntry
import com.pyamsoft.padlock.model.LockState
import com.pyamsoft.pydroid.bus.EventBus
import com.pyamsoft.pydroid.presenter.SchedulerPresenter
import io.reactivex.Scheduler
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

class LockInfoPresenter @Inject internal constructor(
    private val bus: EventBus<LockInfoEvent>,
    private val modifyInteractor: LockStateModifyInteractor,
    private val packageName: String,
    private val lockInfoInteractor: LockInfoInteractor,
    @Named("computation") compScheduler: Scheduler,
    @Named("main") mainScheduler: Scheduler,
    @Named("io") ioScheduler: Scheduler) : SchedulerPresenter<Callback>(compScheduler, ioScheduler,
    mainScheduler) {

  override fun onStart(bound: Callback) {
    super.onStart(bound)
    populateList(false, bound::onBegin, bound::onAdd, bound::onError, bound::onPopulated)

    disposeOnStop {
      bus.listen()
          .filter { it is LockInfoEvent.Modify }
          .map { it as LockInfoEvent.Modify }
          .subscribeOn(ioScheduler).observeOn(mainThreadScheduler)
          .subscribe({ modifyDatabaseEntry(it) }, {
            Timber.e(it, "Error listening to lock info bus")
          })
    }
  }

  private fun modifyDatabaseEntry(event: LockInfoEvent.Modify) {
    disposeOnStop {
      modifyInteractor.modifySingleDatabaseEntry(event.oldState(), event.newState,
          event.packageName(),
          event.name(), event.code, event.system)
          .subscribeOn(ioScheduler)
          .observeOn(mainThreadScheduler)
          .subscribe({
            val id: String = event.id()
            when (it) {
              LockState.LOCKED -> bus.publish(Created(id))
              LockState.DEFAULT -> bus.publish(Deleted(id))
              LockState.WHITELISTED -> bus.publish(Whitelisted(id))
              else -> throw IllegalStateException("Unsupported lock state: $it")
            }
          }, {
            Timber.e(it, "onError modifyDatabaseEntry")
            bus.publish(Error(it))
          })
    }
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
