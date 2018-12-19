package com.pyamsoft.padlock.service

import androidx.annotation.CheckResult
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.Subcomponent

@Subcomponent(modules = [PauseModule::class, PauseProvider::class])
interface PauseComponent {

  fun inject(activity: PauseConfirmActivity)

}

@Module
class PauseProvider(private val activity: PauseConfirmActivity) {

  @Provides
  @CheckResult
  fun provideActivity(): PauseConfirmActivity = activity
}

@Module
abstract class PauseModule {

  @Binds
  internal abstract fun bindPauseView(impl: PauseViewImpl): PauseView
}
