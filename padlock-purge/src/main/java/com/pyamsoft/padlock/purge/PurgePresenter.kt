/*
 * Copyright 2017 Peter Kenji Yamanaka
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
 */

package com.pyamsoft.padlock.purge

import com.pyamsoft.padlock.purge.PurgePresenter.BusCallback
import com.pyamsoft.padlock.purge.PurgePresenter.Callback
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
        "main") mainScheduler: Scheduler) : SchedulerPresenter<BusCallback, Callback>(
    computationScheduler,
    ioScheduler, mainScheduler) {

  override fun onCreate(bound: BusCallback) {
    super.onCreate(bound)
    registerOnBus(bound::onPurge, bound::onPurgeAll)
  }

  override fun onStart(bound: Callback) {
    super.onStart(bound)
    retrieveStaleApplications(false, bound::onRetrieveBegin, bound::onStaleApplicationReceived,
        bound::onRetrievalCompleted)
  }

  private fun registerOnBus(onPurge: (String) -> Unit, onPurgeAll: () -> Unit) {
    disposeOnDestroy {
      purgeBus.listen()
          .subscribeOn(ioScheduler)
          .observeOn(mainThreadScheduler)
          .subscribe({ onPurge(it.packageName) }, {
            Timber.e(it, "onError purge single")
          })
    }

    disposeOnDestroy {
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
    disposeOnStop {
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
    disposeOnStop {
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

  interface Callback {

    fun onRetrieveBegin()

    fun onStaleApplicationReceived(pacakgeName: String)

    fun onRetrievalCompleted()
  }
}
