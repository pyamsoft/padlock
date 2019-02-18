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

package com.pyamsoft.padlock.service

import androidx.annotation.CheckResult
import com.pyamsoft.padlock.api.service.LockServiceInteractor
import com.pyamsoft.padlock.model.db.PadLockDbModels
import com.pyamsoft.padlock.model.db.PadLockEntryModel
import com.pyamsoft.padlock.service.LockServicePresenter.Callback
import com.pyamsoft.pydroid.core.bus.RxBus
import com.pyamsoft.pydroid.core.singleDisposable
import com.pyamsoft.pydroid.core.threads.Enforcer
import com.pyamsoft.pydroid.core.tryDispose
import com.pyamsoft.pydroid.ui.arch.BasePresenter
import com.pyamsoft.pydroid.ui.arch.destroy
import io.reactivex.Maybe
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject

internal class LockServicePresenterImpl @Inject internal constructor(
  private val enforcer: Enforcer,
  private val interactor: LockServiceInteractor
) : BasePresenter<Unit, Callback>(
    RxBus.empty()
), LockServicePresenter {

  private var recheckDisposable by singleDisposable()

  init {
    interactor.init()
  }

  override fun onBind() {
    listenForForegroundEvents()
  }

  override fun onUnbind() {
    recheckDisposable.tryDispose()
  }

  override fun clearForeground(
    packageName: String,
    className: String
  ) {
    interactor.clearMatchingForegroundEvent(packageName, className)
  }

  override fun processIfForeground(
    packageName: String,
    className: String
  ) {
    recheckDisposable = interactor.ifActiveMatching(packageName, className)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .flatMap { processEvent(packageName, className, forced = true) }
        .subscribe({ callback.onListenForegroundLock(it.model, it.className, it.icon) }, {
          Timber.e(it, "Error while attempting foreground recheck")
          callback.onListenForegroundError(it)
        })
  }

  private fun listenForForegroundEvents() {
    interactor.listenForForegroundEvents()
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .doOnCancel { Timber.d("Cancelling foreground listener") }
        .doAfterTerminate { callback.onListenForegroundFinished() }
        .flatMapMaybe { processEvent(it.packageName, it.className, forced = false) }
        .subscribe({ callback.onListenForegroundLock(it.model, it.className, it.icon) }, {
          Timber.e(it, "Error while watching foreground events")
          callback.onListenForegroundError(it)
        })
        .destroy(owner)
  }

  @CheckResult
  private fun processEvent(
    packageName: String,
    className: String,
    forced: Boolean
  ): Maybe<ProcessedEventWithClassNamePayload> {
    enforcer.assertNotOnMainThread()
    return interactor.processEvent(forced, packageName, className)
        .unsubscribeOn(Schedulers.io())
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .filter { (model, _) -> !PadLockDbModels.isEmpty(model) }
        .filter { (_, icon) -> icon != 0 }
        .map { ProcessedEventWithClassNamePayload(it.model, className, it.icon) }
  }

  private data class ProcessedEventWithClassNamePayload(
    val model: PadLockEntryModel,
    val className: String,
    val icon: Int
  )

}