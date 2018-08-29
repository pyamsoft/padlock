package com.pyamsoft.padlock.settings

import dagger.Subcomponent

@Subcomponent(modules = [SettingsModule::class])
interface SettingsComponent {

  fun inject(fragment: PadLockPreferenceFragment)
}
