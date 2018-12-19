package com.pyamsoft.padlock.pin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.CheckResult
import androidx.lifecycle.LifecycleOwner
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.Subcomponent

@Subcomponent(modules = [PinProvider::class, PinModule::class])
interface PinComponent {

  fun inject(dialog: PinDialog)

  fun inject(fragment: PinPatternFragment)

  fun inject(fragment: PinTextFragment)
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

@Module
class PinProvider(
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
