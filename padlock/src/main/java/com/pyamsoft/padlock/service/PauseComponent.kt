package com.pyamsoft.padlock.service

import dagger.Subcomponent

@Subcomponent(modules = [PauseModule::class])
interface PauseComponent {

  fun inject(activity: PauseConfirmActivity)

}
