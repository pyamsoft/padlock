package com.pyamsoft.padlock.service

import dagger.Subcomponent

@Subcomponent(modules = [ServiceModule::class])
interface ServiceComponent {

  fun inject(service: PadLockService)

  fun inject(service: PauseService)

  fun inject(service: ResumeService)
}
