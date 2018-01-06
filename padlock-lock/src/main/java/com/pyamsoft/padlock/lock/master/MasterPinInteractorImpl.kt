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

package com.pyamsoft.padlock.lock.master

import com.pyamsoft.padlock.api.MasterPinInteractor
import com.pyamsoft.padlock.api.MasterPinPreferences
import com.pyamsoft.pydroid.data.Optional
import com.pyamsoft.pydroid.helper.asOptional
import io.reactivex.Single
import javax.inject.Inject
import javax.inject.Singleton

@Singleton internal class MasterPinInteractorImpl @Inject internal constructor(
        private val preferences: MasterPinPreferences) :
        MasterPinInteractor {

    override fun getMasterPin(): Single<Optional<String>> =
            Single.fromCallable { preferences.getMasterPassword().asOptional() }

    override fun setMasterPin(pin: String?) {
        if (pin == null) {
            preferences.clearMasterPassword()
        } else {
            preferences.setMasterPassword(pin)
        }
    }

    override fun getHint(): Single<Optional<String>> =
            Single.fromCallable { preferences.getHint().asOptional() }

    override fun setHint(hint: String?) {
        if (hint == null) {
            preferences.clearHint()
        } else {
            preferences.setHint(hint)
        }
    }
}
