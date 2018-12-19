package com.pyamsoft.padlock.pin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.CheckResult
import androidx.lifecycle.LifecycleOwner
import com.pyamsoft.padlock.pin.PinComponent.PinModule
import dagger.Binds
import dagger.BindsInstance
import dagger.Module
import dagger.Subcomponent

@Subcomponent(modules = [PinModule::class])
interface PinComponent {

  fun inject(dialog: PinDialog)

  fun inject(fragment: PinPatternFragment)

  fun inject(fragment: PinTextFragment)

  @Subcomponent.Builder
  interface Builder {

    @BindsInstance fun owner(owner: LifecycleOwner): Builder

    @BindsInstance fun inflater(inflater: LayoutInflater): Builder

    @BindsInstance fun container(container: ViewGroup?): Builder

    @BindsInstance fun savedInstanceState(savedInstanceState: Bundle?): Builder

    fun build(): PinComponent
  }

  @Module
  abstract class PinModule {

    @Binds
    @CheckResult
    internal abstract fun bindView(impl: PinViewImpl): PinView

    @Binds
    @CheckResult
    internal abstract fun bindPatternView(impl: PinPatternViewImpl): PinPatternView

    @Binds
    @CheckResult
    internal abstract fun bindTextView(impl: PinTextViewImpl): PinTextView

  }
}

