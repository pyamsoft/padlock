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

package com.pyamsoft.padlock.pin

import android.content.Context
import android.view.ViewGroup
import androidx.annotation.CheckResult
import androidx.annotation.ColorRes
import androidx.core.content.withStyledAttributes
import androidx.lifecycle.LifecycleOwner
import com.pyamsoft.padlock.R
import com.pyamsoft.padlock.api.preferences.LockScreenPreferences
import com.pyamsoft.padlock.model.LockScreenType.TYPE_PATTERN
import com.pyamsoft.padlock.model.LockScreenType.TYPE_TEXT
import com.pyamsoft.padlock.pin.PinComponent.PinModule
import com.pyamsoft.padlock.pin.PinComponent.PinProvider
import com.pyamsoft.padlock.pin.confirm.PinConfirmDialog
import com.pyamsoft.padlock.pin.confirm.PinConfirmDialogPresenter
import com.pyamsoft.padlock.pin.confirm.PinConfirmDialogPresenterImpl
import com.pyamsoft.padlock.pin.confirm.PinConfirmUiComponent
import com.pyamsoft.padlock.pin.confirm.PinConfirmUiComponentImpl
import com.pyamsoft.padlock.pin.create.PinCreateDialog
import com.pyamsoft.padlock.pin.create.PinCreateDialogPresenter
import com.pyamsoft.padlock.pin.create.PinCreateDialogPresenterImpl
import com.pyamsoft.padlock.pin.create.PinCreateUiComponent
import com.pyamsoft.padlock.pin.create.PinCreateUiComponentImpl
import com.pyamsoft.padlock.pin.pattern.PatternConfirmPinView
import com.pyamsoft.padlock.pin.pattern.PatternCreatePinView
import com.pyamsoft.padlock.pin.text.TextConfirmPinView
import com.pyamsoft.padlock.pin.text.TextCreatePinView
import com.pyamsoft.padlock.pin.toolbar.PinToolbar
import com.pyamsoft.padlock.pin.toolbar.PinToolbarPresenter
import com.pyamsoft.padlock.pin.toolbar.PinToolbarPresenterImpl
import com.pyamsoft.padlock.pin.toolbar.PinToolbarUiComponent
import com.pyamsoft.padlock.pin.toolbar.PinToolbarUiComponentImpl
import com.pyamsoft.padlock.scopes.FragmentScope
import com.pyamsoft.padlock.service.pause.PauseConfirmActivity
import com.pyamsoft.pydroid.loader.ImageLoader
import com.pyamsoft.pydroid.ui.theme.Theming
import dagger.Binds
import dagger.BindsInstance
import dagger.Module
import dagger.Provides
import dagger.Subcomponent
import javax.inject.Named

@FragmentScope
@Subcomponent(modules = [PinModule::class, PinProvider::class])
interface PinComponent {

  fun inject(dialog: PauseConfirmActivity)

  fun inject(dialog: PinConfirmDialog)

  fun inject(dialog: PinCreateDialog)

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
    fun finishOnDismiss(@Named("finish_on_dismiss") finishOnDismiss: Boolean): Builder

    @CheckResult
    fun build(): PinComponent
  }

  @Module
  abstract class PinModule {

    @Binds
    internal abstract fun bindPinCreateDialogPresenter(impl: PinCreateDialogPresenterImpl): PinCreateDialogPresenter

    @Binds
    internal abstract fun bindCreateViewCallback(impl: PinCreateDialogPresenterImpl): CreatePinView.Callback

    @Binds
    internal abstract fun bindPinConfirmDialogPresenter(impl: PinConfirmDialogPresenterImpl): PinConfirmDialogPresenter

    @Binds
    internal abstract fun bindConfirmViewCallback(impl: PinConfirmDialogPresenterImpl): ConfirmPinView.Callback

    @Binds
    internal abstract fun bindToolbarPresenter(impl: PinToolbarPresenterImpl): PinToolbarPresenter

    @Binds
    internal abstract fun bindToolbarViewCallback(impl: PinToolbarPresenterImpl): PinToolbar.Callback

    @Binds
    internal abstract fun bindPinToolbarComponent(impl: PinToolbarUiComponentImpl): PinToolbarUiComponent

    @Binds
    internal abstract fun bindPinCreateComponent(impl: PinCreateUiComponentImpl): PinCreateUiComponent

    @Binds
    internal abstract fun bindPinConfirmComponent(impl: PinConfirmUiComponentImpl): PinConfirmUiComponent

  }

  @Module
  object PinProvider {

    @Provides
    @JvmStatic
    @CheckResult
    internal fun provideConfirmPinView(
      imageLoader: ImageLoader,
      owner: LifecycleOwner,
      theming: Theming,
      preferences: LockScreenPreferences,
      parent: ViewGroup,
      callback: ConfirmPinView.Callback
    ): ConfirmPinView {
      return when (preferences.getCurrentLockType()) {
        TYPE_PATTERN -> PatternConfirmPinView(
            owner, parent, callback, themeColor(theming, parent.context)
        )
        TYPE_TEXT -> TextConfirmPinView(
            imageLoader, owner, parent, callback
        )
      }
    }

    @Provides
    @JvmStatic
    @CheckResult
    internal fun provideCreatePinView(
      imageLoader: ImageLoader,
      owner: LifecycleOwner,
      theming: Theming,
      preferences: LockScreenPreferences,
      parent: ViewGroup,
      callback: CreatePinView.Callback
    ): CreatePinView {
      return when (preferences.getCurrentLockType()) {
        TYPE_PATTERN -> PatternCreatePinView(
            owner, parent, callback, themeColor(theming, parent.context)
        )
        TYPE_TEXT -> TextCreatePinView(
            imageLoader, owner, parent, callback
        )
      }
    }

    @CheckResult
    @JvmStatic
    @ColorRes
    private fun themeColor(
      theming: Theming,
      context: Context
    ): Int {
      val theme: Int
      if (theming.isDarkTheme()) {
        theme = R.style.Theme_PadLock_Dark_Dialog
      } else {
        theme = R.style.Theme_PadLock_Light_Dialog
      }

      @ColorRes var color = 0
      val attrs = intArrayOf(android.R.attr.colorForeground)
      context.withStyledAttributes(theme, attrs) { color = getResourceId(0, 0) }
      return color
    }
  }
}

