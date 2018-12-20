package com.pyamsoft.padlock.lock

import android.view.LayoutInflater
import androidx.lifecycle.Lifecycle
import com.pyamsoft.padlock.lock.LockStatsComponent.LockStatsModule
import dagger.Binds
import dagger.BindsInstance
import dagger.Module
import dagger.Subcomponent

@Subcomponent(modules = [LockStatsModule::class])
interface LockStatsComponent {

  fun inject(dialog: LockedStatDialog)

  @Module
  abstract class LockStatsModule {

    @Binds
    internal abstract fun bindView(impl: LockStatViewImpl): LockStatView

  }

  @Subcomponent.Builder
  interface Builder {

    @BindsInstance fun lifecycle(lifecycle: Lifecycle): Builder

    @BindsInstance fun inflater(inflater: LayoutInflater): Builder

    fun build(): LockStatsComponent

  }

}
