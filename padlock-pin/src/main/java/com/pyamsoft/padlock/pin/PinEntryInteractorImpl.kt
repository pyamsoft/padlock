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

package com.pyamsoft.padlock.pin

import android.support.annotation.CheckResult
import com.pyamsoft.padlock.api.LockHelper
import com.pyamsoft.padlock.api.MasterPinInteractor
import com.pyamsoft.padlock.api.PinEntryInteractor
import com.pyamsoft.padlock.model.PinEntryEvent
import com.pyamsoft.pydroid.data.Optional
import com.pyamsoft.pydroid.data.Optional.Present
import io.reactivex.Single
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton internal class PinEntryInteractorImpl @Inject internal constructor(
        private val lockHelper: LockHelper,
        private val masterPinInteractor: MasterPinInteractor) :
        PinEntryInteractor {

    @CheckResult private fun getMasterPin(): Single<Optional<String>> =
            masterPinInteractor.getMasterPin()

    override fun hasMasterPin(): Single<Boolean> = getMasterPin().map { it is Present }

    override fun submitPin(currentAttempt: String, reEntryAttempt: String,
            hint: String): Single<PinEntryEvent> {
        return getMasterPin().flatMap {
            if (it is Present) {
                return@flatMap clearPin(it.value, currentAttempt)
            } else {
                return@flatMap createPin(currentAttempt, reEntryAttempt, hint)
            }
        }
    }

    @CheckResult private fun clearPin(
            masterPin: String, attempt: String): Single<PinEntryEvent> {
        return lockHelper.checkSubmissionAttempt(attempt, masterPin).map {
            if (it) {
                Timber.d("Clear master item")
                masterPinInteractor.setMasterPin(null)
                masterPinInteractor.setHint(null)
            } else {
                Timber.d("Failed to clear master item")
            }

            return@map PinEntryEvent.Clear(it)
        }
    }

    @CheckResult private fun createPin(
            attempt: String, reentry: String, hint: String): Single<PinEntryEvent> {
        return Single.defer {
            Timber.d("No existing master item, attempt to create a new one")
            if (attempt == reentry) {
                return@defer lockHelper.encode(attempt)
            } else {
                return@defer Single.just("")
            }
        }.map {
            val success = it.isNotBlank()
            if (success) {
                Timber.d("Entry and Re-Entry match, create")
                masterPinInteractor.setMasterPin(it)

                if (hint.isNotEmpty()) {
                    Timber.d("User provided hint, save it")
                    masterPinInteractor.setHint(hint)
                }
            } else {
                Timber.e("Entry and re-entry do not match")
            }
            return@map PinEntryEvent.Create(success)
        }
    }
}
