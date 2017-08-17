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
import com.pyamsoft.padlock.list.info.LockInfoEvent.Callback.Whitelisted
import com.pyamsoft.padlock.list.info.LockInfoItemPresenter.Callback
import com.pyamsoft.padlock.model.ActivityEntry
import com.pyamsoft.padlock.model.LockState
import com.pyamsoft.pydroid.presenter.SchedulerPresenter
import io.reactivex.Scheduler
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

class LockInfoItemPresenter @Inject internal constructor(
    private val bus: LockInfoBus,
    @Named("computation") compScheduler: Scheduler,
    @Named("main") mainScheduler: Scheduler,
    @Named("io") ioScheduler: Scheduler) : SchedulerPresenter<Callback>(compScheduler, ioScheduler,
    mainScheduler) {

  override fun onStart(bound: LockInfoItemPresenter.Callback) {
    super.onStart(bound)
    disposeOnStop {
      bus.listen()
          .filter { it is LockInfoEvent.Callback }
          .map { it as LockInfoEvent.Callback }
          .subscribeOn(ioScheduler).observeOn(mainThreadScheduler)
          .subscribe({
            when (it) {
              is Created -> bound.onEntryCreated(it.id)
              is Deleted -> bound.onEntryDeleted(it.id)
              is Whitelisted -> bound.onEntryWhitelisted(it.id)
            }
          }, {
            Timber.e(it, "Error listening to lock info bus")
            bound.onEntryError(it)
          })
    }
  }

  fun publish(entry: ActivityEntry, newState: LockState, code: String?, system: Boolean) {
    bus.publish(LockInfoEvent.Modify(entry, newState, code, system))
  }

  interface Callback {

    fun onEntryCreated(id: String)

    fun onEntryDeleted(id: String)

    fun onEntryWhitelisted(id: String)

    fun onEntryError(throwable: Throwable)
  }
}
