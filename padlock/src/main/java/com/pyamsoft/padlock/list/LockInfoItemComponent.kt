package com.pyamsoft.padlock.list

import android.view.View
import com.pyamsoft.padlock.list.LockInfoItemComponent.LockInfoModule
import dagger.Binds
import dagger.BindsInstance
import dagger.Module
import dagger.Subcomponent

@Subcomponent(modules = [LockInfoModule::class])
interface LockInfoItemComponent {

  fun inject(holder: LockInfoItem.ViewHolder)

  fun inject(holder: LockInfoGroup.ViewHolder)

  @Subcomponent.Builder
  interface Builder {

    @BindsInstance fun itemView(itemView: View): Builder

    fun build(): LockInfoItemComponent
  }

  @Module
  abstract class LockInfoModule {

    @Binds
    internal abstract fun bindItemView(impl: LockInfoItemViewImpl): LockInfoItemView

    @Binds
    internal abstract fun bindGroupView(impl: LockInfoGroupViewImpl): LockInfoGroupView

  }
}

