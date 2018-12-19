package com.pyamsoft.padlock.main

import androidx.annotation.CheckResult
import com.pyamsoft.padlock.main.MainComponent.MainModule
import dagger.Binds
import dagger.BindsInstance
import dagger.Module
import dagger.Subcomponent

@Subcomponent(modules = [MainModule::class])
interface MainComponent {

  fun inject(activity: MainActivity)

  @Subcomponent.Builder
  interface Builder {

    @BindsInstance fun mainActivity(mainActivity: MainActivity): Builder

    fun build(): MainComponent
  }

  @Module
  abstract class MainModule {

    @Binds
    @CheckResult
    internal abstract fun bind(impl: MainViewImpl): MainView

  }

}
