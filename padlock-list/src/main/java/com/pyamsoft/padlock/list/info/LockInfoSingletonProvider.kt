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

import com.popinnow.android.repo.SingleRepo
import com.popinnow.android.repo.newRepoBuilder
import com.pyamsoft.padlock.model.list.ActivityEntry
import dagger.Module
import dagger.Provides
import java.util.concurrent.TimeUnit.MINUTES
import javax.inject.Named

@JvmSuppressWildcards
@Module
object LockInfoSingletonProvider {

  private val repo = newRepoBuilder<List<ActivityEntry>>()
      .memoryCache(5, MINUTES)
      .buildSingle()

  @JvmStatic
  @Provides
  @Named("repo_lock_info")
  internal fun provideRepo(): SingleRepo<List<ActivityEntry>> = repo

}
