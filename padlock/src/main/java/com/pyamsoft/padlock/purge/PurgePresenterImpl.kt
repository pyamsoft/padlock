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

package com.pyamsoft.padlock.purge

import com.pyamsoft.padlock.R.id
import com.pyamsoft.padlock.api.PurgeInteractor
import com.pyamsoft.padlock.scopes.FragmentScope
import com.pyamsoft.pydroid.core.bus.RxBus
import com.pyamsoft.pydroid.core.singleDisposable
import com.pyamsoft.pydroid.core.tryDispose
import com.pyamsoft.pydroid.ui.arch.BasePresenter
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject

@FragmentScope
internal class PurgePresenterImpl @Inject internal constructor(
  private val interactor: PurgeInteractor
) : BasePresenter<Unit, PurgePresenter.Callback>(RxBus.empty()),
    PurgeListView.Callback,
    PurgePresenter {

  private var fetchDisposable by singleDisposable()

  override fun onBind() {
  }

  override fun onUnbind() {
    fetchDisposable.tryDispose()
  }

  override fun onRefresh(forced: Boolean) {
    fetchData(true)
  }

  override fun onMenuItemClicked(itemId: Int) {
    if (itemId == id.menu_purge_all) {
      callback.onDeleteAllRequest()
    }
  }

  override fun onListItemClicked(stalePackage: String) {
    callback.onDeleteRequest(stalePackage)
  }

  override fun fetchData(forced: Boolean) {
    fetchDisposable = interactor.fetchStalePackageNames(forced)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .doAfterTerminate { callback.onFetchStaleComplete() }
        .doOnSubscribe { callback.onFetchStaleBegin() }
        .subscribe({ callback.onFetchStaleSuccess(it) }, {
          Timber.e(it, "Error fetching stale applications")
          callback.onFetchStaleError(it)
        })
  }
}