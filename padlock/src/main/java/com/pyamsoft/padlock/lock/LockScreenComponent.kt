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

import android.os.Bundle
import androidx.annotation.CheckResult
import com.pyamsoft.padlock.lock.LockScreenComponent.LockScreenModule
import dagger.Binds
import dagger.BindsInstance
import dagger.Module
import dagger.Subcomponent
import javax.inject.Named

@LockScreen
@Subcomponent(modules = [LockScreenModule::class])
interface LockScreenComponent {

  fun inject(activity: LockScreenActivity)

  @CheckResult
  fun plusFragmentComponent(): LockScreenFragmentComponent.Builder

  @CheckResult
  fun plusStatsComponent(): LockStatsComponent.Builder

  @Subcomponent.Builder
  interface Builder {

    @BindsInstance
    @CheckResult
    fun activity(activity: LockScreenActivity): Builder

    @BindsInstance
    @CheckResult
    fun savedInstanceState(@Named("activity_bundle") bundle: Bundle?): Builder

    @BindsInstance
    @CheckResult
    fun packageName(@Named("locked_package_name") packageName: String): Builder

    @BindsInstance
    @CheckResult
    fun activityName(@Named("locked_activity_name") activityName: String): Builder

    @BindsInstance
    @CheckResult
    fun realName(@Named("locked_real_name") realName: String): Builder

    @BindsInstance
    @CheckResult
    fun lockedIcon(@Named("locked_icon") lockedIcon: Int): Builder

    @BindsInstance
    @CheckResult
    fun system(@Named("locked_system") system: Boolean): Builder

    @CheckResult
    fun build(): LockScreenComponent
  }

  @Module
  abstract class LockScreenModule {

    @Binds
    internal abstract fun bindToolbarView(impl: LockScreenViewImpl): LockToolbarView

    @Binds
    internal abstract fun bindView(impl: LockScreenViewImpl): LockScreenView
  }

}
