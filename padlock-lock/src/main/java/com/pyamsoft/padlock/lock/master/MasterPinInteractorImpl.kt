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

package com.pyamsoft.padlock.lock.master

import com.pyamsoft.padlock.base.preference.MasterPinPreferences
import com.pyamsoft.pydroid.helper.Optional
import io.reactivex.Single
import javax.inject.Inject
import javax.inject.Singleton

@Singleton internal class MasterPinInteractorImpl @Inject internal constructor(
    private val preferences: MasterPinPreferences) : MasterPinInteractor {

  override fun getMasterPin(): Single<Optional<String>> {
    return Single.fromCallable { Optional.ofNullable(preferences.getMasterPassword()) }
  }

  override fun setMasterPin(pin: String?) {
    if (pin == null) {
      preferences.clearMasterPassword()
    } else {
      preferences.setMasterPassword(pin)
    }
  }

  override fun getHint(): Single<Optional<String>> {
    return Single.fromCallable { Optional.ofNullable(preferences.getHint()) }
  }

  override fun setHint(hint: String?) {
    if (hint == null) {
      preferences.clearHint()
    } else {
      preferences.setHint(hint)
    }
  }
}
