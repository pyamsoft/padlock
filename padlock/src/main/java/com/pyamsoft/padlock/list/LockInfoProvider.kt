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

import com.pyamsoft.padlock.model.ActivityEntry
import com.pyamsoft.pydroid.list.ListDiffProvider
import dagger.Module
import dagger.Provides
import javax.inject.Named

@Module
class LockInfoProvider(
    private val packageName: String,
    private val listDiffProvider: ListDiffProvider<List<ActivityEntry>>
) {

  @Provides
  @Named("package_name")
  internal fun providePackageName(): String = packageName

  @Provides
  fun provideData(): ListDiffProvider<List<ActivityEntry>> = listDiffProvider

}

