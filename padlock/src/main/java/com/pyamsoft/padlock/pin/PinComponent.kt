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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.CheckResult
import androidx.lifecycle.LifecycleOwner
import com.pyamsoft.padlock.pin.PinComponent.PinModule
import dagger.Binds
import dagger.BindsInstance
import dagger.Module
import dagger.Subcomponent

@Subcomponent(modules = [PinModule::class])
interface PinComponent {

  fun inject(dialog: PinDialog)

  fun inject(fragment: PinPatternFragment)

  fun inject(fragment: PinTextFragment)

  @Subcomponent.Builder
  interface Builder {

    @BindsInstance fun owner(owner: LifecycleOwner): Builder

    @BindsInstance fun inflater(inflater: LayoutInflater): Builder

    @BindsInstance fun container(container: ViewGroup?): Builder

    @BindsInstance fun savedInstanceState(savedInstanceState: Bundle?): Builder

    fun build(): PinComponent
  }

  @Module
  abstract class PinModule {

    @Binds
    @CheckResult
    internal abstract fun bindView(impl: PinViewImpl): PinView

    @Binds
    @CheckResult
    internal abstract fun bindPatternView(impl: PinPatternViewImpl): PinPatternView

    @Binds
    @CheckResult
    internal abstract fun bindTextView(impl: PinTextViewImpl): PinTextView

  }
}

