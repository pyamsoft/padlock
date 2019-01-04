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

package com.pyamsoft.padlock.base

import androidx.annotation.CheckResult
import com.pyamsoft.padlock.model.ForegroundEvent
import com.pyamsoft.padlock.model.LockWhitelistedEvent
import com.pyamsoft.padlock.model.service.RecheckEvent
import com.pyamsoft.padlock.model.service.ServiceFinishEvent
import com.pyamsoft.pydroid.core.bus.EventBus
import com.pyamsoft.pydroid.core.bus.Listener
import com.pyamsoft.pydroid.core.bus.Publisher
import com.pyamsoft.pydroid.core.bus.RxBus
import dagger.Module
import dagger.Provides

@Module
object BaseProvider {

  private val serviceBus = RxBus.create<ServiceFinishEvent>()
  private val recheckBus = RxBus.create<RecheckEvent>()
  private val foregroundBus = RxBus.create<ForegroundEvent>()
  private val whitelistBus = RxBus.create<LockWhitelistedEvent>()

  @JvmStatic
  @Provides
  @CheckResult
  fun provideServicePublisher(): Publisher<ServiceFinishEvent> = serviceBus

  @JvmStatic
  @Provides
  @CheckResult
  fun provideServiceListener(): Listener<ServiceFinishEvent> = serviceBus

  @JvmStatic
  @Provides
  @CheckResult
  fun provideServiceBus(): EventBus<ServiceFinishEvent> = serviceBus

  @JvmStatic
  @Provides
  @CheckResult
  fun provideRecheckPublisher(): Publisher<RecheckEvent> = recheckBus

  @JvmStatic
  @Provides
  @CheckResult
  fun provideRecheckListener(): Listener<RecheckEvent> = recheckBus

  @JvmStatic
  @Provides
  @CheckResult
  fun provideForegroundPublisher(): Publisher<ForegroundEvent> = foregroundBus

  @JvmStatic
  @Provides
  @CheckResult
  fun provideForegroundListener(): Listener<ForegroundEvent> = foregroundBus

  @JvmStatic
  @Provides
  @CheckResult
  fun provideWhitelistPublisher(): Publisher<LockWhitelistedEvent> = whitelistBus

  @JvmStatic
  @Provides
  @CheckResult
  fun provideWhitelistListener(): Listener<LockWhitelistedEvent> = whitelistBus

}
