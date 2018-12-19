/*
 * Copyright (C) 2018 Peter Kenji Yamanaka
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pyamsoft.padlock.purge

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.CheckResult
import androidx.lifecycle.LifecycleOwner
import com.pyamsoft.padlock.model.list.ListDiffProvider
import com.pyamsoft.pydroid.ui.app.activity.ToolbarActivity
import dagger.Module
import dagger.Provides

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
