package com.pyamsoft.padlock.purge

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CheckResult
import androidx.lifecycle.LifecycleOwner
import com.pyamsoft.padlock.model.list.ListDiffProvider
import com.pyamsoft.padlock.purge.PurgeComponent.PurgeModule
import com.pyamsoft.pydroid.ui.app.activity.ToolbarActivity
import dagger.Binds
import dagger.BindsInstance
import dagger.Module
import dagger.Subcomponent

@Subcomponent(modules = [PurgeModule::class])
interface PurgeComponent {

  fun inject(fragment: PurgeFragment)

  fun inject(holder: PurgeItem.ViewHolder)

  @Subcomponent.Builder
  interface Builder {

    @BindsInstance fun toolbarActivity(toolbarActivity: ToolbarActivity): Builder

    @BindsInstance fun owner(owner: LifecycleOwner): Builder

    @BindsInstance fun inflater(inflater: LayoutInflater): Builder

    @BindsInstance fun container(container: ViewGroup?): Builder

    @BindsInstance fun savedInstanceState(savedInstanceState: Bundle?): Builder

    @BindsInstance fun diffProvider(diffProvider: ListDiffProvider<String>): Builder

    @BindsInstance fun itemView(itemView: View): Builder

    fun build(): PurgeComponent
  }

  @Module
  abstract class PurgeModule {

    @Binds
    @CheckResult
    internal abstract fun bindView(impl: PurgeViewImpl): PurgeView

    @Binds
    @CheckResult
    internal abstract fun bindItemView(impl: PurgeItemViewImpl): PurgeItemView

  }
}

