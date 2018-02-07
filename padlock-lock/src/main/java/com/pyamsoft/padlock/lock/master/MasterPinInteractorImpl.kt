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

package com.pyamsoft.padlock.lock.master

import com.pyamsoft.padlock.api.MasterPinInteractor
import com.pyamsoft.padlock.api.MasterPinPreferences
import com.pyamsoft.pydroid.optional.Optional
import com.pyamsoft.pydroid.optional.asOptional
import io.reactivex.Single
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class MasterPinInteractorImpl @Inject internal constructor(
    private val preferences: MasterPinPreferences
) : MasterPinInteractor {

  override fun getMasterPin(): Single<Optional<String>> =
      Single.fromCallable {
        preferences.getMasterPassword()
            .asOptional()
      }

  override fun setMasterPin(pin: String?) {
    when (pin) {
      null -> preferences.clearMasterPassword()
      else -> preferences.setMasterPassword(pin)
    }
  }

  override fun getHint(): Single<Optional<String>> =
      Single.fromCallable {
        preferences.getHint()
            .asOptional()
      }

  override fun setHint(hint: String?) {
    when (hint) {
      null -> preferences.clearHint()
      else -> preferences.setHint(hint)
    }
  }
}
