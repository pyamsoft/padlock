package com.pyamsoft.padlock.pin

import dagger.Subcomponent

@Subcomponent(modules = [PinModule::class])
interface PinComponent {

  fun inject(dialog: PinDialog)

  fun inject(fragment: PinBaseFragment)
}
