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

import com.pyamsoft.padlock.model.PurgeAllEvent
import com.pyamsoft.padlock.model.PurgeEvent
import com.pyamsoft.padlock.api.PurgeInteractor
import com.pyamsoft.padlock.purge.PurgePresenter.View
import com.pyamsoft.pydroid.bus.EventBus
import com.pyamsoft.pydroid.presenter.SchedulerPresenter
import io.reactivex.Scheduler
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

class PurgePresenter @Inject internal constructor(private val interactor: PurgeInteractor,
        private val purgeBus: EventBus<PurgeEvent>,
        private val purgeAllBus: EventBus<PurgeAllEvent>, @Named(
                "computation") computationScheduler: Scheduler, @Named(
                "io") ioScheduler: Scheduler, @Named(
                "main") mainScheduler: Scheduler) : SchedulerPresenter<View>(computationScheduler,
        ioScheduler, mainScheduler) {

    override fun onCreate() {
        super.onCreate()
        registerOnBus()
    }

    override fun onStart() {
        super.onStart()
        retrieveStaleApplications(false)
    }

    private fun registerOnBus() {
        dispose {
            purgeBus.listen()
                    .subscribeOn(ioScheduler)
                    .observeOn(mainThreadScheduler)
                    .subscribe({ view?.onPurge(it.packageName) }, {
                        Timber.e(it, "onError purge single")
                    })
        }

        dispose {
            purgeAllBus.listen()
                    .subscribeOn(ioScheduler)
                    .observeOn(mainThreadScheduler)
                    .subscribe({ view?.onPurgeAll() }, {
                        Timber.e(it, "onError purge all")
                    })
        }
    }

    fun retrieveStaleApplications(force: Boolean) {
        dispose {
            interactor.populateList(force)
                    .subscribeOn(ioScheduler)
                    .observeOn(mainThreadScheduler)
                    .doOnSubscribe { view?.onRetrieveBegin() }
                    .doAfterTerminate { view?.onRetrieveComplete() }
                    .subscribe({ view?.onRetrievedStale(it) }, {
                        Timber.e(it, "onError retrieveStaleApplications")
                        view?.onRetrieveError(it)
                    })
        }
    }

    fun deleteStale(packageName: String) {
        dispose {
            interactor.deleteEntry(packageName)
                    .subscribeOn(ioScheduler)
                    .observeOn(mainThreadScheduler)
                    .subscribe({ view?.onDeleted(it) }
                            , { Timber.e(it, "onError deleteStale") })
        }
    }

    interface View : BusCallback, DeleteCallback, RetrieveCallback

    interface RetrieveCallback {

        fun onRetrieveBegin()

        fun onRetrieveComplete()

        fun onRetrievedStale(packageName: String)

        fun onRetrieveError(throwable: Throwable)
    }

    interface DeleteCallback {

        fun onDeleted(packageName: String)
    }

    interface BusCallback {

        fun onPurge(packageName: String)

        fun onPurgeAll()
    }
}
