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

import com.pyamsoft.padlock.lock.LockScreenPresenter.Callback
import com.pyamsoft.pydroid.arch.Presenter

internal interface LockScreenPresenter : Presenter<Callback> {

  fun checkUnlocked()

  fun submit(
    lockCode: String?,
    currentAttempt: String,
    isSystem: Boolean,
    shouldExclude: Boolean,
    ignoreTime: Long
  )

  fun displayHint()

  fun loadDefaultIgnoreTime()

  interface Callback {

    fun onCloseOld()

    fun onDisplayNameLoaded(name: String)

    fun onDefaultIgnoreTimeLoaded(time: Long)

    fun onAlreadyUnlocked()

    fun onSubmitUnlockAttempt(attempt: String)

    fun showLockedStats()

    fun onShowLockHint(hint: String)

    fun onSubmitBegin()

    fun onSubmitUnlocked()

    fun onSubmitFailed()

    fun onSubmitLocked()

    fun onSubmitError(throwable: Throwable)

  }

}

