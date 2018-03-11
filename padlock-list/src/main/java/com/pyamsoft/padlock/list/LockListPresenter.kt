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

import com.pyamsoft.padlock.api.LockListInteractor
import com.pyamsoft.padlock.api.LockListUpdater
import com.pyamsoft.padlock.api.LockServiceStateInteractor
import com.pyamsoft.padlock.list.info.LockInfoEvent
import com.pyamsoft.padlock.model.AppEntry
import com.pyamsoft.padlock.model.ClearPinEvent
import com.pyamsoft.padlock.model.CreatePinEvent
import com.pyamsoft.padlock.model.LockState
import com.pyamsoft.padlock.model.LockState.DEFAULT
import com.pyamsoft.padlock.model.LockState.LOCKED
import com.pyamsoft.padlock.model.LockState.WHITELISTED
import com.pyamsoft.padlock.model.LockWhitelistedEvent
import com.pyamsoft.padlock.model.PadLockEntry
import com.pyamsoft.pydroid.bus.EventBus
import com.pyamsoft.pydroid.cache.Cache
import com.pyamsoft.pydroid.list.ListDiffProvider
import com.pyamsoft.pydroid.list.ListDiffResult
import com.pyamsoft.pydroid.presenter.SchedulerPresenter
import io.reactivex.Scheduler
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

