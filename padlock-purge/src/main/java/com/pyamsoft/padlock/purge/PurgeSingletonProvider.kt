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

package com.pyamsoft.padlock.purge

import com.popinnow.android.repo.SingleRepo
import com.popinnow.android.repo.newRepoBuilder
import com.pyamsoft.padlock.model.purge.PurgeAllEvent
import com.pyamsoft.padlock.model.purge.PurgeEvent
import com.pyamsoft.pydroid.core.bus.Listener
import com.pyamsoft.pydroid.core.bus.Publisher
import com.pyamsoft.pydroid.core.bus.RxBus
import dagger.Module
import dagger.Provides
import java.util.concurrent.TimeUnit.MINUTES
import javax.inject.Named

@Module
object PurgeSingletonProvider {

  private val repo = newRepoBuilder<List<String>>()
      .memoryCache(5, MINUTES)
      .buildSingle()

  private val purgeBus = RxBus.create<PurgeEvent>()
  private val purgeAllBus = RxBus.create<PurgeAllEvent>()

  @JvmStatic
  @Provides
  @Named("repo_purge")
  internal fun provideRepo(): SingleRepo<List<String>> = repo

  @JvmStatic
  @Provides
  internal fun providePurgePublisher(): Publisher<PurgeEvent> = purgeBus

  @JvmStatic
  @Provides
  internal fun providePurgeListener(): Listener<PurgeEvent> = purgeBus

  @JvmStatic
  @Provides
  internal fun provideAllPublisher(): Publisher<PurgeAllEvent> = purgeAllBus

  @JvmStatic
  @Provides
  internal fun provideAllListener(): Listener<PurgeAllEvent> = purgeAllBus
}
