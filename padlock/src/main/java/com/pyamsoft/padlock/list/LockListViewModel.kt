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

package com.pyamsoft.padlock.list

import androidx.annotation.CheckResult
import com.pyamsoft.padlock.api.LockListInteractor
import com.pyamsoft.padlock.api.service.LockServiceInteractor
import com.pyamsoft.padlock.api.service.LockServiceInteractor.ServiceState
import com.pyamsoft.padlock.model.LockState
import com.pyamsoft.padlock.model.LockState.DEFAULT
import com.pyamsoft.padlock.model.LockState.LOCKED
import com.pyamsoft.padlock.model.LockWhitelistedEvent
import com.pyamsoft.padlock.model.db.PadLockDbModels
import com.pyamsoft.padlock.model.list.AppEntry
import com.pyamsoft.padlock.model.list.ListDiffProvider
import com.pyamsoft.padlock.model.list.LockListUpdatePayload
import com.pyamsoft.padlock.model.pin.CreatePinEvent
import com.pyamsoft.padlock.pin.ClearPinPresenterImpl.ClearPinEvent
import com.pyamsoft.pydroid.core.bus.EventBus
import com.pyamsoft.pydroid.core.bus.Listener
import com.pyamsoft.pydroid.core.threads.Enforcer
import com.pyamsoft.pydroid.core.tryDispose
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.disposables.Disposables
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject

class LockListViewModel @Inject internal constructor(
  private val enforcer: Enforcer,
  private val lockListInteractor: LockListInteractor,
  private val serviceInteractor: LockServiceInteractor,
  private val lockListBus: EventBus<LockListEvent>,
  private val lockWhitelistedBus: Listener<LockWhitelistedEvent>,
  private val createPinBus: Listener<CreatePinEvent>,
  private val listDiffProvider: ListDiffProvider<AppEntry>
) {

  @CheckResult
  fun onDatabaseChangeEvent(
    onChange: (payload: LockListUpdatePayload) -> Unit,
    onError: (error: Throwable) -> Unit
  ): Disposable {
    return lockListInteractor.subscribeForUpdates(listDiffProvider)
        .unsubscribeOn(Schedulers.io())
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe({ onChange(it) }, {
          Timber.e(it, "Error while subscribed to database changes")
          onError(it)
        })
  }

  @CheckResult
  fun onLockEvent(
    onWhitelist: (event: LockWhitelistedEvent) -> Unit,
    onError: (error: Throwable) -> Unit
  ): Disposable {
    val whitelistDisposable = lockWhitelistedBus.listen()
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe { onWhitelist(it) }

    val errorDisposable = lockListBus.listen()
        .flatMapSingle { modifyDatabaseEntry(it.isChecked, it.packageName, it.code, it.isSystem) }
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .doOnError {
          Timber.e(it, "Error occurred modifying database entry")
          onError(it)
        }
        .onErrorReturnItem(Unit)
        .subscribe()

    return object : Disposable {

      override fun isDisposed(): Boolean {
        return whitelistDisposable.isDisposed && errorDisposable.isDisposed
      }

      override fun dispose() {
        whitelistDisposable.tryDispose()
        errorDisposable.tryDispose()
      }

    }
  }

  @CheckResult
  internal fun onClearPinEvent(onClear: (event: ClearPinEvent) -> Unit): Disposable {
    return Disposables.empty()
//    return clearPinBus.listen()
//        .subscribeOn(Schedulers.io())
//        .observeOn(AndroidSchedulers.mainThread())
//        .subscribe(onClear)
  }

  @CheckResult
  fun onCreatePinEvent(onCreate: (event: CreatePinEvent) -> Unit): Disposable {
    return createPinBus.listen()
        .subscribe(onCreate)
  }

  @CheckResult
  private fun modifyDatabaseEntry(
    isChecked: Boolean,
    packageName: String,
    code: String?,
    system: Boolean
  ): Single<Unit> {
    return Single.defer {
      enforcer.assertNotOnMainThread()
      // No whitelisting for modifications from the List
      val oldState: LockState
      val newState: LockState
      if (isChecked) {
        oldState = DEFAULT
        newState = LOCKED
      } else {
        oldState = LOCKED
        newState = DEFAULT
      }

      return@defer lockListInteractor.modifyEntry(
          oldState, newState, packageName,
          PadLockDbModels.PACKAGE_ACTIVITY_NAME, code, system
      )
          .andThen(Single.just(Unit))
          .subscribeOn(Schedulers.io())
          .observeOn(Schedulers.io())
    }
        .subscribeOn(Schedulers.io())
        .observeOn(Schedulers.io())
  }

  @CheckResult
  fun onFabStateChange(onChange: (state: ServiceState) -> Unit): Disposable {
    return serviceInteractor.observeServiceState()
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(onChange)
  }

  @CheckResult
  fun checkFabState(onChecked: (state: ServiceState) -> Unit): Disposable {
    return serviceInteractor.isServiceEnabled()
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(onChecked)
  }

  @CheckResult
  fun onSystemVisibilityChanged(onChange: (visible: Boolean) -> Unit): Disposable {
    return lockListInteractor.watchSystemVisible()
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(onChange)
  }

  fun setSystemVisibility(visible: Boolean) {
    lockListInteractor.setSystemVisible(visible)
  }

  @CheckResult
  fun populateList(
    force: Boolean,
    onPopulateBegin: (forced: Boolean) -> Unit,
    onPopulateSuccess: (appList: List<AppEntry>) -> Unit,
    onPopulateError: (error: Throwable) -> Unit,
    onPopulateComplete: () -> Unit
  ): Disposable {
    return lockListInteractor.fetchAppEntryList(force)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .doAfterTerminate { onPopulateComplete() }
        .doOnSubscribe { onPopulateBegin(force) }
        .subscribe({ onPopulateSuccess(it) }, {
          Timber.e(it, "LockListPresenter populateList error")
          onPopulateError(it)
        })
  }

}