@JvmSuppressWildcards
class LockListPresenter @Inject internal constructor(
  private val lockListInteractor: LockListInteractor,
  private val lockListUpdater: LockListUpdater,
  @Named("cache_lock_list") private val cache: Cache,
  private val stateInteractor: LockServiceStateInteractor,
  private val lockListBus: EventBus<LockListEvent>,
  private val lockInfoBus: EventBus<LockInfoEvent>,
  private val lockWhitelistedBus: EventBus<LockWhitelistedEvent>,
  private val clearPinBus: EventBus<ClearPinEvent>,
  private val createPinBus: EventBus<CreatePinEvent>,
  private val lockInfoChangeBus: EventBus<LockInfoEvent.Callback>,
  private val listDiffProvider: ListDiffProvider<AppEntry>,
  @Named("computation") compScheduler: Scheduler,
  @Named("main") mainScheduler: Scheduler,
  @Named("io") ioScheduler: Scheduler
) : SchedulerPresenter<LockListPresenter.View>(compScheduler, ioScheduler, mainScheduler) {

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
          .subscribeOn(ioScheduler)
          .observeOn(mainThreadScheduler)
          .subscribe({ populateList(true) }, {
            Timber.e(it, "Error listening to lock whitelist bus")
          })
    }
  }

  private fun registerOnModifyBus() {
    dispose {
      lockListBus.listen()
          .filter { it is LockListEvent.Modify }
          .map { it as LockListEvent.Modify }
          .subscribeOn(ioScheduler)
          .observeOn(mainThreadScheduler)
          .subscribe({
            modifyDatabaseEntry(it.isChecked, it.packageName, it.code, it.isSystem)
          }, {
            Timber.e(it, "Error listening to lock list bus")
          })
    }

    dispose {
      lockListBus.listen()
          .filter { it is LockListEvent.Callback }
          .map { it as LockListEvent.Callback }
          .subscribeOn(ioScheduler)
          .observeOn(mainThreadScheduler)
          .subscribe({
            when (it) {
              is LockListEvent.Callback.Created -> view?.onModifyEntryCreated(
                  it.packageName
              )
              is LockListEvent.Callback.Deleted -> view?.onModifyEntryDeleted(
                  it.packageName
              )
            }
          }, {
            Timber.e(it, "Error listening to lock info bus")
            view?.onModifyEntryError(it)
          })
    }

    dispose {
      lockInfoBus.listen()
          .filter { it is LockInfoEvent.Callback }
          .map { it as LockInfoEvent.Callback }
          .subscribeOn(ioScheduler)
          .observeOn(mainThreadScheduler)
          .subscribe({ processLockInfoCallback(it) }, {
            Timber.e(it, "Error listening to lock info bus")
            view?.onModifySubEntryError(it)
          })
    }

    dispose {
      lockInfoChangeBus.listen()
          .subscribeOn(ioScheduler)
          .observeOn(mainThreadScheduler)
          .subscribe({ processLockInfoCallback(it) }, {
            Timber.e(it, "Error listening to lock info change bus")
            view?.onModifySubEntryError(it)
          })
    }
  }

  private fun processLockInfoCallback(event: LockInfoEvent.Callback) {
    when (event) {
      is LockInfoEvent.Callback.Created -> {
        if (event.oldState == DEFAULT) {
          view?.onModifySubEntryToHardlockedFromDefault(event.packageName)
        } else if (event.oldState == WHITELISTED) {
          view?.onModifySubEntryToHardlockedFromWhitelisted(event.packageName)
        }
      }
      is LockInfoEvent.Callback.Deleted -> {
        if (event.oldState == WHITELISTED) {
          view?.onModifySubEntryToDefaultFromWhitelisted(event.packageName)
        } else if (event.oldState == LOCKED) {
          view?.onModifySubEntryToDefaultFromHardlocked(event.packageName)
        }
      }
      is LockInfoEvent.Callback.Whitelisted -> {
        if (event.oldState == LOCKED) {
          view?.onModifySubEntryToWhitelistedFromHardlocked(event.packageName)
        } else if (event.oldState == DEFAULT) {
          view?.onModifySubEntryToWhitelistedFromDefault(event.packageName)
        }
      }
    }
  }

  private fun registerOnClearBus() {
    dispose {
      clearPinBus.listen()
          .subscribeOn(ioScheduler)
          .observeOn(mainThreadScheduler)
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
          .subscribeOn(ioScheduler)
          .observeOn(mainThreadScheduler)
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

      lockListInteractor.modifySingleDatabaseEntry(
          oldState, newState, packageName,
          PadLockEntry.PACKAGE_ACTIVITY_NAME, code, system
      )
          .subscribeOn(ioScheduler)
          .observeOn(mainThreadScheduler)
          .subscribe({
            when (it) {
              LockState.DEFAULT -> lockListBus.publish(
                  LockListEvent.Callback.Deleted(packageName)
              )
              LockState.LOCKED -> lockListBus.publish(
                  LockListEvent.Callback.Created(packageName)
              )
              else -> throw RuntimeException("Whitelist/None results are not handled")
            }
          }, {
            Timber.e(it, "onError modifyDatabaseEntry")
            lockListBus.publish(LockListEvent.Callback.Error(it))
          })
    }
  }

  private fun setFABStateFromPreference() {
    dispose {
      stateInteractor.isServiceEnabled()
          .subscribeOn(ioScheduler)
          .observeOn(mainThreadScheduler)
          .subscribe({
            if (it) {
              view?.onFABEnabled()
            } else {
              view?.onFABDisabled()
            }
          }, { Timber.e(it, "onError") })
    }
  }

  fun updateCache(
    packageName: String,
    whitelisted: Int,
    hardLocked: Int
  ) {
    dispose {
      lockListUpdater.update(packageName, whitelisted, hardLocked)
          .subscribeOn(ioScheduler)
          .observeOn(mainThreadScheduler)
          .subscribe({
            Timber.d("Updated $packageName -- W: $whitelisted, H: $hardLocked")
          }, {
            Timber.e(it, "Error updating cache for $packageName")
          })
    }
  }

  fun setSystemVisibilityFromPreference() {
    dispose {
      lockListInteractor.isSystemVisible()
          .subscribeOn(ioScheduler)
          .observeOn(mainThreadScheduler)
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
          .subscribeOn(ioScheduler)
          .observeOn(mainThreadScheduler)
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
    dispose {
      lockListInteractor.fetchAppEntryList(force)
          .flatMap { lockListInteractor.calculateListDiff(listDiffProvider.data(), it) }
          .subscribeOn(ioScheduler)
          .observeOn(mainThreadScheduler)
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
      FABStateCallback, SystemVisibilityChangeCallback, OnboardingCallback,
      ListPopulateCallback, LockSubModifyCallback

  interface LockModifyCallback {

    fun onModifyEntryCreated(packageName: String)

    fun onModifyEntryDeleted(packageName: String)

    fun onModifyEntryError(throwable: Throwable)
  }

  interface LockSubModifyCallback {

    fun onModifySubEntryToDefaultFromWhitelisted(packageName: String)

    fun onModifySubEntryToDefaultFromHardlocked(packageName: String)

    fun onModifySubEntryToWhitelistedFromDefault(packageName: String)

    fun onModifySubEntryToWhitelistedFromHardlocked(packageName: String)

    fun onModifySubEntryToHardlockedFromDefault(packageName: String)

    fun onModifySubEntryToHardlockedFromWhitelisted(packageName: String)

    fun onModifySubEntryError(throwable: Throwable)
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
