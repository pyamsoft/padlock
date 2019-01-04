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

package com.pyamsoft.padlock.settings

import androidx.annotation.CheckResult
import androidx.preference.PreferenceScreen
import com.pyamsoft.padlock.settings.SettingsComponent.SettingsModule
import dagger.Binds
import dagger.BindsInstance
import dagger.Module
import dagger.Subcomponent

@Subcomponent(modules = [SettingsModule::class])
interface SettingsComponent {

  fun inject(fragment: PadLockPreferenceFragment)

  @Subcomponent.Builder
  interface Builder {

    @BindsInstance fun preferenceScreen(preferenceScreen: PreferenceScreen): Builder

    fun build(): SettingsComponent
  }

  @Module
  abstract class SettingsModule {

    @Binds
    @CheckResult
    internal abstract fun bindSettingsView(impl: SettingsViewImpl): SettingsView
  }
}
