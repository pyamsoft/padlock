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

import android.support.annotation.CheckResult
import com.pyamsoft.padlock.base.queue.ActionQueue
import com.pyamsoft.pydroid.bus.EventBus
import dagger.Module
import dagger.Provides
import io.reactivex.Scheduler
import javax.inject.Named

@Module
class LockEntryModule(private val packageName: String, private val activityName: String,
    private val realName: String) {

  @Provides
  @CheckResult internal fun providePresenter(actionQueue: ActionQueue,
      bus: EventBus<LockPassEvent>,
      interactor: LockEntryInteractor, @Named("computation") compScheduler: Scheduler,
      @Named("main") mainScheduler: Scheduler,
      @Named("io") ioScheduler: Scheduler): LockEntryPresenter {
    return LockEntryPresenter(bus, packageName, activityName, realName, actionQueue, interactor,
        compScheduler, ioScheduler, mainScheduler)
  }
}

