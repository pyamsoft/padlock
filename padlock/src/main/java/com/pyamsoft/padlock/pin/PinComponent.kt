package com.pyamsoft.padlock.pin

import dagger.Subcomponent

@Subcomponent(modules = [PinModule::class])
interface PinComponent {

  fun inject(fragment: PinBaseFragment)
}
