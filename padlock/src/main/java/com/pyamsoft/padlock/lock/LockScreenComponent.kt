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

package com.pyamsoft.padlock.lock

import androidx.annotation.CheckResult
import androidx.lifecycle.LifecycleOwner
import dagger.BindsInstance
import dagger.Subcomponent
import javax.inject.Named

@Subcomponent
interface LockScreenComponent {

  fun inject(activity: LockScreenActivity)

  @CheckResult
  fun plusFragmentComponent(): LockScreenFragmentComponent

  @Subcomponent.Builder
  interface Builder {

    @BindsInstance fun owner(owner: LifecycleOwner): Builder

    @BindsInstance fun packageName(@Named("locked_package_name") packageName: String): Builder

    @BindsInstance fun activityName(@Named("locked_activity_name") activityName: String): Builder

    @BindsInstance fun realName(@Named("locked_real_name") realName: String): Builder

    fun build(): LockScreenComponent
  }

  @Subcomponent
  interface LockScreenFragmentComponent {

    fun inject(fragment: LockScreenPatternFragment)

    fun inject(fragment: LockScreenTextFragment)

  }
}
