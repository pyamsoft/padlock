package com.pyamsoft.padlock.settings

import dagger.Subcomponent

@Subcomponent(modules = [SettingsProvider::class, SettingsModule::class])
interface SettingsComponent {

  fun inject(fragment: PadLockPreferenceFragment)
}
