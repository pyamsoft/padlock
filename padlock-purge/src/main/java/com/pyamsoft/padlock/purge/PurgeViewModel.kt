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

import androidx.annotation.CheckResult
import androidx.lifecycle.LifecycleOwner
import com.pyamsoft.padlock.api.PurgeInteractor
import com.pyamsoft.padlock.model.list.ListDiffProvider
import com.pyamsoft.padlock.model.purge.PurgeAllEvent
import com.pyamsoft.padlock.model.purge.PurgeEvent
import com.pyamsoft.pydroid.core.bus.Listener
import com.pyamsoft.pydroid.core.threads.Enforcer
import com.pyamsoft.pydroid.core.viewmodel.BaseViewModel
import com.pyamsoft.pydroid.core.viewmodel.DataBus
import com.pyamsoft.pydroid.core.viewmodel.DataWrapper
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject

@JvmSuppressWildcards
class PurgeViewModel @Inject internal constructor(
  owner: LifecycleOwner,
  private val enforcer: Enforcer,
  private val purgeListDiffProvider: ListDiffProvider<String>,
  private val interactor: PurgeInteractor,
  private val purgeBus: Listener<PurgeEvent>,
  private val purgeAllBus: Listener<PurgeAllEvent>
) : BaseViewModel(owner) {

  private var fetchDisposable by disposable()
  private val fetchBus = DataBus<List<String>>()

  override fun onCleared() {
    super.onCleared()
    fetchDisposable.tryDispose()
  }

  @CheckResult
  private fun deleteStale(packageName: String): Single<String> {
    return interactor.deleteEntry(packageName)
        .andThen(Single.just(packageName))
  }

  fun onPurgeEvent(func: (String) -> Unit) {
    dispose {
      purgeBus.listen()
          .observeOn(Schedulers.io())
          .flatMapSingle {
            enforcer.assertNotOnMainThread()
            return@flatMapSingle deleteStale(it.packageName)
                .observeOn(Schedulers.io())
          }
          .subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread())
          .subscribe(func)
    }
  }

  @CheckResult
  private fun deleteAll(packageNames: List<String>): Single<List<String>> {
    return interactor.deleteEntries(packageNames)
        .andThen(Single.just(packageNames))
  }

  fun onPurgeAllEvent(func: (List<String>) -> Unit) {
    dispose {
      purgeAllBus.listen()
          .observeOn(Schedulers.io())
          .flatMapSingle {
            enforcer.assertNotOnMainThread()
            return@flatMapSingle deleteAll(purgeListDiffProvider.data())
                .observeOn(Schedulers.io())
          }
          .subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread())
          .subscribe(func)
    }
  }

  fun onStaleApplicationsFetched(func: (DataWrapper<List<String>>) -> Unit) {
    dispose {
      fetchBus.listen()
          .subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread())
          .subscribe(func)
    }
  }

  fun fetch(force: Boolean) {
    fetchDisposable = interactor.fetchStalePackageNames(force)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .doAfterTerminate { fetchBus.publishComplete() }
        .doOnSubscribe { fetchBus.publishLoading(force) }
        .subscribe({ fetchBus.publishSuccess(it) }, {
          Timber.e(it, "Error fetching stale applications")
          fetchBus.publishError(it)
        })
  }
}
