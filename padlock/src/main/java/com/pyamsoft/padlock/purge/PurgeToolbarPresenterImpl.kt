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

package com.pyamsoft.padlock.purge

import com.pyamsoft.padlock.scopes.FragmentScope
import com.pyamsoft.pydroid.arch.BasePresenter
import com.pyamsoft.pydroid.core.bus.RxBus
import javax.inject.Inject

@FragmentScope
internal class PurgeToolbarPresenterImpl @Inject internal constructor(
) : BasePresenter<Unit, PurgeToolbarPresenter.Callback>(RxBus.empty()),
    PurgeToolbarView.Callback,
    PurgeToolbarPresenter {

  override fun onBind() {
  }

  override fun onUnbind() {
  }

  override fun onPurgeAllClicked() {
    callback.onDeleteAllRequest()
  }

}