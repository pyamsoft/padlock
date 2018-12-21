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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LifecycleOwner
import com.pyamsoft.padlock.list.LockInfoComponent.LockInfoModule
import com.pyamsoft.padlock.model.list.ActivityEntry
import com.pyamsoft.padlock.model.list.ListDiffProvider
import dagger.Binds
import dagger.BindsInstance
import dagger.Module
import dagger.Subcomponent
import javax.inject.Named

@Subcomponent(modules = [LockInfoModule::class])
interface LockInfoComponent {

  fun inject(dialog: LockInfoDialog)

  @Subcomponent.Builder
  interface Builder {

    @BindsInstance fun activity(activity: FragmentActivity): Builder

    @BindsInstance fun owner(owner: LifecycleOwner): Builder

    @BindsInstance fun diffProvider(diffProvider: ListDiffProvider<ActivityEntry>): Builder

    @BindsInstance fun packageName(@Named("package_name") packageName: String): Builder

    @BindsInstance fun appName(@Named("app_name") appName: String): Builder

    @BindsInstance fun listStateTag(@Named("list_state_tag") listStateTag: String): Builder

    @BindsInstance fun appIcon(@Named("app_icon") appIcon: Int): Builder

    @BindsInstance fun appSystem(@Named("app_system") system: Boolean): Builder

    @BindsInstance fun inflater(inflater: LayoutInflater): Builder

    @BindsInstance fun container(container: ViewGroup?): Builder

    @BindsInstance fun savedInstanceState(savedInstanceState: Bundle?): Builder

    fun build(): LockInfoComponent
  }

  @Module
  abstract class LockInfoModule {

    @Binds
    internal abstract fun bindView(impl: LockInfoViewImpl): LockInfoView
  }
}
