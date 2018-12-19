package com.pyamsoft.padlock.pin

import androidx.annotation.CheckResult
import androidx.lifecycle.LifecycleOwner
import dagger.Module
import dagger.Provides
import dagger.Subcomponent

@Subcomponent(modules = [PinModule::class])
interface PinComponent {

  fun inject(dialog: PinDialog)

  fun inject(fragment: PinBaseFragment)
}

@Module
class PinModule(private val owner: LifecycleOwner) {

  @Provides
  @CheckResult
  fun provideOwner(): LifecycleOwner = owner
}
