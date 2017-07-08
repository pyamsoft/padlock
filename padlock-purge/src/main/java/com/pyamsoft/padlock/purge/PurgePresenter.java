/*
 * Copyright 2017 Peter Kenji Yamanaka
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
 */

package com.pyamsoft.padlock.purge;

import android.support.annotation.NonNull;
import com.pyamsoft.pydroid.bus.EventBus;
import com.pyamsoft.pydroid.presenter.SchedulerPresenter;
import io.reactivex.Scheduler;
import javax.inject.Inject;
import javax.inject.Named;
import timber.log.Timber;

class PurgePresenter extends SchedulerPresenter {

  @NonNull private final PurgeInteractor interactor;

  @Inject PurgePresenter(@NonNull PurgeInteractor interactor, @Named("obs") Scheduler obsScheduler,
      @Named("sub") Scheduler subScheduler) {
    super(obsScheduler, subScheduler);
    this.interactor = interactor;
  }

  /**
   * public
   */
  void registerOnBus(@NonNull PurgeCallback callback) {
    //disposeOnStop(EventBus.get()
    //    .listen(PurgeEvent.class)
    //    .subscribeOn(getBackgroundScheduler())
    //    .observeOn(getForegroundScheduler())
    //    .subscribe(purgeEvent -> callback.purge(purgeEvent.packageName()),
    //        throwable -> Timber.e(throwable, "onError purge single")));
    //
    //disposeOnStop(EventBus.get()
    //    .listen(PurgeAllEvent.class)
    //    .subscribeOn(getBackgroundScheduler())
    //    .observeOn(getForegroundScheduler())
    //    .subscribe(purgeAllEvent -> callback.purgeAll(),
    //        throwable -> Timber.e(throwable, "onError purge all")));
  }

  /**
   * public
   */
  void retrieveStaleApplications(@NonNull RetrievalCallback callback, boolean forceRefresh) {
    disposeOnStop(interactor.populateList(forceRefresh)
        .subscribeOn(getBackgroundScheduler())
        .observeOn(getForegroundScheduler())
        .doAfterTerminate(callback::onRetrievalComplete)
        .subscribe(callback::onStaleApplicationRetrieved,
            throwable -> Timber.e(throwable, "onError retrieveStaleApplications")));
  }

  /**
   * public
   */
  void deleteStale(@NonNull String packageName, @NonNull DeleteCallback callback) {
    disposeOnStop(interactor.deleteEntry(packageName)
        .subscribeOn(getBackgroundScheduler())
        .observeOn(getForegroundScheduler())
        .subscribe(() -> callback.onDeleted(packageName),
            throwable -> Timber.e(throwable, "onError deleteStale")));
  }

  interface PurgeCallback {

    void purge(@NonNull String packageName);

    void purgeAll();
  }

  interface RetrievalCallback {

    void onStaleApplicationRetrieved(@NonNull String name);

    void onRetrievalComplete();
  }

  interface DeleteCallback {

    void onDeleted(@NonNull String packageName);
  }
}
