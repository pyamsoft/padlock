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

import androidx.lifecycle.Lifecycle.Event.ON_STOP
import com.pyamsoft.padlock.api.LockListInteractor
import com.pyamsoft.padlock.api.service.LockServiceInteractor
import com.pyamsoft.padlock.api.service.LockServiceInteractor.ServiceState.DISABLED
import com.pyamsoft.padlock.api.service.LockServiceInteractor.ServiceState.ENABLED
import com.pyamsoft.padlock.api.service.LockServiceInteractor.ServiceState.PAUSED
import com.pyamsoft.padlock.api.service.LockServiceInteractor.ServiceState.PERMISSION
import com.pyamsoft.padlock.model.LockState
import com.pyamsoft.padlock.model.LockState.DEFAULT
import com.pyamsoft.padlock.model.LockState.LOCKED
import com.pyamsoft.padlock.model.LockWhitelistedEvent
import com.pyamsoft.padlock.model.db.PadLockDbModels
import com.pyamsoft.padlock.model.list.AppEntry
import com.pyamsoft.padlock.model.list.ListDiffProvider
import com.pyamsoft.padlock.model.pin.ClearPinEvent
import com.pyamsoft.padlock.model.pin.CreatePinEvent
import com.pyamsoft.pydroid.core.bus.EventBus
import com.pyamsoft.pydroid.core.bus.Listener
import com.pyamsoft.pydroid.core.cache.Cache
import com.pyamsoft.pydroid.core.presenter.Presenter
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.Consumer
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

