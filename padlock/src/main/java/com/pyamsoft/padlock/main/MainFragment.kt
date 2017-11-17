/*
 *     Copyright (C) 2017 Peter Kenji Yamanaka
 *
 *     This program is free software; you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation; either version 2 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License along
 *     with this program; if not, write to the Free Software Foundation, Inc.,
 *     51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
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
import com.pyamsoft.pydroid.presenter.Presenter
import timber.log.Timber

class MainFragment : CanaryFragment() {

    override fun provideBoundPresenters(): List<Presenter<*>> = emptyList()

    private lateinit var binding: FragmentMainBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?): View? {
        binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
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
                        val handled: Boolean = when (item.itemId) {
                            R.id.menu_locklist -> replaceFragment(LockListFragment(),
                                    LockListFragment.TAG)
                            R.id.menu_settings -> replaceFragment(SettingsFragment(),
                                    SettingsFragment.TAG)
                            R.id.menu_purge -> replaceFragment(PurgeFragment(), PurgeFragment.TAG)
                            else -> false
                        }

                        if (handled) {
                            item.isChecked = !item.isChecked
                        }

                        return handled
                    }

                    @CheckResult
                    private fun replaceFragment(fragment: Fragment, tag: String): Boolean {
                        val fragmentManager = childFragmentManager
                        return if (fragmentManager.findFragmentByTag(tag) == null) {
                            fragmentManager.beginTransaction()
                                    .replace(R.id.main_view_container, fragment, tag)
                                    .commit()
                            true
                        } else {
                            false
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

        const val TAG = "MainFragment"
    }
}

