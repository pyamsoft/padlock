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

import android.os.Bundle
import androidx.lifecycle.LifecycleOwner
import com.pyamsoft.padlock.purge.PurgeUiComponent.Callback
import com.pyamsoft.pydroid.arch.BaseUiComponent
import com.pyamsoft.pydroid.arch.doOnDestroy
import timber.log.Timber
import javax.inject.Inject

internal class PurgeUiComponentImpl @Inject internal constructor(
  private val purgeSinglePresenter: PurgeSinglePresenter,
  private val listPresenter: PurgeListPresenter,
  private val presenter: PurgePresenter,
  private val purgeView: PurgeListView
) : BaseUiComponent<PurgeUiComponent.Callback>(),
    PurgeUiComponent,
    PurgeSinglePresenter.Callback,
    PurgeListPresenter.Callback,
    PurgePresenter.Callback {

  override fun onBind(
    owner: LifecycleOwner,
    savedInstanceState: Bundle?,
    callback: Callback
  ) {
    owner.doOnDestroy {
      purgeView.teardown()
      presenter.unbind()
      listPresenter.unbind()
      purgeSinglePresenter.unbind()
    }

    purgeView.inflate(savedInstanceState)
    presenter.bind(this)
    listPresenter.bind(this)
    purgeSinglePresenter.bind(this)
  }

  override fun onRefreshRequest(forced: Boolean) {
    refresh(forced)
  }

  override fun refresh(force: Boolean) {
    presenter.fetchData(force)
  }

  override fun saveListPosition() {
    purgeView.storeListPosition()
  }

  override fun saveState(outState: Bundle) {
    purgeView.saveState(outState)
  }

  override fun onSinglePurged(stalePackage: String) {
    Timber.d("Purged stale: $stalePackage")
    presenter.fetchData(true)
  }

  override fun onDeleteRequest(stalePackage: String) {
    callback.showPurgeSingleConfirmation(stalePackage)
  }

  override fun onFetchStaleBegin() {
    purgeView.onStaleFetchBegin()
  }

  override fun onFetchStaleSuccess(data: List<String>) {
    purgeView.onStaleFetchSuccess(data)
  }

  override fun onFetchStaleError(throwable: Throwable) {
    purgeView.onStaleFetchError { presenter.fetchData(true) }
  }

  override fun onFetchStaleComplete() {
    purgeView.onStaleFetchComplete()
  }

  override fun onPurgeSingleError(throwable: Throwable) {
    purgeView.showErrorMessage(DEFAULT_ERROR)
  }

  override fun showError(throwable: Throwable) {
    purgeView.showErrorMessage(throwable.message ?: DEFAULT_ERROR)
  }

  companion object {

    private const val DEFAULT_ERROR = "Unable to purge entry, please try again later."
  }

}