@JvmSuppressWildcards
class LockListPresenter @Inject internal constructor(
  private val lockListInteractor: LockListInteractor,
  private val serviceInteractor: LockServiceInteractor,
  private val lockListBus: EventBus<LockListEvent>,
  private val lockWhitelistedBus: EventBus<LockWhitelistedEvent>,
  private val clearPinBus: Listener<ClearPinEvent>,
  private val createPinBus: Listener<CreatePinEvent>,
  private val listDiffProvider: ListDiffProvider<AppEntry>,
  @Named("cache_lock_list") private val cache: Cache
) : Presenter<LockListPresenter.View>() {

  override fun onCreate() {
    super.onCreate()
    registerOnCreateBus()
    registerOnClearBus()
    registerOnModifyBus()
    registerOnWhitelistedBus()
    watchFabState()
  }

  override fun onStart() {
    super.onStart()
    populateList(false)
    subscribeToDatabaseChanges()
    setSystemVisibilityFromPreference()
  }

  private fun subscribeToDatabaseChanges() {
    dispose(ON_STOP) {
      lockListInteractor.subscribeForUpdates(listDiffProvider)
          .subscribeOn(Schedulers.computation())
          .observeOn(AndroidSchedulers.mainThread())
          .subscribe({ view?.onDatabaseChangeReceived(it.index, it.entry) }, {
            Timber.e(it, "Error while subscribed to database changes")
            view?.onDatabaseChangeError(it)
          })
    }
  }

  private fun registerOnWhitelistedBus() {
    dispose {
      lockWhitelistedBus.listen()
          .subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread())
          .subscribe({ populateList(true) }, {
            Timber.e(it, "Error listening to lock whitelist bus")
          })
    }
  }

  private fun registerOnModifyBus() {
    dispose {
      lockListBus.listen()
          .subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread())
          .subscribe({
            modifyDatabaseEntry(it.isChecked, it.packageName, it.code, it.isSystem)
          }, {
            Timber.e(it, "Error listening to lock list bus")
          })
    }
  }

  private fun registerOnClearBus() {
    dispose {
      clearPinBus.listen()
          .subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread())
          .subscribe({
            if (it.success) {
              view?.onMasterPinClearSuccess()
            } else {
              view?.onMasterPinClearFailure()
            }
          }, {
            Timber.e(it, "error create pin bus")
          })
    }
  }

  private fun registerOnCreateBus() {
    dispose {
      createPinBus.listen()
          .subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread())
          .subscribe({
            if (it.success) {
              view?.onMasterPinCreateSuccess()
            } else {
              view?.onMasterPinCreateFailure()
            }
          }, {
            Timber.e(it, "error create pin bus")
          })
    }
  }

  private fun modifyDatabaseEntry(
    isChecked: Boolean,
    packageName: String,
    code: String?,
    system: Boolean
  ) {
    dispose {
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

      lockListInteractor.modifyEntry(
          oldState, newState, packageName,
          PadLockDbModels.PACKAGE_ACTIVITY_NAME, code, system
      )
          .subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread())
          .subscribe({ Timber.d("Modify complete $packageName") }, {
            Timber.e(it, "onError modifyEntry")
            view?.onModifyEntryError(it)
          })
    }
  }

  fun checkFabState(manually: Boolean) {
    dispose {
      serviceInteractor.isServiceEnabled()
          .subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread())
          .subscribe(Consumer {
            when (it) {
              ENABLED -> view?.onFabIconLocked(manually)
              DISABLED -> view?.onFabIconUnlocked(manually)
              PAUSED -> view?.onFabIconPaused(manually)
              PERMISSION -> view?.onFabIconPermissionDenied(manually)
              else -> error("Enum ServiceState is null")
            }
          })
    }
  }

  private fun watchFabState() {
    checkFabState(false)

    dispose {
      serviceInteractor.observeServiceState()
          .subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread())
          .subscribe {
            when (it) {
              ENABLED -> view?.onFabIconLocked(false)
              DISABLED -> view?.onFabIconUnlocked(false)
              PAUSED -> view?.onFabIconPaused(false)
              PERMISSION -> view?.onFabIconPermissionDenied(false)
              else -> error("Enum ServiceState is null")
            }
          }
    }
  }

  private fun setSystemVisibilityFromPreference() {
    dispose(ON_STOP) {
      lockListInteractor.isSystemVisible()
          .subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread())
          .subscribe({
            view?.onSystemVisibilityChanged(it)
          }, { Timber.e(it, "onError") })
    }
  }

  fun setSystemVisibility(visible: Boolean) {
    lockListInteractor.setSystemVisible(visible)
    view?.onSystemVisibilityChanged(visible)
  }

  fun populateList(force: Boolean) {
    dispose(ON_STOP) {
      lockListInteractor.fetchAppEntryList(force)
          .subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread())
          .doAfterTerminate { view?.onListPopulated() }
          .doOnSubscribe { view?.onListPopulateBegin() }
          .subscribe({ view?.onListLoaded(it) }, {
            Timber.e(it, "populateList onError")
            view?.onListPopulateError(it)
          })
    }
  }

  /**
   * Used when the activity is launched from Notification
   */
  fun forceClearCache() {
    cache.clearCache()
  }

  interface View : LockModifyCallback, MasterPinCreateCallback, MasterPinClearCallback,
      FabIconStateCallback, SystemVisibilityChangeCallback,
      ListPopulateCallback, ChangeCallback

  interface ChangeCallback {

    fun onDatabaseChangeReceived(
      index: Int,
      entry: AppEntry
    )

    fun onDatabaseChangeError(throwable: Throwable)
  }

  interface LockModifyCallback {

    fun onModifyEntryError(throwable: Throwable)
  }

  interface MasterPinCreateCallback {

    fun onMasterPinCreateSuccess()

    fun onMasterPinCreateFailure()
  }

  interface MasterPinClearCallback {

    fun onMasterPinClearSuccess()

    fun onMasterPinClearFailure()
  }

  interface FabIconStateCallback {

    fun onFabIconLocked(manually: Boolean)

    fun onFabIconUnlocked(manually: Boolean)

    fun onFabIconPermissionDenied(manually: Boolean)

    fun onFabIconPaused(manually: Boolean)
  }

  interface SystemVisibilityChangeCallback {

    fun onSystemVisibilityChanged(visible: Boolean)
  }

  interface ListPopulateCallback {

    fun onListPopulateBegin()

    fun onListLoaded(entries: List<AppEntry>)

    fun onListPopulated()

    fun onListPopulateError(throwable: Throwable)
  }
}
