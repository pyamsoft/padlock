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

import com.pyamsoft.padlock.base.db.PadLockEntry
import com.pyamsoft.padlock.base.queue.ActionQueue
import com.pyamsoft.padlock.model.LockState
import com.pyamsoft.padlock.model.LockState.DEFAULT
import com.pyamsoft.padlock.model.LockState.LOCKED
import com.pyamsoft.pydroid.presenter.SchedulerPresenter
import io.reactivex.Scheduler
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

class LockListItemPresenter @Inject internal constructor(
    private val actionQueue: ActionQueue,
    private val interactor: LockListItemInteractor,
    @Named("computation") compScheduler: Scheduler,
    @Named("main") mainScheduler: Scheduler,
    @Named("io") ioScheduler: Scheduler) : SchedulerPresenter<Unit>(compScheduler, ioScheduler,
    mainScheduler) {

  fun modifyDatabaseEntry(isChecked: Boolean, packageName: String, code: String?,
      system: Boolean, onDatabaseEntryCreated: () -> Unit, onDatabaseEntryDeleted: () -> Unit,
      onDatabaseEntryError: (Throwable) -> Unit) {
    // No whitelisting for modifications from the List
    val oldState: LockState
    val newState: LockState
    if (isChecked) {
      oldState = DEFAULT
      newState = LOCKED
    } else {
      oldState = LOCKED
      newState = DEFAULT
    }

    actionQueue.queue {
      interactor.modifySingleDatabaseEntry(oldState, newState, packageName,
          PadLockEntry.PACKAGE_ACTIVITY_NAME, code, system)
          .subscribeOn(ioScheduler)
          .observeOn(mainThreadScheduler)
          .subscribe({
            when (it) {
              LockState.DEFAULT -> onDatabaseEntryDeleted()
              LockState.LOCKED -> onDatabaseEntryCreated()
              else -> throw RuntimeException("Whitelist/None results are not handled")
            }
          }, {
            Timber.e(it, "onError modifyDatabaseEntry")
            onDatabaseEntryError(it)
          })
    }
  }
}
