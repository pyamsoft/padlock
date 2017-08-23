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

import android.support.annotation.CheckResult
import com.pyamsoft.padlock.base.queue.ActionQueue
import com.pyamsoft.pydroid.bus.EventBus
import dagger.Module
import dagger.Provides
import io.reactivex.Scheduler
import javax.inject.Named

@Module
class LockInfoModule(private val packageName: String) {

  @Provides
  @CheckResult internal fun provideInfoPresenter(lockInfoInteractor: LockInfoInteractor,
      actionQueue: ActionQueue, @Named("computation") computationScheduler: Scheduler,
      @Named("io") ioScheduler: Scheduler, bus: EventBus<LockInfoEvent>,
      modifyInteractor: LockInfoItemInteractor,
      @Named("main") mainScheduler: Scheduler): LockInfoPresenter {
    return LockInfoPresenter(actionQueue, bus, modifyInteractor, packageName, lockInfoInteractor,
        computationScheduler, ioScheduler, mainScheduler)
  }
}

