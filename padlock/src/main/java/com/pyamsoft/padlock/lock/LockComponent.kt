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

package com.pyamsoft.padlock.lock

import androidx.annotation.CheckResult
import com.pyamsoft.padlock.lock.LockComponent.LockModule
import com.pyamsoft.padlock.scopes.FragmentScope
import dagger.Binds
import dagger.BindsInstance
import dagger.Module
import dagger.Subcomponent
import javax.inject.Named

@FragmentScope
@Subcomponent(modules = [LockModule::class])
interface LockComponent {

  fun inject(activity: LockScreenActivity)

  @Subcomponent.Builder
  interface Builder {

    @BindsInstance
    @CheckResult
    fun packageName(@Named("locked_package_name") packageName: String): Builder

    @BindsInstance
    @CheckResult
    fun activityName(@Named("locked_activity_name") activityName: String): Builder

    @CheckResult
    fun build(): LockComponent
  }

  @Module
  abstract class LockModule {

    @Binds
    internal abstract fun bindClosePresenter(impl: CloseOldPresenterImpl): CloseOldPresenter

  }
}

