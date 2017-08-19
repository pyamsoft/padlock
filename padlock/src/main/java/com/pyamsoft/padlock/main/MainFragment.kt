/*
 * Copyright 2017 Peter Kenji Yamanaka
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
 */

package com.pyamsoft.padlock.main

import android.os.Bundle
import android.support.annotation.CheckResult
import android.support.design.widget.BottomNavigationView
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import com.pyamsoft.padlock.R
import com.pyamsoft.padlock.databinding.FragmentMainBinding
import com.pyamsoft.padlock.list.LockListFragment
import com.pyamsoft.padlock.purge.PurgeFragment
import com.pyamsoft.padlock.settings.SettingsFragment
import com.pyamsoft.padlock.uicommon.CanaryFragment
import timber.log.Timber

class MainFragment : CanaryFragment() {

  private lateinit var binding: FragmentMainBinding

  override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
      savedInstanceState: Bundle?): View? {
    binding = FragmentMainBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    setupBottomNavigation()

    if (childFragmentManager.findFragmentById(R.id.main_view_container) == null) {
      Timber.d("Load default Tab: List")
      binding.bottomTabs.menu.performIdentifierAction(R.id.menu_locklist, 0)
    }
  }

  private fun setupBottomNavigation() {
    binding.bottomTabs.setOnNavigationItemSelectedListener(
        object : BottomNavigationView.OnNavigationItemSelectedListener {
          override fun onNavigationItemSelected(item: MenuItem): Boolean {
            val handled: Boolean
            when (item.itemId) {
              R.id.menu_locklist -> handled = replaceFragment(LockListFragment(),
                  LockListFragment.TAG)
              R.id.menu_settings -> handled = replaceFragment(SettingsFragment(),
                  SettingsFragment.TAG)
              R.id.menu_purge -> handled = replaceFragment(PurgeFragment(), PurgeFragment.TAG)
              else -> handled = false
            }

            if (handled) {
              item.isChecked = !item.isChecked
            }

            return handled
          }

          @CheckResult
          private fun replaceFragment(fragment: Fragment, tag: String): Boolean {
            val fragmentManager = childFragmentManager
            if (fragmentManager.findFragmentByTag(tag) == null) {
              fragmentManager.beginTransaction()
                  .replace(R.id.main_view_container, fragment, tag)
                  .commit()
              return true
            } else {
              return false
            }
          }
        })
  }

  override fun onResume() {
    super.onResume()
    setActionBarUpEnabled(false)
  }

  override fun onDestroyView() {
    super.onDestroyView()
    binding.unbind()
  }

  companion object {

    val TAG = "MainFragment"
  }
}

