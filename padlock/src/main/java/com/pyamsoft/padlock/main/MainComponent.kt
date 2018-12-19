package com.pyamsoft.padlock.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.CheckResult
import androidx.lifecycle.LifecycleOwner
import com.pyamsoft.padlock.main.MainComponent.MainModule
import dagger.Binds
import dagger.BindsInstance
import dagger.Module
import dagger.Subcomponent

@Subcomponent(modules = [MainModule::class])
interface MainComponent {

  fun inject(activity: MainActivity)

  fun inject(fragment: MainFragment)

  @Subcomponent.Builder
  interface Builder {

    @BindsInstance fun mainActivity(mainActivity: MainActivity): Builder

    @BindsInstance fun owner(owner: LifecycleOwner): Builder

    @BindsInstance fun inflater(inflater: LayoutInflater): Builder

    @BindsInstance fun container(container: ViewGroup?): Builder

    @BindsInstance fun savedInstanceState(savedInstanceState: Bundle?): Builder

    fun build(): MainComponent
  }

  @Module
  abstract class MainModule {

    @Binds
    @CheckResult
    internal abstract fun bind(impl: MainViewImpl): MainView

    @Binds
    @CheckResult
    internal abstract fun bindView(impl: MainFragmentViewImpl): MainFragmentView

  }

}
