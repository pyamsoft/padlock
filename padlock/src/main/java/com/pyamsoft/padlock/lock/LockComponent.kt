/*
 * Copyright 2019 Peter Kenji Yamanaka
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.pyamsoft.padlock.lock

import android.view.ViewGroup
import androidx.annotation.CheckResult
import androidx.lifecycle.LifecycleOwner
import com.pyamsoft.padlock.R.color
import com.pyamsoft.padlock.api.preferences.LockScreenPreferences
import com.pyamsoft.padlock.lock.LockComponent.LockModule
import com.pyamsoft.padlock.lock.LockComponent.LockProvider
import com.pyamsoft.padlock.model.LockScreenType.TYPE_PATTERN
import com.pyamsoft.padlock.model.LockScreenType.TYPE_TEXT
import com.pyamsoft.padlock.pin.ConfirmPinView
import com.pyamsoft.padlock.pin.pattern.PatternConfirmPinView
import com.pyamsoft.padlock.pin.text.TextConfirmPinView
import com.pyamsoft.padlock.scopes.FragmentScope
import com.pyamsoft.pydroid.loader.ImageLoader
import com.pyamsoft.pydroid.ui.app.ToolbarActivityProvider
import dagger.Binds
import dagger.BindsInstance
import dagger.Module
import dagger.Provides
import dagger.Subcomponent
import javax.inject.Named

@FragmentScope
@Subcomponent(modules = [LockProvider::class, LockModule::class])
interface LockComponent {

  fun inject(activity: LockScreenActivity)

  @Subcomponent.Builder
  interface Builder {

    @BindsInstance
    @CheckResult
    fun owner(owner: LifecycleOwner): Builder

    @BindsInstance
    @CheckResult
    fun parent(parent: ViewGroup): Builder

    @BindsInstance
    @CheckResult
    fun toolbarActivityProvider(provider: ToolbarActivityProvider): Builder

    @BindsInstance
    @CheckResult
    fun lockedCode(@Named("locked_code") lockedCode: String?): Builder

    @BindsInstance
    @CheckResult
    fun lockedSystem(@Named("locked_system") isSystem: Boolean): Builder

    @BindsInstance
    @CheckResult
    fun packageName(@Named("locked_package_name") packageName: String): Builder

    @BindsInstance
    @CheckResult
    fun appIcon(@Named("locked_app_icon") icon: Int): Builder

    @BindsInstance
    @CheckResult
    fun activityName(@Named("locked_activity_name") activityName: String): Builder

    @BindsInstance
    @CheckResult
    fun realName(@Named("locked_real_name") realName: String): Builder

    @CheckResult
    fun build(): LockComponent
  }

  @Module
  object LockProvider {

    @Provides
    @JvmStatic
    @CheckResult
    internal fun provideConfirmPinView(
      imageLoader: ImageLoader,
      owner: LifecycleOwner,
      preferences: LockScreenPreferences,
      parent: ViewGroup,
      callback: ConfirmPinView.Callback
    ): ConfirmPinView {
      return when (preferences.getCurrentLockType()) {
        TYPE_PATTERN -> PatternConfirmPinView(
            owner, parent, callback, color.white
        )
        TYPE_TEXT -> TextConfirmPinView(
            imageLoader, owner, parent, callback
        )
      }
    }
  }

  @Module
  abstract class LockModule {

    @Binds
    internal abstract fun bindLockScreenPresenter(impl: LockScreenPresenterImpl): LockScreenPresenter

    @Binds
    internal abstract fun bindLockScreenToolbarPresenter(impl: LockScreenToolbarPresenterImpl): LockScreenToolbarPresenter

    @Binds
    internal abstract fun bindCallback(impl: LockScreenPresenterImpl): ConfirmPinView.Callback

    @Binds
    internal abstract fun bindToolbarComponent(impl: LockScreenToolbarUiComponentImpl): LockScreenToolbarUiComponent

    @Binds
    internal abstract fun bindComponent(impl: LockScreenUiComponentImpl): LockScreenUiComponent

  }
}

