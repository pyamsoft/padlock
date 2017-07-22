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

package com.pyamsoft.padlock.pin

import android.support.annotation.CheckResult
import com.pyamsoft.padlock.base.preference.LockScreenPreferences
import com.pyamsoft.padlock.lock.LockHelper
import com.pyamsoft.padlock.lock.common.LockTypeInteractor
import com.pyamsoft.padlock.lock.master.MasterPinInteractor
import com.pyamsoft.pydroid.helper.Optional
import io.reactivex.Single
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton internal class PinEntryInteractor @Inject constructor(
    private val masterPinInteractor: MasterPinInteractor,
    preferences: LockScreenPreferences) : LockTypeInteractor(preferences) {

  private val masterPin: Single<Optional<String>>
    @CheckResult get() = masterPinInteractor.masterPin

  @CheckResult fun hasMasterPin(): Single<Boolean> {
    return masterPin.map { it.isPresent() }
  }

  @CheckResult fun submitPin(currentAttempt: String,
      reEntryAttempt: String, hint: String): Single<PinEntryEvent> {
    return masterPin.flatMap {
      if (it.isPresent()) {
        return@flatMap clearPin(it.item(), currentAttempt)
      } else {
        return@flatMap createPin(currentAttempt, reEntryAttempt, hint)
      }
    }
  }

  @CheckResult private fun clearPin(
      masterPin: String, attempt: String): Single<PinEntryEvent> {
    return LockHelper.get().checkSubmissionAttempt(attempt, masterPin).map {
      if (it) {
        Timber.d("Clear master item")
        masterPinInteractor.setMasterPin(null)
        masterPinInteractor.setHint(null)
      } else {
        Timber.d("Failed to clear master item")
      }

      return@map PinEntryEvent.create(PinEntryEvent.Type.TYPE_CLEAR, it)
    }
  }

  @CheckResult private fun createPin(
      attempt: String, reentry: String, hint: String): Single<PinEntryEvent> {
    return Single.fromCallable {
      Timber.d("No existing master item, attempt to create a new one")

      val success = (attempt == reentry)
      if (success) {
        Timber.d("Entry and Re-Entry match, create")

        // Blocking, should be run off main thread
        val encodedMasterPin = LockHelper.get().encodeSHA256(attempt).blockingGet()
        masterPinInteractor.setMasterPin(encodedMasterPin)

        if (hint.isNotEmpty()) {
          Timber.d("User provided hint, save it")
          masterPinInteractor.setHint(hint)
        }
      } else {
        Timber.e("Entry and re-entry do not match")
      }

      return@fromCallable PinEntryEvent.create(PinEntryEvent.Type.TYPE_CREATE, success)
    }
  }
}
