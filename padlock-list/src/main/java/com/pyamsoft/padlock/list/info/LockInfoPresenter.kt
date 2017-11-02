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

package com.pyamsoft.padlock.list.info

import com.pyamsoft.padlock.list.info.LockInfoEvent.Callback.Created
import com.pyamsoft.padlock.list.info.LockInfoEvent.Callback.Deleted
import com.pyamsoft.padlock.list.info.LockInfoEvent.Callback.Error
import com.pyamsoft.padlock.list.info.LockInfoEvent.Callback.Whitelisted
import com.pyamsoft.padlock.list.info.LockInfoPresenter.View
import com.pyamsoft.padlock.model.ActivityEntry
import com.pyamsoft.padlock.model.LockState
import com.pyamsoft.pydroid.bus.EventBus
import com.pyamsoft.pydroid.presenter.SchedulerPresenter
import io.reactivex.Scheduler
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

class LockInfoPresenter @Inject internal constructor(
    private val changeBus: EventBus<LockInfoEvent.Callback>,
    private val bus: EventBus<LockInfoEvent>, @param:Named(
        "package_name") private val packageName: String,
    private val lockInfoUpdater: LockInfoUpdater,
    private val interactor: LockInfoInteractor, @Named("computation") compScheduler: Scheduler,
    @Named("io") ioScheduler: Scheduler, @Named(
        "main") mainScheduler: Scheduler) : SchedulerPresenter<View>(compScheduler,
    ioScheduler, mainScheduler) {

  override fun onBind(v: View) {
    super.onBind(v)
    registerOnModifyBus(v)
  }

  private fun registerOnModifyBus(v: LockModifyCallback) {
    dispose {
      bus.listen()
          .filter { it is LockInfoEvent.Modify }
          .map { it as LockInfoEvent.Modify }
          .subscribeOn(ioScheduler).observeOn(mainThreadScheduler)
          .subscribe({ modifyDatabaseEntry(it) }, {
            Timber.e(it, "Error listening to lock info bus")
          })
    }

    dispose {
      bus.listen()
          .filter { it is LockInfoEvent.Callback }
          .map { it as LockInfoEvent.Callback }
          .subscribeOn(ioScheduler).observeOn(mainThreadScheduler)
          .subscribe({
            when (it) {
              is Created -> v.onModifyEntryCreated(it.id)
              is Deleted -> v.onModifyEntryDeleted(it.id)
              is Whitelisted -> v.onModifyEntryWhitelisted(it.id)
            }
          }, {
            Timber.e(it, "Error listening to lock info bus")
            v.onModifyEntryError(it)
          })
    }
  }

  private fun modifyDatabaseEntry(event: LockInfoEvent.Modify) {
    dispose {
      interactor.modifySingleDatabaseEntry(event.oldState, event.newState,
          event.packageName,
          event.name, event.code, event.system)
          .subscribeOn(ioScheduler)
          .observeOn(mainThreadScheduler)
          .subscribe({
            val id: String = event.id
            when (it) {
              LockState.LOCKED -> bus.publish(Created(id, event.packageName, event.oldState))
              LockState.DEFAULT -> bus.publish(Deleted(id, event.packageName, event.oldState))
              LockState.WHITELISTED -> bus.publish(
                  Whitelisted(id, event.packageName, event.oldState))
              else -> throw IllegalStateException("Unsupported lock state: $it")
            }
          }, {
            Timber.e(it, "onError modifyDatabaseEntry")
            bus.publish(Error(it, event.packageName))
          })
    }
  }

  fun update(packageName: String, activityName: String, lockState: LockState) {
    dispose {
      lockInfoUpdater.update(packageName, activityName, lockState)
          .subscribeOn(ioScheduler)
          .observeOn(mainThreadScheduler)
          .subscribe({
            Timber.d("Updated $packageName $activityName -- state: $lockState")
          }, {
            Timber.e(it, "Error updating cache for $packageName $activityName")
          })
    }
  }

  fun populateList(forceRefresh: Boolean) {
    dispose {
      interactor.populateList(packageName, forceRefresh)
          .subscribeOn(ioScheduler)
          .observeOn(mainThreadScheduler)
          .doAfterTerminate { view?.onListPopulated() }
          .doOnSubscribe { view?.onListPopulateBegin() }
          .subscribe({ view?.onEntryAddedToList(it) }, {
            Timber.e(it, "LockInfoPresenterImpl populateList onError")
            view?.onListPopulateError(it)
          })
    }
  }

  fun showOnBoarding() {
    dispose {
      interactor.hasShownOnBoarding()
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

  fun publish(event: LockInfoEvent.Callback) {
    changeBus.publish(event)
  }

  interface View : LockModifyCallback, ListPopulateCallback, OnboardingCallback

  interface LockModifyCallback {

    fun onModifyEntryCreated(id: String)

    fun onModifyEntryDeleted(id: String)

    fun onModifyEntryWhitelisted(id: String)

    fun onModifyEntryError(throwable: Throwable)
  }

  interface ListPopulateCallback {

    fun onListPopulateBegin()

    fun onListPopulated()

    fun onEntryAddedToList(entry: ActivityEntry)

    fun onListPopulateError(throwable: Throwable)

  }

  interface OnboardingCallback {

    fun onOnboardingComplete()

    fun onShowOnboarding()
  }
}
