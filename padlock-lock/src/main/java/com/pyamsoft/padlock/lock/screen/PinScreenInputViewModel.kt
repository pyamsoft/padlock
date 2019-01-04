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

package com.pyamsoft.padlock.lock.screen

import androidx.annotation.CheckResult
import com.pyamsoft.padlock.api.lockscreen.LockScreenInteractor
import com.pyamsoft.padlock.model.LockScreenType.TYPE_PATTERN
import com.pyamsoft.padlock.model.LockScreenType.TYPE_TEXT
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Consumer
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

class PinScreenInputViewModel @Inject internal constructor(
  private val interactor: LockScreenInteractor
) {

  @CheckResult
  fun resolveLockScreenType(
    onTypeText: () -> Unit,
    onTypePattern: () -> Unit
  ): Disposable {
    return interactor.getLockScreenType()
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(Consumer {
          when (it) {
            TYPE_TEXT -> onTypeText()
            TYPE_PATTERN -> onTypePattern()
            else -> throw IllegalArgumentException("Invalid lock screen type: $it")
          }
        })
  }

}
