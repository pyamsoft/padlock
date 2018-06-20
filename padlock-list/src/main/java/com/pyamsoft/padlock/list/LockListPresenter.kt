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
import com.pyamsoft.padlock.api.LockServiceStateInteractor
import com.pyamsoft.padlock.model.AppEntry
import com.pyamsoft.padlock.model.ClearPinEvent
import com.pyamsoft.padlock.model.CreatePinEvent
import com.pyamsoft.padlock.model.LockState
import com.pyamsoft.padlock.model.LockState.DEFAULT
import com.pyamsoft.padlock.model.LockState.LOCKED
import com.pyamsoft.padlock.model.LockWhitelistedEvent
import com.pyamsoft.padlock.model.db.PadLockDbModels
import com.pyamsoft.pydroid.core.bus.EventBus
import com.pyamsoft.pydroid.core.cache.Cache
import com.pyamsoft.pydroid.core.presenter.Presenter
import com.pyamsoft.pydroid.list.ListDiffProvider
import com.pyamsoft.pydroid.list.ListDiffResult
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

@JvmSuppressWildcards
class LockListPresenter @Inject internal constructor(
  private val lockListInteractor: LockListInteractor,
  @Named("cache_lock_list") private val cache: Cache,
  private val stateInteractor: LockServiceStateInteractor,
  private val lockListBus: EventBus<LockListEvent>,
  private val lockWhitelistedBus: EventBus<LockWhitelistedEvent>,
  private val clearPinBus: EventBus<ClearPinEvent>,
  private val createPinBus: EventBus<CreatePinEvent>,
  private val listDiffProvider: ListDiffProvider<AppEntry>
) : Presenter<LockListPresenter.View>() {

  override fun onCreate() {
    super.onCreate()
    registerOnCreateBus()
    registerOnClearBus()
    registerOnModifyBus()
    registerOnWhitelistedBus()
  }

  override fun onStart() {
    super.onStart()
    populateList(false)
    setFABStateFromPreference()
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

  private fun setFABStateFromPreference() {
    dispose(ON_STOP) {
      stateInteractor.isServiceEnabled()
          .subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread())
          .subscribe({
            if (it) {
              view?.onFABEnabled()
            } else {
              view?.onFABDisabled()
            }
          }, { Timber.e(it, "onError") })
    }
  }

  fun setSystemVisibilityFromPreference() {
    dispose {
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

  fun showOnBoarding() {
    dispose {
      lockListInteractor.hasShownOnBoarding()
          .subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread())
          .subscribe({
            if (it) {
              view?.onOnboardingComplete()
            } else {
              view?.onShowOnboarding()
            }
          }, { Timber.e(it, "onError") })
    }
  }

  fun populateList(force: Boolean) {
    dispose(ON_STOP) {
      lockListInteractor.fetchAppEntryList(force)
          .flatMapSingle { lockListInteractor.calculateListDiff(listDiffProvider.data(), it) }
          .subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread())
          .doAfterNext { view?.onListPopulated() }
          .doOnError { view?.onListPopulated() }
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
      FABStateCallback, SystemVisibilityChangeCallback, OnboardingCallback,
      ListPopulateCallback

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

  interface FABStateCallback {

    fun onFABEnabled()

    fun onFABDisabled()
  }

  interface SystemVisibilityChangeCallback {

    fun onSystemVisibilityChanged(visible: Boolean)
  }

  interface OnboardingCallback {

    fun onOnboardingComplete()

    fun onShowOnboarding()
  }

  interface ListPopulateCallback {

    fun onListPopulateBegin()

    fun onListLoaded(result: ListDiffResult<AppEntry>)

    fun onListPopulated()

    fun onListPopulateError(throwable: Throwable)
  }
}
