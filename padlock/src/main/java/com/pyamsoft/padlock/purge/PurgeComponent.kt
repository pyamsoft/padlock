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

import android.view.ViewGroup
import androidx.annotation.CheckResult
import androidx.lifecycle.LifecycleOwner
import com.pyamsoft.padlock.purge.PurgeComponent.PurgeModule
import com.pyamsoft.padlock.scopes.FragmentScope
import com.pyamsoft.pydroid.ui.app.ToolbarActivity
import dagger.Binds
import dagger.BindsInstance
import dagger.Module
import dagger.Subcomponent

@FragmentScope
@Subcomponent(modules = [PurgeModule::class])
interface PurgeComponent {

  fun inject(fragment: PurgeFragment)

  @Subcomponent.Builder
  interface Builder {

    @BindsInstance
    @CheckResult
    fun toolbarActivity(toolbarActivity: ToolbarActivity): Builder

    @BindsInstance
    @CheckResult
    fun owner(owner: LifecycleOwner): Builder

    @BindsInstance
    @CheckResult
    fun parent(parent: ViewGroup): Builder

    @CheckResult
    fun build(): PurgeComponent
  }

  @Module
  abstract class PurgeModule {

    @Binds
    internal abstract fun bindPresenter(impl: PurgePresenterImpl): PurgePresenter

    @Binds
    internal abstract fun bindListViewCallback(impl: PurgePresenterImpl): PurgeListView.Callback

    @Binds
    internal abstract fun bindToolbarViewCallback(impl: PurgePresenterImpl): PurgeToolbarView.Callback

  }
}

