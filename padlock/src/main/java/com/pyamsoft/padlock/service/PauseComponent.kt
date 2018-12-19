package com.pyamsoft.padlock.service

import com.pyamsoft.padlock.service.PauseComponent.PauseModule
import dagger.Binds
import dagger.BindsInstance
import dagger.Module
import dagger.Subcomponent

@Subcomponent(modules = [PauseModule::class])
interface PauseComponent {

  fun inject(activity: PauseConfirmActivity)

  @Subcomponent.Builder
  interface Builder {

    @BindsInstance fun activity(activity: PauseConfirmActivity): Builder

    fun build(): PauseComponent

  }

  @Module
  abstract class PauseModule {

    @Binds
    internal abstract fun bindPauseView(impl: PauseViewImpl): PauseView
  }

}
