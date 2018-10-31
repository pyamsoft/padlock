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

package com.pyamsoft.padlock.list

import android.content.Context
import com.popinnow.android.repo.Repo
import com.popinnow.android.repo.moshi.MoshiPersister
import com.popinnow.android.repo.moshi.persister
import com.popinnow.android.repo.newRepoBuilder
import com.pyamsoft.padlock.model.list.AppEntry
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
object LockListSingletonProvider {

  private val bus = RxBus.create<LockListEvent>()

  @JvmStatic
  @Provides
  @Singleton
  @Named("repo_lock_list")
  internal fun provideRepo(
    context: Context,
    moshi: Moshi
  ): Repo<List<AppEntry>> {
    val type = Types.newParameterizedType(List::class.java, AppEntry::class.java)
    return newRepoBuilder<List<AppEntry>>()
        .memoryCache(30, MINUTES)
        .persister(
            2, HOURS,
            File(context.cacheDir, "repo-lock-list"),
            MoshiPersister.create(moshi, type)
        )
        .build()
  }

  @JvmStatic
  @Provides
  internal fun providePublisher(): Publisher<LockListEvent> = bus

  @JvmStatic
  @Provides
  internal fun provideListener(): Listener<LockListEvent> = bus

}
