package com.pyamsoft.padlock.purge

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.CheckResult
import androidx.lifecycle.LifecycleOwner
import com.pyamsoft.padlock.model.list.ListDiffProvider
import com.pyamsoft.pydroid.ui.app.activity.ToolbarActivity
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.Subcomponent

@Subcomponent(modules = [PurgeProvider::class, PurgeModule::class])
interface PurgeComponent {

  fun inject(fragment: PurgeFragment)
}

@Module
abstract class PurgeModule {

  @Binds
  @CheckResult
  internal abstract fun bindView(impl: PurgeViewImpl): PurgeView

}

@Module
class PurgeProvider(
  private val toolbarActivity: ToolbarActivity,
  private val owner: LifecycleOwner,
  private val inflater: LayoutInflater,
  private val container: ViewGroup?,
  private val savedInstanceState: Bundle?,
  private val diffProvider: ListDiffProvider<String>
) {

  @Provides
  @CheckResult
  fun provideToolbarActivity(): ToolbarActivity = toolbarActivity

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

  @Provides
  @CheckResult
  fun provideDiffProvider(): ListDiffProvider<String> = diffProvider
}
