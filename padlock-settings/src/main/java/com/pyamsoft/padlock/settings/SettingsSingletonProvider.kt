/*
 * Copyright 2019 Peter Kenji Yamanaka
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
 *
 */

package com.pyamsoft.padlock.settings

import com.pyamsoft.padlock.model.ConfirmEvent
import com.pyamsoft.pydroid.core.bus.EventBus
import com.pyamsoft.pydroid.core.bus.Listener
import com.pyamsoft.pydroid.core.bus.Publisher
import com.pyamsoft.pydroid.core.bus.RxBus
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
object SettingsSingletonProvider {

  private val bus = RxBus.create<ConfirmEvent>()

  @JvmStatic
  @Provides
  internal fun provideBus(): EventBus<ConfirmEvent> = bus

  @JvmStatic
  @Provides
  internal fun providePublisher(): Publisher<ConfirmEvent> = bus

  @JvmStatic
  @Provides
  internal fun provideListener(): Listener<ConfirmEvent> = bus
}
