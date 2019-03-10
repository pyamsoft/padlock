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

package com.pyamsoft.padlock.list.info

import android.view.View
import androidx.annotation.CheckResult
import com.pyamsoft.padlock.list.info.LockInfoItem.ViewHolder
import com.pyamsoft.padlock.list.info.LockInfoItemComponent.LockInfoModule
import dagger.Binds
import dagger.BindsInstance
import dagger.Module
import dagger.Subcomponent

@Subcomponent(modules = [LockInfoModule::class])
interface LockInfoItemComponent {

  fun inject(holder: ViewHolder)

  fun inject(holder: LockInfoGroup.ViewHolder)

  @Subcomponent.Builder
  interface Builder {

    @BindsInstance
    @CheckResult
    fun itemView(itemView: View): Builder

    @CheckResult
    fun build(): LockInfoItemComponent
  }

  @Module
  abstract class LockInfoModule {

    @Binds
    internal abstract fun bindItemView(impl: LockInfoItemViewImpl): LockInfoItemView

    @Binds
    internal abstract fun bindGroupView(impl: LockInfoGroupViewImpl): LockInfoGroupView

  }
}

