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

import com.pyamsoft.padlock.pin.PinCreateDialogPresenter.Callback
import com.pyamsoft.padlock.scopes.FragmentScope
import com.pyamsoft.pydroid.core.bus.RxBus
import com.pyamsoft.pydroid.arch.BasePresenter
import javax.inject.Inject

@FragmentScope
internal class PinCreateDialogPresenterImpl @Inject internal constructor(
) : BasePresenter<Unit, Callback>(RxBus.empty()),
    CreatePinView.Callback,
    PinCreateDialogPresenter {

  override fun onBind() {
  }

  override fun onSubmit(
    attempt: String,
    reConfirmAttempt: String,
    optionalHint: String
  ) {
    callback.onAttemptSubmit(attempt, reConfirmAttempt, optionalHint)
  }

  override fun onUnbind() {
  }
}