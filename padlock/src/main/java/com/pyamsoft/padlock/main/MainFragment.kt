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

package com.pyamsoft.padlock.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CheckResult
import androidx.fragment.app.Fragment
import com.pyamsoft.padlock.Injector
import com.pyamsoft.padlock.PadLockComponent
import com.pyamsoft.padlock.R
import com.pyamsoft.padlock.list.LockListFragment
import com.pyamsoft.padlock.purge.PurgeFragment
import com.pyamsoft.padlock.settings.SettingsFragment
import com.pyamsoft.pydroid.ui.app.fragment.ToolbarFragment
import com.pyamsoft.pydroid.ui.app.fragment.requireToolbarActivity
import com.pyamsoft.pydroid.ui.util.commit
import com.pyamsoft.pydroid.ui.util.setUpEnabled
import javax.inject.Inject

class MainFragment : ToolbarFragment() {

  @field:Inject internal lateinit var mainView: MainFragmentView

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    Injector.obtain<PadLockComponent>(requireActivity().applicationContext)
        .plusMainFragmentComponent()
        .owner(viewLifecycleOwner)
        .inflater(inflater)
        .container(container)
        .savedInstanceState(savedInstanceState)
        .build()
        .inject(this)

    mainView.create()
    return mainView.root()
  }

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)

    mainView.onBottomNavigationClicked {
      return@onBottomNavigationClicked when (it) {
        R.id.menu_locklist -> replaceFragment(LockListFragment(), LockListFragment.TAG)
        R.id.menu_settings -> replaceFragment(SettingsFragment(), SettingsFragment.TAG)
        R.id.menu_purge -> replaceFragment(PurgeFragment(), PurgeFragment.TAG)
        else -> false
      }
    }

    if (childFragmentManager.findFragmentById(R.id.main_view_container) == null) {
      mainView.loadDefaultPage()
    }
  }

  @CheckResult
  private fun replaceFragment(
    fragment: Fragment,
    tag: String
  ): Boolean {
    val containerId = R.id.main_view_container
    val fragmentManager = childFragmentManager
    val currentFragment: Fragment? = fragmentManager.findFragmentById(containerId)

    // Do nothing on same fragment
    if (currentFragment != null && currentFragment.tag == tag) {
      return false
    }

    fragmentManager.beginTransaction()
        .replace(containerId, fragment, tag)
        .commit(viewLifecycleOwner)
    return true
  }

  override fun onResume() {
    super.onResume()
    requireToolbarActivity().withToolbar {
      it.setTitle(R.string.app_name)
      it.setUpEnabled(false)
    }
  }

  companion object {

    const val TAG = "MainFragment"
  }
}
