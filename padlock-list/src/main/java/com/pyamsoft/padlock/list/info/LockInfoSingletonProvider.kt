/*
 * Copyright (C) 2018 Peter Kenji Yamanaka
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pyamsoft.padlock.list.info

import com.popinnow.android.repo.Repo
import com.popinnow.android.repo.newRepoBuilder
import com.pyamsoft.pydroid.core.bus.Listener
import com.pyamsoft.pydroid.core.bus.Publisher
import com.pyamsoft.pydroid.core.bus.RxBus
import dagger.Module
import dagger.Provides
import java.util.concurrent.TimeUnit.MINUTES
import javax.inject.Named

@JvmSuppressWildcards
@Module
object LockInfoSingletonProvider {

  private val bus = RxBus.create<LockInfoEvent>()

  private val repo = newRepoBuilder()
      .memoryCache(5, MINUTES)
      .build()

  @JvmStatic
  @Provides
  @Named("repo_lock_info")
  internal fun provideRepo(): Repo = repo

  @JvmStatic
  @Provides
  internal fun providePublisher(): Publisher<LockInfoEvent> = bus

  @JvmStatic
  @Provides
  internal fun provideListener(): Listener<LockInfoEvent> = bus

}
