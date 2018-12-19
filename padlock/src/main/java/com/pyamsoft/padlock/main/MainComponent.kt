package com.pyamsoft.padlock.main

import androidx.annotation.CheckResult
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.Subcomponent

@Subcomponent(modules = [MainProvider::class, MainModule::class])
interface MainComponent {

  fun inject(activity: MainActivity)

}

@Module
abstract class MainModule {

  @Binds
  @CheckResult
  internal abstract fun bind(impl: MainViewImpl): MainView

}

@Module
class MainProvider(
  private val activity: MainActivity
) {

  @Provides
  @CheckResult
  fun provideActivity(): MainActivity = activity

}
