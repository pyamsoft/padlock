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

package com.pyamsoft.padlock.list

import androidx.annotation.CheckResult
import androidx.lifecycle.LifecycleOwner
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
import com.pyamsoft.padlock.model.pin.ClearPinEvent
import com.pyamsoft.padlock.model.pin.CreatePinEvent
import com.pyamsoft.pydroid.core.bus.Listener
import com.pyamsoft.pydroid.core.bus.RxBus
import com.pyamsoft.pydroid.core.threads.Enforcer
import com.pyamsoft.pydroid.core.viewmodel.BaseViewModel
import com.pyamsoft.pydroid.core.viewmodel.DataBus
import com.pyamsoft.pydroid.core.viewmodel.DataWrapper
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.Consumer
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject

@JvmSuppressWildcards
class LockListViewModel @Inject internal constructor(
  owner: LifecycleOwner,
  private val enforcer: Enforcer,
  private val lockListInteractor: LockListInteractor,
  private val serviceInteractor: LockServiceInteractor,
  private val lockListBus: Listener<LockListEvent>,
  private val lockWhitelistedBus: Listener<LockWhitelistedEvent>,
  private val clearPinBus: Listener<ClearPinEvent>,
  private val createPinBus: Listener<CreatePinEvent>,
  private val listDiffProvider: ListDiffProvider<AppEntry>
) : BaseViewModel(owner) {

  private val populateListBus = DataBus<List<AppEntry>>()
  private val databaseChangeBus = DataBus<LockListUpdatePayload>()
  private val fabStateBus = RxBus.create<Pair<ServiceState, Boolean>>()

  private var fabDisposable by disposable()
  private var populateListDisposable by disposable()

  override fun onCleared() {
    super.onCleared()
    fabDisposable.tryDispose()
    populateListDisposable.tryDispose()
  }

  fun onDatabaseChangeEvent(func: (DataWrapper<LockListUpdatePayload>) -> Unit) {
    dispose {
      lockListInteractor.subscribeForUpdates(listDiffProvider)
          .subscribeOn(Schedulers.computation())
          .observeOn(AndroidSchedulers.mainThread())
          .doOnSubscribe { databaseChangeBus.publishLoading(false) }
          .doAfterTerminate { databaseChangeBus.publishComplete() }
          .subscribe({ databaseChangeBus.publishSuccess(it) }, {
            Timber.e(it, "Error while subscribed to database changes")
            databaseChangeBus.publishError(it)
          })

    }

    dispose {
      databaseChangeBus.listen()
          .subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread())
          .subscribe(func)
    }
  }

  fun onPopulateListEvent(func: (DataWrapper<List<AppEntry>>) -> Unit) {
    dispose {
      populateListBus.listen()
          .subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread())
          .subscribe(func)
    }

    dispose {
      lockWhitelistedBus.listen()
          .subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread())
          .subscribe { populateList(true) }
    }
  }

  fun onModifyError(func: (Throwable) -> Unit) {
    dispose {
      lockListBus.listen()
          .observeOn(Schedulers.io())
          .flatMapSingle { modifyDatabaseEntry(it.isChecked, it.packageName, it.code, it.isSystem) }
          .subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread())
          .doOnError {
            Timber.e(it, "Error occurred modifying database entry")
            func(it)
          }
          .onErrorReturnItem(Unit)
          .subscribe()
    }
  }

  fun onClearPinEvent(func: (ClearPinEvent) -> Unit) {
    dispose {
      clearPinBus.listen()
          .subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread())
          .subscribe(func)
    }
  }

  fun onCreatePinEvent(func: (CreatePinEvent) -> Unit) {
    dispose {
      createPinBus.listen()
          .subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread())
          .subscribe(func)
    }
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
    }
  }

  fun onFabStateChange(func: (ServiceState, Boolean) -> Unit) {
    dispose {
      serviceInteractor.observeServiceState()
          .subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread())
          .subscribe { func(it, false) }
    }

    dispose {
      fabStateBus.listen()
          .subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread())
          .subscribe { func(it.first, it.second) }
    }
  }

  fun checkFabState(fromClick: Boolean) {
    fabDisposable = serviceInteractor.isServiceEnabled()
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(Consumer { fabStateBus.publish(it to fromClick) })
  }

  fun onSystemVisibilityChanged(func: (Boolean) -> Unit) {
    dispose {
      lockListInteractor.watchSystemVisible()
          .subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread())
          .subscribe(func)
    }
  }

  fun setSystemVisibility(visible: Boolean) {
    lockListInteractor.setSystemVisible(visible)
  }

  fun populateList(force: Boolean) {
    populateListDisposable = lockListInteractor.fetchAppEntryList(force)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .doAfterTerminate { populateListBus.publishComplete() }
        .doOnSubscribe { populateListBus.publishLoading(force) }
        .subscribe({ populateListBus.publishSuccess(it) }, {
          Timber.e(it, "LockListPresenter populateList error")
          populateListBus.publishError(it)
        })
  }

}
