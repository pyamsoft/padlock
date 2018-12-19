package com.pyamsoft.padlock.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.CheckResult
import androidx.lifecycle.LifecycleOwner
import com.pyamsoft.padlock.main.MainFragmentComponent.MainModule
import dagger.Binds
import dagger.BindsInstance
import dagger.Module
import dagger.Subcomponent

@Subcomponent(modules = [MainModule::class])
interface MainFragmentComponent {

  fun inject(fragment: MainFragment)

  @Subcomponent.Builder
  interface Builder {

    @BindsInstance fun owner(owner: LifecycleOwner): Builder

    @BindsInstance fun inflater(inflater: LayoutInflater): Builder

    @BindsInstance fun container(container: ViewGroup?): Builder

    @BindsInstance fun savedInstanceState(savedInstanceState: Bundle?): Builder

    fun build(): MainFragmentComponent
  }

  @Module
  abstract class MainModule {

    @Binds
    @CheckResult
    internal abstract fun bindView(impl: MainFragmentViewImpl): MainFragmentView

  }

}
