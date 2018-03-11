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

package com.pyamsoft.padlock.purge

import com.pyamsoft.padlock.api.PurgeInteractor
import com.pyamsoft.padlock.model.PurgeAllEvent
import com.pyamsoft.padlock.model.PurgeEvent
import com.pyamsoft.pydroid.bus.EventBus
import com.pyamsoft.pydroid.list.ListDiffProvider
import com.pyamsoft.pydroid.list.ListDiffResult
import com.pyamsoft.pydroid.presenter.SchedulerPresenter
import io.reactivex.Scheduler
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

@JvmSuppressWildcards
class PurgePresenter @Inject internal constructor(
  private val interactor: PurgeInteractor,
  private val purgeBus: EventBus<PurgeEvent>,
  private val purgeAllBus: EventBus<PurgeAllEvent>,
  private val listDiffProvider: ListDiffProvider<String>,
  @Named("computation") computationScheduler: Scheduler,
  @Named("io") ioScheduler: Scheduler,
  @Named("main") mainScheduler: Scheduler
) : SchedulerPresenter<PurgePresenter.View>(computationScheduler, ioScheduler, mainScheduler) {

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
      interactor.fetchStalePackageNames(force)
          .flatMap { interactor.calculateDiff(listDiffProvider.data(), it) }
          .subscribeOn(ioScheduler)
          .observeOn(mainThreadScheduler)
          .doOnSubscribe { view?.onRetrieveBegin() }
          .doAfterTerminate { view?.onRetrieveComplete() }
          .subscribe({ view?.onRetrievedList(it) }, {
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

    fun onRetrievedList(result: ListDiffResult<String>)

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
