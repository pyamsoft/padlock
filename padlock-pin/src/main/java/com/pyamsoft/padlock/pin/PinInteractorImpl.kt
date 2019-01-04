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

import androidx.annotation.CheckResult
import com.pyamsoft.padlock.api.MasterPinInteractor
import com.pyamsoft.padlock.api.PinInteractor
import com.pyamsoft.padlock.api.lockscreen.LockHelper
import com.pyamsoft.padlock.model.pin.PinEntryEvent
import com.pyamsoft.pydroid.core.optional.Optional
import com.pyamsoft.pydroid.core.optional.Optional.Present
import com.pyamsoft.pydroid.core.threads.Enforcer
import io.reactivex.Single
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

internal class PinInteractorImpl @Inject internal constructor(
  private val enforcer: Enforcer,
  private val lockHelper: LockHelper,
  private val masterPinInteractor: MasterPinInteractor
) : PinInteractor {

  @CheckResult
  private fun getMasterPin(): Single<Optional<String>> = Single.defer {
    enforcer.assertNotOnMainThread()
    return@defer masterPinInteractor.getMasterPin()
  }

  override fun hasMasterPin(): Single<Boolean> = getMasterPin().map {
    enforcer.assertNotOnMainThread()
    return@map it is Present
  }

  override fun submitPin(
    currentAttempt: String,
    reEntryAttempt: String,
    hint: String
  ): Single<PinEntryEvent> {
    return getMasterPin().flatMap {
      enforcer.assertNotOnMainThread()
      return@flatMap when (it) {
        is Present -> clearPin(it.value, currentAttempt)
        else -> createPin(currentAttempt, reEntryAttempt, hint)
      }
    }
  }

  override fun comparePin(attempt: String): Single<Boolean> {
    return getMasterPin().flatMap {
      if (it is Present) {
        return@flatMap lockHelper.checkSubmissionAttempt(attempt, it.value)
      } else {
        return@flatMap Single.just(false)
      }
    }
  }

  @CheckResult
  private fun clearPin(
    masterPin: String,
    attempt: String
  ): Single<PinEntryEvent> {
    return lockHelper.checkSubmissionAttempt(attempt, masterPin)
        .map {
          enforcer.assertNotOnMainThread()
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

  @CheckResult
  private fun createPin(
    attempt: String,
    reentry: String,
    hint: String
  ): Single<PinEntryEvent> {
    return Single.defer {
      enforcer.assertNotOnMainThread()
      Timber.d("No existing master item, attempt to create a new one")
      return@defer when (attempt) {
        reentry -> lockHelper.encode(attempt)
        else -> Single.just("")
      }
    }
        .map {
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
