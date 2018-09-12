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

package com.pyamsoft.padlock.lock.screen

import androidx.lifecycle.LifecycleOwner
import com.pyamsoft.padlock.api.lockscreen.LockScreenInteractor
import com.pyamsoft.padlock.model.LockScreenType
import com.pyamsoft.padlock.model.LockScreenType.TYPE_PATTERN
import com.pyamsoft.padlock.model.LockScreenType.TYPE_TEXT
import com.pyamsoft.pydroid.core.bus.RxBus
import com.pyamsoft.pydroid.core.viewmodel.BaseViewModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject

class PinScreenInputViewModel @Inject internal constructor(
  owner: LifecycleOwner,
  private val interactor: LockScreenInteractor
) : BaseViewModel(owner) {

  private val lockScreenTypeBus = RxBus.create<LockScreenType>()
  private var lockScreenTypeDisposable by disposable()

  override fun onCleared() {
    super.onCleared()
    lockScreenTypeDisposable.tryDispose()
  }

  fun onLockScreenTypePattern(func: () -> Unit) {
    dispose {
      lockScreenTypeBus.listen()
          .filter { it == TYPE_PATTERN }
          .subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread())
          .subscribe { func() }
    }
  }

  fun onLockScreenTypeText(func: () -> Unit) {
    dispose {
      lockScreenTypeBus.listen()
          .filter { it == TYPE_TEXT }
          .subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread())
          .subscribe { func() }
    }
  }

  fun resolveLockScreenType() {
    lockScreenTypeDisposable = interactor.getLockScreenType()
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe({ lockScreenTypeBus.publish(it) }, {
          Timber.e(it, "Error resolving lock screen type")
        })
  }

}
