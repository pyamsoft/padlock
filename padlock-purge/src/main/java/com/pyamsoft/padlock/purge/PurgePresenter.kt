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

package com.pyamsoft.padlock.purge

import com.pyamsoft.padlock.purge.PurgePresenter.BusCallback
import com.pyamsoft.pydroid.bus.EventBus
import com.pyamsoft.pydroid.presenter.SchedulerPresenter
import io.reactivex.Scheduler
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

class PurgePresenter @Inject internal constructor(private val interactor: PurgeInteractor,
    private val purgeBus: EventBus<PurgeEvent>,
    private val purgeAllBus: EventBus<PurgeAllEvent>, @Named(
        "computation") computationScheduler: Scheduler, @Named("io") ioScheduler: Scheduler, @Named(
        "main") mainScheduler: Scheduler) : SchedulerPresenter<BusCallback>(computationScheduler,
    ioScheduler, mainScheduler) {

  override fun onBind(v: BusCallback) {
    super.onBind(v)
    registerOnBus(v::onPurge, v::onPurgeAll)
  }

  private fun registerOnBus(onPurge: (String) -> Unit, onPurgeAll: () -> Unit) {
    dispose {
      purgeBus.listen()
          .subscribeOn(ioScheduler)
          .observeOn(mainThreadScheduler)
          .subscribe({ onPurge(it.packageName) }, {
            Timber.e(it, "onError purge single")
          })
    }

    dispose {
      purgeAllBus.listen()
          .subscribeOn(ioScheduler)
          .observeOn(mainThreadScheduler)
          .subscribe({ onPurgeAll() }, {
            Timber.e(it, "onError purge all")
          })
    }
  }

  fun retrieveStaleApplications(force: Boolean, onRetrieveBegin: () -> Unit,
      onStaleApplicationRetrieved: (String) -> Unit,
      onRetrievalComplete: () -> Unit) {
    dispose {
      interactor.populateList(force)
          .subscribeOn(ioScheduler)
          .observeOn(mainThreadScheduler)
          .doOnSubscribe { onRetrieveBegin() }
          .doAfterTerminate { onRetrievalComplete() }
          .subscribe({ onStaleApplicationRetrieved(it) },
              { Timber.e(it, "onError retrieveStaleApplications") })
    }
  }

  fun deleteStale(packageName: String, onDeleted: (String) -> Unit) {
    dispose {
      interactor.deleteEntry(packageName)
          .subscribeOn(ioScheduler)
          .observeOn(mainThreadScheduler)
          .subscribe({ onDeleted(it) }
              , { Timber.e(it, "onError deleteStale") })
    }
  }

  interface BusCallback {

    fun onPurge(packageName: String)

    fun onPurgeAll()
  }
}
