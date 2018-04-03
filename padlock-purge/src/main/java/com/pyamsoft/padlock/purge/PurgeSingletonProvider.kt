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

import com.pyamsoft.pydroid.cache.Repository
import com.pyamsoft.pydroid.cache.repository
import dagger.Module
import dagger.Provides
import javax.inject.Named

@Module
object PurgeSingletonProvider {

  private val repo = repository<List<String>>()

  @JvmStatic
  @Provides
  @Named("repo_purge")
  internal fun provideRepo(): Repository<List<String>> = repo
}
