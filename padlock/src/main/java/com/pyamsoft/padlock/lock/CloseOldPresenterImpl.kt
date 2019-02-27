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

package com.pyamsoft.padlock.lock

import com.pyamsoft.padlock.lock.CloseOldPresenter.Callback
import com.pyamsoft.padlock.lock.CloseOldPresenterImpl.CloseOldEvent
import com.pyamsoft.padlock.scopes.FragmentScope
import com.pyamsoft.pydroid.arch.BasePresenter
import com.pyamsoft.pydroid.arch.destroy
import com.pyamsoft.pydroid.core.bus.EventBus
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

@FragmentScope
internal class CloseOldPresenterImpl @Inject internal constructor(
  bus: EventBus<CloseOldEvent>,
  @Named("locked_package_name") private val packageName: String,
  @Named("locked_activity_name") private val activityName: String
) : BasePresenter<CloseOldEvent, Callback>(bus), CloseOldPresenter {

  override fun onBind() {
    Timber.d("Publish: $packageName $activityName before we listen so we don't close ourselves")

    // If any old listener is present, they would already be subscribed and receive the event
    publish(CloseOldEvent(packageName, activityName))

    listen()
        .filter { it.packageName == packageName }
        .filter { it.activityName == activityName }
        .subscribe { callback.onCloseOld() }
        .destroy(owner)
  }

  override fun onUnbind() {
  }

  internal data class CloseOldEvent(
    val packageName: String,
    val activityName: String
  )

}