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

import android.arch.lifecycle.Lifecycle.Event.ON_STOP
import com.pyamsoft.padlock.api.PurgeInteractor
import com.pyamsoft.padlock.model.PurgeAllEvent
import com.pyamsoft.padlock.model.PurgeEvent
import com.pyamsoft.pydroid.bus.EventBus
import com.pyamsoft.pydroid.list.ListDiffProvider
import com.pyamsoft.pydroid.list.ListDiffResult
import com.pyamsoft.pydroid.presenter.Presenter
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject

@JvmSuppressWildcards
class PurgePresenter @Inject internal constructor(
  private val interactor: PurgeInteractor,
  private val purgeBus: EventBus<PurgeEvent>,
  private val purgeAllBus: EventBus<PurgeAllEvent>,
  private val listDiffProvider: ListDiffProvider<String>
) : Presenter<PurgePresenter.View>() {

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
          .subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread())
          .subscribe({ view?.onPurge(it.packageName) }, {
            Timber.e(it, "onError purge single")
          })
    }

    dispose {
      purgeAllBus.listen()
          .subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread())
          .subscribe({ view?.onPurgeAll() }, {
            Timber.e(it, "onError purge all")
          })
    }
  }

  fun retrieveStaleApplications(force: Boolean) {
    dispose(ON_STOP) {
      interactor.fetchStalePackageNames(force)
          .flatMap { interactor.calculateDiff(listDiffProvider.data(), it) }
          .subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread())
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
          .subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread())
          .subscribe({ view?.onDeleted() }
              , { Timber.e(it, "onError deleteStale") })
    }
  }

  fun deleteStale(packageNames: List<String>) {
    dispose {
      interactor.deleteEntries(packageNames)
          .subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread())
          .subscribe({ view?.onDeleted() }
              , { Timber.e(it, "onError deleteStale all") })
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

    fun onDeleted()
  }

  interface BusCallback {

    fun onPurge(packageName: String)

    fun onPurgeAll()
  }
}
