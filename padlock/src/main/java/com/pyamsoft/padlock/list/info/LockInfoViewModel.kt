/*
 * Copyright 2019 Peter Kenji Yamanaka
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
 *
 */

package com.pyamsoft.padlock.list.info

import androidx.annotation.CheckResult
import com.pyamsoft.padlock.api.LockInfoInteractor
import com.pyamsoft.padlock.list.info.LockInfoEvent
import com.pyamsoft.padlock.model.LockWhitelistedEvent
import com.pyamsoft.padlock.model.list.ActivityEntry
import com.pyamsoft.padlock.model.list.ListDiffProvider
import com.pyamsoft.padlock.model.list.LockInfoUpdatePayload
import com.pyamsoft.pydroid.core.bus.Listener
import com.pyamsoft.pydroid.core.threads.Enforcer
import com.pyamsoft.pydroid.core.tryDispose
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

class LockInfoViewModel @Inject internal constructor(
  private val enforcer: Enforcer,
  private val lockWhitelistedBus: Listener<LockWhitelistedEvent>,
  private val bus: Listener<LockInfoEvent>,
  private val interactor: LockInfoInteractor,
  @param:Named("package_name") private val packageName: String,
  private val listDiffProvider: ListDiffProvider<ActivityEntry>
) {

  @CheckResult
  fun onDatabaseChangeEvent(
    onChange: (payload: LockInfoUpdatePayload) -> Unit,
    onError: (error: Throwable) -> Unit
  ): Disposable {
    return interactor.subscribeForUpdates(packageName, listDiffProvider)
        .subscribeOn(Schedulers.computation())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe({ onChange(it) }, {
          Timber.e(it, "Error while subscribed to database changes")
          onError(it)
        })
  }

  @CheckResult
  fun onLockEvent(
    onWhitelist: (event: LockWhitelistedEvent) -> Unit,
    onError: (error: Throwable) -> Unit
  ): Disposable {
    val whitelistDisposable = lockWhitelistedBus.listen()
        .filter { it.packageName == packageName }
        .subscribe { onWhitelist(it) }

    val errorDisposable = bus.listen()
        .flatMapSingle { modifyDatabaseEntry(it) }
        .subscribeOn(Schedulers.trampoline())
        .observeOn(Schedulers.trampoline())
        .doOnError {
          Timber.e(it, "Error occurred modifying database entry")
          onError(it)
        }
        .onErrorReturnItem(Unit)
        .subscribe()

    return object : Disposable {

      override fun isDisposed(): Boolean {
        return whitelistDisposable.isDisposed && errorDisposable.isDisposed
      }

      override fun dispose() {
        whitelistDisposable.tryDispose()
        errorDisposable.tryDispose()
      }

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
        .subscribeOn(Schedulers.io())
        .observeOn(Schedulers.io())
  }

  @CheckResult
  fun populateList(
    force: Boolean,
    onPopulateBegin: (forced: Boolean) -> Unit,
    onPopulateSuccess: (appList: List<ActivityEntry>) -> Unit,
    onPopulateError: (error: Throwable) -> Unit,
    onPopulateComplete: () -> Unit
  ): Disposable {
    return interactor.fetchActivityEntryList(force, packageName)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .doAfterTerminate { onPopulateComplete() }
        .doOnSubscribe { onPopulateBegin(force) }
        .subscribe({ onPopulateSuccess(it) }, {
          Timber.e(it, "LockListPresenter populateList error")
          onPopulateError(it)
        })
  }

}
