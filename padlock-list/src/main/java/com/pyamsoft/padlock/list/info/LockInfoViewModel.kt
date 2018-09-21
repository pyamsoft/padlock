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

import androidx.annotation.CheckResult
import androidx.lifecycle.LifecycleOwner
import com.pyamsoft.padlock.api.LockInfoInteractor
import com.pyamsoft.padlock.model.LockWhitelistedEvent
import com.pyamsoft.padlock.model.list.ActivityEntry
import com.pyamsoft.padlock.model.list.ListDiffProvider
import com.pyamsoft.padlock.model.list.LockInfoUpdatePayload
import com.pyamsoft.pydroid.core.bus.Listener
import com.pyamsoft.pydroid.core.singleDisposable
import com.pyamsoft.pydroid.core.threads.Enforcer
import com.pyamsoft.pydroid.core.tryDispose
import com.pyamsoft.pydroid.core.viewmodel.BaseViewModel
import com.pyamsoft.pydroid.core.viewmodel.DataBus
import com.pyamsoft.pydroid.core.viewmodel.DataWrapper
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

@JvmSuppressWildcards
class LockInfoViewModel @Inject internal constructor(
  owner: LifecycleOwner,
  private val enforcer: Enforcer,
  private val lockWhitelistedBus: Listener<LockWhitelistedEvent>,
  private val bus: Listener<LockInfoEvent>,
  private val interactor: LockInfoInteractor,
  @param:Named("package_name") private val packageName: String,
  private val listDiffProvider: ListDiffProvider<ActivityEntry>
) : BaseViewModel(owner) {

  private val populateListBus = DataBus<List<ActivityEntry>>()
  private val databaseChangeBus = DataBus<LockInfoUpdatePayload>()

  private var populateListDisposable by singleDisposable()

  override fun onCleared() {
    super.onCleared()
    populateListDisposable.tryDispose()
  }

  fun onDatabaseChangeEvent(func: (DataWrapper<LockInfoUpdatePayload>) -> Unit) {
    dispose {
      interactor.subscribeForUpdates(packageName, listDiffProvider)
          .subscribeOn(Schedulers.computation())
          .observeOn(AndroidSchedulers.mainThread())
          .doOnSubscribe { databaseChangeBus.publishLoading(false) }
          .doAfterTerminate { databaseChangeBus.publishComplete() }
          .subscribe({ databaseChangeBus.publishSuccess(it) }, {
            Timber.e(it, "Error while subscribed to database changes")
            databaseChangeBus.publishError(it)
          })

    }

    dispose {
      databaseChangeBus.listen()
          .subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread())
          .subscribe(func)
    }
  }

  fun onPopulateListEvent(func: (DataWrapper<List<ActivityEntry>>) -> Unit) {
    dispose {
      populateListBus.listen()
          .subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread())
          .subscribe(func)
    }

    dispose {
      lockWhitelistedBus.listen()
          .filter { it.packageName == packageName }
          .subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread())
          .subscribe { populateList(true) }
    }
  }

  fun onModifyError(func: (Throwable) -> Unit) {
    dispose {
      bus.listen()
          .observeOn(Schedulers.io())
          .flatMapSingle { modifyDatabaseEntry(it) }
          .subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread())
          .doOnError {
            Timber.e(it, "Error occurred modifying database entry")
            func(it)
          }
          .onErrorReturnItem(Unit)
          .subscribe()
    }
  }

  @CheckResult
  private fun modifyDatabaseEntry(event: LockInfoEvent): Single<Unit> {
    return Single.defer {
      enforcer.assertNotOnMainThread()
      return@defer interactor.modifyEntry(
          event.oldState, event.newState,
          event.packageName,
          event.name, event.code, event.system
      )
          .andThen(Single.just(Unit))
    }
  }

  fun populateList(force: Boolean) {
    populateListDisposable = interactor.fetchActivityEntryList(force, packageName)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .doAfterTerminate { populateListBus.publishComplete() }
        .doOnSubscribe { populateListBus.publishLoading(force) }
        .subscribe({ populateListBus.publishSuccess(it) }, {
          Timber.e(it, "LockInfoViewModel populateList error")
          populateListBus.publishError(it)
        })
  }

}
