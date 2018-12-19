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
