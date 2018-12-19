package com.pyamsoft.padlock.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.CheckResult
import androidx.lifecycle.LifecycleOwner
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.Subcomponent

@Subcomponent(modules = [MainViewProvider::class, MainViewModule::class])
interface MainViewComponent {

  fun inject(fragment: MainFragment)

}

@Module
abstract class MainViewModule {

  @Binds
  @CheckResult
  internal abstract fun bindView(impl: MainFragmentViewImpl): MainFragmentView

}

@Module
class MainViewProvider(
  private val owner: LifecycleOwner,
  private val inflater: LayoutInflater,
  private val container: ViewGroup?,
  private val savedInstanceState: Bundle?
) {

  @Provides
  @CheckResult
  fun provideOwner(): LifecycleOwner = owner

  @Provides
  @CheckResult
  fun provideInflater(): LayoutInflater = inflater

  @Provides
  @CheckResult
  fun provideContainer(): ViewGroup? = container

  @Provides
  @CheckResult
  fun provideSavedInstanceState(): Bundle? = savedInstanceState

}
