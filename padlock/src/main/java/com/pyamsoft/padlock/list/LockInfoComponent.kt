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

import androidx.lifecycle.LifecycleOwner
import com.pyamsoft.padlock.model.list.ActivityEntry
import com.pyamsoft.padlock.model.list.ListDiffProvider
import dagger.BindsInstance
import dagger.Subcomponent
import javax.inject.Named

@Subcomponent
interface LockInfoComponent {

  fun inject(dialog: LockInfoDialog)

  @Subcomponent.Builder
  interface Builder {

    @BindsInstance fun owner(owner: LifecycleOwner): Builder

    @BindsInstance fun diffProvider(diffProvider: ListDiffProvider<ActivityEntry>): Builder

    @BindsInstance fun packageName(@Named("package_name") packageName: String): Builder

    fun build(): LockInfoComponent
  }
}
