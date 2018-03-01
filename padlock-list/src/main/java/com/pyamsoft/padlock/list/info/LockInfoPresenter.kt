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

import com.pyamsoft.padlock.api.LockInfoInteractor
import com.pyamsoft.padlock.api.LockInfoUpdater
import com.pyamsoft.padlock.list.info.LockInfoEvent.Callback.*
import com.pyamsoft.padlock.model.ActivityEntry
import com.pyamsoft.padlock.model.LockState
import com.pyamsoft.padlock.model.LockWhitelistedEvent
import com.pyamsoft.pydroid.bus.EventBus
import com.pyamsoft.pydroid.list.ListDiffProvider
import com.pyamsoft.pydroid.list.ListDiffResult
import com.pyamsoft.pydroid.presenter.SchedulerPresenter
import io.reactivex.Scheduler
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

@JvmSuppressWildcards
class LockInfoPresenter @Inject internal constructor(
    private val changeBus: EventBus<LockInfoEvent.Callback>,
    private val lockWhitelistedBus: EventBus<LockWhitelistedEvent>,
    private val bus: EventBus<LockInfoEvent>,
    @param:Named("package_name") private val packageName: String,
    private val lockInfoUpdater: LockInfoUpdater,
    private val interactor: LockInfoInteractor,
    private val listDiffProvider: ListDiffProvider<ActivityEntry>,
    @Named("computation") compScheduler: Scheduler,
    @Named("io") ioScheduler: Scheduler,
    @Named("main") mainScheduler: Scheduler
) : SchedulerPresenter<LockInfoPresenter.View>(compScheduler, ioScheduler, mainScheduler) {

  override fun onCreate() {
    super.onCreate()
    registerOnModifyBus()
    registerOnWhitelistedBus()
  }

  override fun onStart() {
    super.onStart()
    populateList(false)
  }

  private fun registerOnWhitelistedBus() {
    dispose {
      lockWhitelistedBus.listen()
          .filter { it.packageName == packageName }
          .subscribeOn(ioScheduler)
          .observeOn(mainThreadScheduler)
          .subscribe({ populateList(true) }, {
            Timber.e(it, "Error listening to lock whitelist bus")
          })
    }
  }

  private fun registerOnModifyBus() {
    dispose {
      bus.listen()
          .filter { it is LockInfoEvent.Modify }
          .map { it as LockInfoEvent.Modify }
          .subscribeOn(ioScheduler)
          .observeOn(mainThreadScheduler)
          .subscribe({ modifyDatabaseEntry(it) }, {
            Timber.e(it, "Error listening to lock info bus")
          })
    }

    dispose {
      bus.listen()
          .filter { it is LockInfoEvent.Callback }
          .map { it as LockInfoEvent.Callback }
          .subscribeOn(ioScheduler)
          .observeOn(mainThreadScheduler)
          .subscribe({
            when (it) {
              is Created -> view?.onModifyEntryCreated(it.id)
              is Deleted -> view?.onModifyEntryDeleted(it.id)
              is Whitelisted -> view?.onModifyEntryWhitelisted(it.id)
            }
          }, {
            Timber.e(it, "Error listening to lock info bus")
            view?.onModifyEntryError(it)
          })
    }
  }

  private fun modifyDatabaseEntry(event: LockInfoEvent.Modify) {
    dispose {
      interactor.modifySingleDatabaseEntry(
          event.oldState, event.newState,
          event.packageName,
          event.name, event.code, event.system
      )
          .subscribeOn(ioScheduler)
          .observeOn(mainThreadScheduler)
          .subscribe({
            val id: String = event.id
            when (it) {
              LockState.LOCKED -> bus.publish(
                  Created(id, event.packageName, event.oldState)
              )
              LockState.DEFAULT -> bus.publish(
                  Deleted(id, event.packageName, event.oldState)
              )
              LockState.WHITELISTED -> bus.publish(
                  Whitelisted(id, event.packageName, event.oldState)
              )
              else -> throw IllegalStateException("Unsupported lock state: $it")
            }
          }, {
            Timber.e(it, "onError modifyDatabaseEntry")
            bus.publish(Error(it, event.packageName))
          })
    }
  }

  fun update(
      packageName: String,
      activityName: String,
      lockState: LockState
  ) {
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
      interactor.fetchActivityEntryList(forceRefresh, packageName)
          .flatMap { interactor.calculateListDiff(packageName, listDiffProvider.data(), it) }
          .subscribeOn(ioScheduler)
          .observeOn(mainThreadScheduler)
          .doAfterTerminate { view?.onListPopulated() }
          .doOnSubscribe { view?.onListPopulateBegin() }
          .subscribe({ view?.onListLoaded(it) }, {
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

    fun onListLoaded(result: ListDiffResult<ActivityEntry>)

    fun onListPopulateError(throwable: Throwable)

    fun onListPopulated()
  }

  interface OnboardingCallback {

    fun onOnboardingComplete()

    fun onShowOnboarding()
  }
}
