package com.pyamsoft.padlock.purge

import android.view.View
import androidx.annotation.CheckResult
import com.pyamsoft.padlock.purge.PurgeItemComponent.PurgeModule
import dagger.Binds
import dagger.BindsInstance
import dagger.Module
import dagger.Subcomponent

@Subcomponent(modules = [PurgeModule::class])
interface PurgeItemComponent {

  fun inject(holder: PurgeItem.ViewHolder)

  @Subcomponent.Builder
  interface Builder {

    @BindsInstance fun itemView(itemView: View): Builder

    fun build(): PurgeItemComponent
  }

  @Module
  abstract class PurgeModule {

    @Binds
    @CheckResult
    internal abstract fun bindItemView(impl: PurgeItemViewImpl): PurgeItemView

  }
}

