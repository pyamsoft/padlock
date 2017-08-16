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

import com.pyamsoft.padlock.model.LockState
import com.pyamsoft.pydroid.presenter.SchedulerPresenter
import io.reactivex.Scheduler
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

class LockInfoItemPresenter @Inject internal constructor(
    private val interactor: LockInfoItemInteractor,
    @Named("computation") compScheduler: Scheduler,
    @Named("main") mainScheduler: Scheduler,
    @Named("io") ioScheduler: Scheduler) : SchedulerPresenter<Unit>(compScheduler, ioScheduler,
    mainScheduler) {

  fun modifyDatabaseEntry(oldLockState: LockState, newLockState: LockState,
      packageName: String, activityName: String, code: String?,
      system: Boolean, onDatabaseEntryCreated: () -> Unit, onDatabaseEntryDeleted: () -> Unit,
      onDatabaseEntryWhitelisted: () -> Unit, onDatabaseEntryError: (Throwable) -> Unit) {
    disposeOnStop {
      interactor.modifySingleDatabaseEntry(oldLockState, newLockState, packageName, activityName,
          code, system)
          .subscribeOn(ioScheduler)
          .observeOn(mainThreadScheduler)
          .subscribe({
            when (it) {
              LockState.DEFAULT -> onDatabaseEntryDeleted()
              LockState.WHITELISTED -> onDatabaseEntryWhitelisted()
              LockState.LOCKED -> onDatabaseEntryCreated()
              else -> throw IllegalStateException("Unsupported lock state: $it")
            }
          }, {
            Timber.e(it, "onError modifyDatabaseEntry")
            onDatabaseEntryError(it)
          })
    }
  }
}
