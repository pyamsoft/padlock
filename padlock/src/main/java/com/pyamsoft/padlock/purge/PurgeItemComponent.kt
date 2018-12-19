package com.pyamsoft.padlock.purge

import android.view.View
import androidx.annotation.CheckResult
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.Subcomponent

@Subcomponent(modules = [PurgeItemProvider::class, PurgeItemModule::class])
interface PurgeItemComponent {

  fun inject(holder: PurgeItem.ViewHolder)
}

@Module
abstract class PurgeItemModule {

  @Binds
  @CheckResult
  internal abstract fun bindView(impl: PurgeItemViewImpl): PurgeItemView

}

@Module
class PurgeItemProvider(
  private val view: View
) {

  @Provides
  @CheckResult
  fun provideView(): View = view

}
