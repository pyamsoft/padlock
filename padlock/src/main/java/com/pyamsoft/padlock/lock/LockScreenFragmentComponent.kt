package com.pyamsoft.padlock.lock

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import com.pyamsoft.padlock.lock.LockScreenFragmentComponent.LockScreenModule
import dagger.Binds
import dagger.BindsInstance
import dagger.Module
import dagger.Subcomponent

@Subcomponent(modules = [LockScreenModule::class])
interface LockScreenFragmentComponent {

  fun inject(fragment: LockScreenPatternFragment)

  fun inject(fragment: LockScreenTextFragment)

  @Module
  abstract class LockScreenModule {

    @Binds
    internal abstract fun bindPatternView(impl: LockScreenPatternViewImpl): LockScreenPatternView

    @Binds
    internal abstract fun bindTextView(impl: LockScreenTextViewImpl): LockScreenTextView
  }

  @Subcomponent.Builder
  interface Builder {

    @BindsInstance fun owner(owner: LifecycleOwner): Builder

    @BindsInstance fun inflater(inflater: LayoutInflater): Builder

    @BindsInstance fun container(container: ViewGroup?): Builder

    @BindsInstance fun savedInstanceState(savedInstanceState: Bundle?): Builder

    fun build(): LockScreenFragmentComponent

  }

}
