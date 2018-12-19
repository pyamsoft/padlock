package com.pyamsoft.padlock.pin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.CheckResult
import androidx.fragment.app.FragmentActivity
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.Subcomponent

@Subcomponent(modules = [PinPatternProvider::class, PinPatternModule::class])
interface PinPatternComponent {

  fun inject(fragment: PinPatternFragment)
}

@Module
abstract class PinPatternModule {

  @Binds
  @CheckResult
  internal abstract fun bindView(impl: PinPatternViewImpl): PinPatternView

}

@Module
class PinPatternProvider(
  private val activity: FragmentActivity,
  private val inflater: LayoutInflater,
  private val container: ViewGroup?
) {

  @Provides
  @CheckResult
  fun provideActivity(): FragmentActivity = activity

  @Provides
  @CheckResult
  fun provideInflater(): LayoutInflater = inflater

  @Provides
  @CheckResult
  fun provideContainer(): ViewGroup? = container

}
