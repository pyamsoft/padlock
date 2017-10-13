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
import com.pyamsoft.padlock.list.info.LockInfoPresenter.BusCallback
import com.pyamsoft.padlock.model.ActivityEntry
import com.pyamsoft.padlock.model.LockState
import com.pyamsoft.pydroid.bus.EventBus
import com.pyamsoft.pydroid.presenter.SchedulerPresenter
import io.reactivex.Scheduler
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

class LockInfoPresenter @Inject internal constructor(
    private val bus: EventBus<LockInfoEvent>, @param:Named(
        "package_name") private val packageName: String,
    private val interactor: LockInfoInteractor, @Named("computation") compScheduler: Scheduler,
    @Named("io") ioScheduler: Scheduler, @Named(
        "main") mainScheduler: Scheduler) : SchedulerPresenter<BusCallback>(compScheduler,
    ioScheduler, mainScheduler) {

  override fun onBind(v: BusCallback) {
    super.onBind(v)
    registerOnModifyBus(v::onEntryCreated, v::onEntryDeleted, v::onEntryWhitelisted,
        v::onEntryError)
  }

  private fun registerOnModifyBus(onEntryCreated: (String) -> Unit,
      onEntryDeleted: (String) -> Unit, onEntryWhitelisted: (String) -> Unit,
      onEntryError: (Throwable) -> Unit) {
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
              is Created -> onEntryCreated(it.id)
              is Deleted -> onEntryDeleted(it.id)
              is Whitelisted -> onEntryWhitelisted(it.id)
            }
          }, {
            Timber.e(it, "Error listening to lock info bus")
            onEntryError(it)
          })
    }
  }

  private fun modifyDatabaseEntry(event: LockInfoEvent.Modify) {
    dispose {
      interactor.modifySingleDatabaseEntry(event.oldState(), event.newState,
          event.packageName(),
          event.name(), event.code, event.system)
          .subscribeOn(ioScheduler)
          .observeOn(mainThreadScheduler)
          .subscribe({
            val id: String = event.id()
            when (it) {
              LockState.LOCKED -> bus.publish(Created(id))
              LockState.DEFAULT -> bus.publish(Deleted(id))
              LockState.WHITELISTED -> bus.publish(Whitelisted(id))
              else -> throw IllegalStateException("Unsupported lock state: $it")
            }
          }, {
            Timber.e(it, "onError modifyDatabaseEntry")
            bus.publish(Error(it))
          })
    }
  }

  fun populateList(forceRefresh: Boolean, onListPopulateBegin: () -> Unit,
      onEntryAddedToList: (ActivityEntry) -> Unit, onListPopulateError: (Throwable) -> Unit,
      onListPopulated: () -> Unit) {
    dispose {
      interactor.populateList(packageName, forceRefresh)
          .subscribeOn(ioScheduler)
          .observeOn(mainThreadScheduler)
          .doAfterTerminate { onListPopulated() }
          .doOnSubscribe { onListPopulateBegin() }
          .subscribe({ onEntryAddedToList(it) }, {
            Timber.e(it, "LockInfoPresenterImpl populateList onError")
            onListPopulateError(it)
          })
    }
  }

  fun showOnBoarding(onShowOnboarding: () -> Unit, onOnboardingComplete: () -> Unit) {
    dispose {
      interactor.hasShownOnBoarding()
          .subscribeOn(ioScheduler)
          .observeOn(mainThreadScheduler)
          .subscribe({
            if (it) {
              onOnboardingComplete()
            } else {
              onShowOnboarding()
            }
          }, { Timber.e(it, "onError") })
    }
  }

  interface BusCallback {

    fun onEntryCreated(id: String)

    fun onEntryDeleted(id: String)

    fun onEntryWhitelisted(id: String)

    fun onEntryError(throwable: Throwable)
  }
}
