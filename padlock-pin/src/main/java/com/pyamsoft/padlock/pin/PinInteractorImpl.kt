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
import com.pyamsoft.pydroid.core.optional.Optional
import com.pyamsoft.pydroid.core.optional.Optional.Present
import com.pyamsoft.pydroid.core.threads.Enforcer
import io.reactivex.Single
import timber.log.Timber
import javax.inject.Inject

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

  override fun comparePin(attempt: String): Single<Boolean> {
    return getMasterPin().flatMap {
      if (it is Present) {
        return@flatMap lockHelper.checkSubmissionAttempt(attempt, it.value)
      } else {
        return@flatMap Single.just(false)
      }
    }
  }

  override fun createPin(
    currentAttempt: String,
    reEntryAttempt: String,
    hint: String
  ): Single<Boolean> {
    return Single.defer {
      enforcer.assertNotOnMainThread()
      return@defer hasMasterPin()
          .flatMap { hasPin ->
            enforcer.assertNotOnMainThread()
            if (hasPin) {
              Timber.e("Cannot create PIN, we already have one. Clear it first")
              return@flatMap Single.just(false)
            } else {
              if (currentAttempt != reEntryAttempt) {
                Timber.w("Pin and Re-Entry do not match, try again")
                return@flatMap Single.just(false)
              }

              return@flatMap lockHelper.encode(currentAttempt)
                  .doOnSuccess { encoded ->
                    enforcer.assertNotOnMainThread()
                    Timber.d("Set master password.")
                    masterPinInteractor.setMasterPin(encoded)

                    if (hint.isNotBlank()) {
                      Timber.d("Set optional hint")
                      masterPinInteractor.setHint(hint)
                    }

                  }
                  .map { true }
            }
          }
    }
  }

  override fun clearPin(attempt: String): Single<Boolean> {
    return Single.defer {
      enforcer.assertNotOnMainThread()
      return@defer getMasterPin()
          .flatMap { masterPinOptional ->
            enforcer.assertNotOnMainThread()
            if (masterPinOptional is Present) {
              val masterPin = masterPinOptional.value
              return@flatMap lockHelper.checkSubmissionAttempt(attempt, masterPin)
                  .doOnSuccess { canClear ->
                    enforcer.assertNotOnMainThread()
                    if (canClear) {
                      masterPinInteractor.setMasterPin(null)
                      masterPinInteractor.setHint(null)
                    } else {
                      Timber.w("Cannot clear PIN, inputs do not match")
                    }
                  }
            } else {
              Timber.e("Cannot clear PIN, we do not have one. Create it first")
              return@flatMap Single.just(false)
            }
          }
    }
  }
}
