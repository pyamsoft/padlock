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

package com.pyamsoft.padlock.purge

import android.content.Context
import com.popinnow.android.repo.Repo
import com.popinnow.android.repo.moshi.MoshiPersister
import com.popinnow.android.repo.newRepoBuilder
import com.pyamsoft.padlock.model.purge.PurgeAllEvent
import com.pyamsoft.padlock.model.purge.PurgeEvent
import com.pyamsoft.pydroid.core.bus.Listener
import com.pyamsoft.pydroid.core.bus.Publisher
import com.pyamsoft.pydroid.core.bus.RxBus
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import dagger.Module
import dagger.Provides
import java.io.File
import java.util.concurrent.TimeUnit.HOURS
import java.util.concurrent.TimeUnit.MINUTES
import javax.inject.Named
import javax.inject.Singleton

@Module
object PurgeSingletonProvider {

  private val purgeBus = RxBus.create<PurgeEvent>()
  private val purgeAllBus = RxBus.create<PurgeAllEvent>()

  @JvmStatic
  @Provides
  @Singleton
  @Named("repo_purge_list")
  internal fun provideRepo(
    context: Context,
    moshi: Moshi
  ): Repo<List<String>> {
    val type = Types.newParameterizedType(List::class.java, String::class.java)
    return newRepoBuilder<List<String>>()
        .memoryCache(30, MINUTES)
        .persister(
            2, HOURS,
            File(context.cacheDir, "repo-purge-list"),
            MoshiPersister.create(moshi, type)
        )
        .build()
  }

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
