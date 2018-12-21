package com.pyamsoft.padlock.list

import android.view.View
import androidx.lifecycle.LifecycleOwner
import com.pyamsoft.padlock.list.LockListItemComponent.LockListModule
import dagger.Binds
import dagger.BindsInstance
import dagger.Module
import dagger.Subcomponent

@Subcomponent(modules = [LockListModule::class])
interface LockListItemComponent {

  fun inject(holder: LockListItem.ViewHolder)

  @Subcomponent.Builder
  interface Builder {

    @BindsInstance fun owner(owner: LifecycleOwner): Builder

    @BindsInstance fun itemView(itemView: View): Builder

    fun build(): LockListItemComponent
  }

  @Module
  abstract class LockListModule {

    @Binds
    internal abstract fun bindItemView(impl: LockListItemViewImpl): LockListItemView

  }
}

