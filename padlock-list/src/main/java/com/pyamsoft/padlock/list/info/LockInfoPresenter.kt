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

package com.pyamsoft.padlock.list.info

import androidx.lifecycle.Lifecycle.Event.ON_STOP
import com.pyamsoft.padlock.api.LockInfoInteractor
import com.pyamsoft.padlock.model.LockWhitelistedEvent
import com.pyamsoft.padlock.model.list.ActivityEntry
import com.pyamsoft.pydroid.core.bus.EventBus
import com.pyamsoft.pydroid.core.presenter.Presenter
import com.pyamsoft.pydroid.list.ListDiffProvider
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

@JvmSuppressWildcards
class LockInfoPresenter @Inject internal constructor(
  private val lockWhitelistedBus: EventBus<LockWhitelistedEvent>,
  private val bus: EventBus<LockInfoEvent>,
  private val interactor: LockInfoInteractor,
  @param:Named("package_name") private val packageName: String,
  private val listDiffProvider: ListDiffProvider<ActivityEntry>
) : Presenter<LockInfoPresenter.View>() {

  override fun onCreate() {
    super.onCreate()
    registerOnModifyBus()
    registerOnWhitelistedBus()
  }

  override fun onStart() {
    super.onStart()
    populateList(false)
    subscribeToDatabaseChanges()
  }

  private fun subscribeToDatabaseChanges() {
    dispose(ON_STOP) {
      interactor.subscribeForUpdates(packageName, listDiffProvider)
          .subscribeOn(Schedulers.computation())
          .observeOn(AndroidSchedulers.mainThread())
          .subscribe({ view?.onDatabaseChangeReceived(it.index, it.entry) }, {
            Timber.e(it, "Error while subscribed to database changes")
            view?.onDatabaseChangeError(it)
          })
    }
  }

  private fun registerOnWhitelistedBus() {
    dispose {
      lockWhitelistedBus.listen()
          .filter { it.packageName == packageName }
          .subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread())
          .subscribe({ populateList(true) }, {
            Timber.e(it, "Error listening to lock whitelist bus")
          })
    }
  }

  private fun registerOnModifyBus() {
    dispose {
      bus.listen()
          .subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread())
          .subscribe({ modifyDatabaseEntry(it) }, {
            Timber.e(it, "Error listening to lock info bus")
          })
    }
  }

  private fun modifyDatabaseEntry(event: LockInfoEvent) {
    dispose {
      interactor.modifyEntry(
          event.oldState, event.newState,
          event.packageName,
          event.name, event.code, event.system
      )
          .subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread())
          .subscribe({ Timber.d("Modify completed") }, {
            Timber.e(it, "onError modifyEntry")
            view?.onModifyEntryError(it)
          })
    }
  }

  fun populateList(forceRefresh: Boolean) {
    dispose(ON_STOP) {
      interactor.fetchActivityEntryList(forceRefresh, packageName)
          .subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread())
          .doAfterTerminate { view?.onListPopulated() }
          .doOnSubscribe { view?.onListPopulateBegin() }
          .subscribe({ view?.onListLoaded(it) }, {
            Timber.e(it, "LockInfoPresenter populateList onError")
            view?.onListPopulateError(it)
          })
    }
  }

  fun showOnBoarding() {
    dispose {
      interactor.hasShownOnBoarding()
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

  interface View : LockModifyCallback, ListPopulateCallback, OnboardingCallback, ChangeCallback

  interface ChangeCallback {

    fun onDatabaseChangeReceived(
      index: Int,
      entry: ActivityEntry
    )

    fun onDatabaseChangeError(throwable: Throwable)
  }

  interface LockModifyCallback {

    fun onModifyEntryError(throwable: Throwable)
  }

  interface ListPopulateCallback {

    fun onListPopulateBegin()

    fun onListLoaded(list: List<ActivityEntry>)

    fun onListPopulateError(throwable: Throwable)

    fun onListPopulated()
  }

  interface OnboardingCallback {

    fun onOnboardingComplete()

    fun onShowOnboarding()
  }
}
