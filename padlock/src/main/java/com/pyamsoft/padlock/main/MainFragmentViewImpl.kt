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

package com.pyamsoft.padlock.main

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle.Event.ON_DESTROY
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import com.pyamsoft.padlock.R
import com.pyamsoft.padlock.databinding.FragmentMainBinding
import javax.inject.Inject

internal class MainFragmentViewImpl @Inject internal constructor(
  private val owner: LifecycleOwner,
  private val inflater: LayoutInflater,
  private val container: ViewGroup?
) : MainFragmentView, LifecycleObserver {

  private lateinit var binding: FragmentMainBinding

  init {
    owner.lifecycle.addObserver(this)
  }

  @Suppress("unused")
  @OnLifecycleEvent(ON_DESTROY)
  internal fun destroy() {
    owner.lifecycle.removeObserver(this)

    binding.unbind()
  }

  override fun root(): View {
    return binding.root
  }

  override fun create() {
    binding = FragmentMainBinding.inflate(inflater, container, false)

    binding.bottomTabs.inflateMenu(R.menu.navigation_menu)
  }

  override fun onBottomNavigationClicked(onClick: (id: Int) -> Boolean) {
    binding.bottomTabs.setOnNavigationItemSelectedListener { item ->
      val handled = onClick(item.itemId)
      if (handled) {
        item.isChecked = !item.isChecked
      }
      return@setOnNavigationItemSelectedListener handled
    }
  }

  override fun loadDefaultPage() {
    binding.bottomTabs.menu.performIdentifierAction(R.id.menu_locklist, 0)
  }

}
