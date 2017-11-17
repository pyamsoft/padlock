/*
 *     Copyright (C) 2017 Peter Kenji Yamanaka
 *
 *     This program is free software; you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation; either version 2 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License along
 *     with this program; if not, write to the Free Software Foundation, Inc.,
 *     51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.pyamsoft.padlock.list

import com.pyamsoft.padlock.base.bus.LockWhitelistedEvent
import com.pyamsoft.padlock.base.db.PadLockEntry
import com.pyamsoft.padlock.list.LockListPresenter.View
import com.pyamsoft.padlock.list.info.LockInfoEvent
import com.pyamsoft.padlock.model.AppEntry
import com.pyamsoft.padlock.model.LockState
import com.pyamsoft.padlock.model.LockState.DEFAULT
import com.pyamsoft.padlock.model.LockState.LOCKED
import com.pyamsoft.padlock.model.LockState.WHITELISTED
import com.pyamsoft.padlock.pin.ClearPinEvent
import com.pyamsoft.padlock.pin.CreatePinEvent
import com.pyamsoft.padlock.service.LockServiceStateInteractor
import com.pyamsoft.pydroid.bus.EventBus
import com.pyamsoft.pydroid.data.Cache
import com.pyamsoft.pydroid.presenter.SchedulerPresenter
import io.reactivex.Scheduler
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

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
        @Named("computation") compScheduler: Scheduler,
        @Named("main") mainScheduler: Scheduler,
        @Named("io") ioScheduler: Scheduler) : SchedulerPresenter<View>(compScheduler,
        ioScheduler,
        mainScheduler) {

    override fun onBind(v: View) {
        super.onBind(v)
        registerOnCreateBus(v)
        registerOnClearBus(v)
        registerOnModifyBus(v, v)
        registerOnWhitelistedBus()
    }

    private fun registerOnWhitelistedBus() {
        dispose {
            lockWhitelistedBus.listen()
                    .subscribeOn(ioScheduler).observeOn(mainThreadScheduler)
                    .subscribe({ populateList(true) }, {
                        Timber.e(it, "Error listening to lock whitelist bus")
                    })
        }
    }

    private fun registerOnModifyBus(modifyCallback: LockModifyCallback,
            subModifyCallback: LockSubModifyCallback) {
        dispose {
            lockListBus.listen()
                    .filter { it is LockListEvent.Modify }
                    .map { it as LockListEvent.Modify }
                    .subscribeOn(ioScheduler).observeOn(mainThreadScheduler)
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
                    .subscribeOn(ioScheduler).observeOn(mainThreadScheduler)
                    .subscribe({
                        when (it) {
                            is LockListEvent.Callback.Created -> modifyCallback.onModifyEntryCreated(
                                    it.packageName)
                            is LockListEvent.Callback.Deleted -> modifyCallback.onModifyEntryDeleted(
                                    it.packageName)
                        }
                    }, {
                        Timber.e(it, "Error listening to lock info bus")
                        modifyCallback.onModifyEntryError(it)
                    })
        }

        dispose {
            lockInfoBus.listen()
                    .filter { it is LockInfoEvent.Callback }
                    .map { it as LockInfoEvent.Callback }
                    .subscribeOn(ioScheduler).observeOn(mainThreadScheduler)
                    .subscribe({ processLockInfoCallback(it, subModifyCallback) }, {
                        Timber.e(it, "Error listening to lock info bus")
                        subModifyCallback.onModifySubEntryError(it)
                    })
        }

        dispose {
            lockInfoChangeBus.listen()
                    .subscribeOn(ioScheduler).observeOn(mainThreadScheduler)
                    .subscribe({ processLockInfoCallback(it, subModifyCallback) }, {
                        Timber.e(it, "Error listening to lock info change bus")
                        subModifyCallback.onModifySubEntryError(it)
                    })
        }
    }

    private fun processLockInfoCallback(event: LockInfoEvent.Callback,
            subModifyCallback: LockSubModifyCallback) {
        when (event) {
            is LockInfoEvent.Callback.Created -> {
                if (event.oldState == DEFAULT) {
                    subModifyCallback.onModifySubEntryToHardlockedFromDefault(event.packageName)
                } else if (event.oldState == WHITELISTED) {
                    subModifyCallback.onModifySubEntryToHardlockedFromWhitelisted(event.packageName)
                }
            }
            is LockInfoEvent.Callback.Deleted -> {
                if (event.oldState == WHITELISTED) {
                    subModifyCallback.onModifySubEntryToDefaultFromWhitelisted(event.packageName)
                } else if (event.oldState == LOCKED) {
                    subModifyCallback.onModifySubEntryToDefaultFromHardlocked(event.packageName)
                }
            }
            is LockInfoEvent.Callback.Whitelisted -> {
                if (event.oldState == LOCKED) {
                    subModifyCallback.onModifySubEntryToWhitelistedFromHardlocked(event.packageName)
                } else if (event.oldState == DEFAULT) {
                    subModifyCallback.onModifySubEntryToWhitelistedFromDefault(event.packageName)
                }
            }
        }
    }

    private fun registerOnClearBus(v: MasterPinClearCallback) {
        dispose {
            clearPinBus.listen().subscribeOn(ioScheduler).observeOn(mainThreadScheduler)
                    .subscribe({
                        if (it.success) {
                            v.onMasterPinClearSuccess()
                        } else {
                            v.onMasterPinClearFailure()
                        }
                    }, {
                        Timber.e(it, "error create pin bus")
                    })
        }
    }

    private fun registerOnCreateBus(v: MasterPinCreateCallback) {
        dispose {
            createPinBus.listen().subscribeOn(ioScheduler).observeOn(mainThreadScheduler)
                    .subscribe({
                        if (it.success) {
                            v.onMasterPinCreateSuccess()
                        } else {
                            v.onMasterPinCreateFailure()
                        }
                    }, {
                        Timber.e(it, "error create pin bus")
                    })
        }
    }

    private fun modifyDatabaseEntry(isChecked: Boolean, packageName: String, code: String?,
            system: Boolean) {
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

            lockListInteractor.modifySingleDatabaseEntry(oldState, newState, packageName,
                    PadLockEntry.PACKAGE_ACTIVITY_NAME, code, system)
                    .subscribeOn(ioScheduler)
                    .observeOn(mainThreadScheduler)
                    .subscribe({
                        when (it) {
                            LockState.DEFAULT -> lockListBus.publish(
                                    LockListEvent.Callback.Deleted(packageName))
                            LockState.LOCKED -> lockListBus.publish(
                                    LockListEvent.Callback.Created(packageName))
                            else -> throw RuntimeException("Whitelist/None results are not handled")
                        }
                    }, {
                        Timber.e(it, "onError modifyDatabaseEntry")
                        lockListBus.publish(LockListEvent.Callback.Error(it))
                    })
        }
    }

    fun setFABStateFromPreference() {
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

    fun updateCache(packageName: String, whitelisted: Int, hardLocked: Int) {
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
            lockListInteractor.populateList(force)
                    .subscribeOn(ioScheduler)
                    .observeOn(mainThreadScheduler)
                    .doAfterTerminate { view?.onListPopulated() }
                    .doOnSubscribe { view?.onListPopulateBegin() }
                    .subscribe({ view?.onEntryAddedToList(it) }, {
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

        fun onEntryAddedToList(entry: AppEntry)

        fun onListPopulated()

        fun onListPopulateError(throwable: Throwable)
    }
}
