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
import com.pyamsoft.padlock.api.PurgeInteractor
import com.pyamsoft.padlock.model.list.ListDiffProvider
import com.pyamsoft.padlock.model.purge.PurgeAllEvent
import com.pyamsoft.padlock.model.purge.PurgeEvent
import com.pyamsoft.pydroid.core.bus.Listener
import com.pyamsoft.pydroid.core.threads.Enforcer
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject

@JvmSuppressWildcards
class PurgeViewModel @Inject internal constructor(
  private val enforcer: Enforcer,
  private val purgeListDiffProvider: ListDiffProvider<String>,
  private val interactor: PurgeInteractor,
  private val purgeBus: Listener<PurgeEvent>,
  private val purgeAllBus: Listener<PurgeAllEvent>
) {

  @CheckResult
  private fun deleteStale(packageName: String): Single<String> {
    enforcer.assertNotOnMainThread()
    return interactor.deleteEntry(packageName)
        .andThen(Single.just(packageName))
        .subscribeOn(Schedulers.io())
        .observeOn(Schedulers.io())
  }

  @CheckResult
  fun onPurgeEvent(func: (String) -> Unit): Disposable {
    return purgeBus.listen()
        .map { it.packageName }
        .subscribeOn(Schedulers.io())
        .observeOn(Schedulers.io())
        .flatMapSingle { deleteStale(it) }
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(func)
  }

  @CheckResult
  private fun deleteAll(packageNames: List<String>): Single<List<String>> {
    enforcer.assertNotOnMainThread()
    return interactor.deleteEntries(packageNames)
        .andThen(Single.just(packageNames))
        .subscribeOn(Schedulers.io())
        .observeOn(Schedulers.io())
  }

  @CheckResult
  fun onPurgeAllEvent(func: (List<String>) -> Unit): Disposable {
    return purgeAllBus.listen()
        .map { purgeListDiffProvider.data() }
        .subscribeOn(Schedulers.io())
        .observeOn(Schedulers.io())
        .flatMapSingle { deleteAll(it) }
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(func)
  }

  @CheckResult
  fun fetchStalePackages(
    force: Boolean,
    onFetchBegin: (forced: Boolean) -> Unit,
    onFetchSuccess: (stalePackages: List<String>) -> Unit,
    onFetchError: (error: Throwable) -> Unit,
    onFetchComplete: () -> Unit
  ): Disposable {
    return interactor.fetchStalePackageNames(force)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .doAfterTerminate { onFetchComplete() }
        .doOnSubscribe { onFetchBegin(force) }
        .subscribe({ onFetchSuccess(it) }, {
          Timber.e(it, "Error fetching stale applications")
          onFetchError(it)
        })
  }
}
