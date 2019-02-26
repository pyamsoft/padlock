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
import com.pyamsoft.padlock.scopes.FragmentScope
import com.pyamsoft.padlock.service.pause.PauseConfirmActivity
import com.pyamsoft.pydroid.ui.theme.Theming
import dagger.Binds
import dagger.BindsInstance
import dagger.Module
import dagger.Provides
import dagger.Subcomponent

@FragmentScope
@Subcomponent(modules = [PinModule::class, PinProvider::class])
interface PinComponent {

  fun inject(dialog: PauseConfirmActivity)

  fun inject(dialog: PinConfirmDialog)

  fun inject(dialog: PinCreateDialog)

  @Subcomponent.Builder
  interface Builder {

    @BindsInstance
    fun owner(owner: LifecycleOwner): Builder

    @BindsInstance
    fun parent(parent: ViewGroup): Builder

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

  }

  @Module
  object PinProvider {

    @Provides
    @JvmStatic
    @CheckResult
    internal fun provideConfirmPinView(
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
        TYPE_TEXT -> TextConfirmPinView(owner, parent, callback)
      }
    }

    @Provides
    @JvmStatic
    @CheckResult
    internal fun provideCreatePinView(
      theming: Theming,
      preferences: LockScreenPreferences,
      parent: ViewGroup,
      callback: CreatePinView.Callback
    ): CreatePinView {
      return when (preferences.getCurrentLockType()) {
        TYPE_PATTERN -> PatternCreatePinView(parent, callback, themeColor(theming, parent.context))
        TYPE_TEXT -> TextCreatePinView(parent, callback)
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

