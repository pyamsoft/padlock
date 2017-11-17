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

package com.pyamsoft.padlock.settings

import com.pyamsoft.pydroid.presenter.Presenter
import com.pyamsoft.pydroid.ui.app.fragment.ActionBarSettingsPreferenceFragment
import com.pyamsoft.pydroid.ui.app.fragment.AppSettingsFragment

class SettingsFragment : AppSettingsFragment() {

    override fun provideBoundPresenters(): List<Presenter<*>> = emptyList()

    override fun provideSettingsFragment(): ActionBarSettingsPreferenceFragment = SettingsPreferenceFragment()

    override fun provideSettingsTag(): String = SettingsPreferenceFragment.TAG

    companion object {
        const val TAG = "SettingsFragment"
    }
}

