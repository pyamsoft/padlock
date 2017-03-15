/*
 * Copyright 2016 Peter Kenji Yamanaka
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
import com.pyamsoft.padlock.model.event.PurgeAllEvent;
import com.pyamsoft.padlock.model.event.PurgeEvent;
import com.pyamsoft.pydroid.bus.EventBus;
import com.pyamsoft.pydroid.helper.DisposableHelper;
import com.pyamsoft.pydroid.presenter.Presenter;
import com.pyamsoft.pydroid.presenter.SchedulerPresenter;
import io.reactivex.Scheduler;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.disposables.Disposables;
import javax.inject.Inject;
import javax.inject.Named;
import timber.log.Timber;

class PurgePresenter extends SchedulerPresenter<Presenter.Empty> {

  @NonNull private final PurgeInteractor interactor;
  @NonNull private final CompositeDisposable compositeDisposable;
  @NonNull private Disposable retrievalDisposable = Disposables.empty();
  @NonNull private Disposable purgeBus = Disposables.empty();
  @NonNull private Disposable purgeAllBus = Disposables.empty();

  @Inject PurgePresenter(@NonNull PurgeInteractor interactor, @Named("obs") Scheduler obsScheduler,
      @Named("sub") Scheduler subScheduler) {
    super(obsScheduler, subScheduler);
    this.interactor = interactor;
    compositeDisposable = new CompositeDisposable();
  }

  @Override protected void onUnbind() {
    super.onUnbind();
    compositeDisposable.clear();
    retrievalDisposable = DisposableHelper.dispose(retrievalDisposable);
    purgeBus = DisposableHelper.dispose(purgeBus);
    purgeAllBus = DisposableHelper.dispose(purgeAllBus);
  }

  public void registerOnBus(@NonNull PurgeCallback callback) {
    purgeBus = DisposableHelper.dispose(purgeBus);
    purgeBus = EventBus.get()
        .listen(PurgeEvent.class)
        .subscribeOn(getSubscribeScheduler())
        .observeOn(getObserveScheduler())
        .subscribe(purgeEvent -> callback.purge(purgeEvent.packageName()),
            throwable -> Timber.e(throwable, "onError purge single"));

    purgeAllBus = DisposableHelper.dispose(purgeAllBus);
    purgeAllBus = EventBus.get()
        .listen(PurgeAllEvent.class)
        .subscribeOn(getSubscribeScheduler())
        .observeOn(getObserveScheduler())
        .subscribe(purgeAllEvent -> callback.purgeAll(),
            throwable -> Timber.e(throwable, "onError purge all"));
  }

  public void retrieveStaleApplications(@NonNull RetrievalCallback callback, boolean forceRefresh) {
    retrievalDisposable = DisposableHelper.dispose(retrievalDisposable);
    retrievalDisposable = interactor.populateList(forceRefresh)
        .subscribeOn(getSubscribeScheduler())
        .observeOn(getObserveScheduler())
        .doAfterTerminate(callback::onRetrievalComplete)
        .subscribe(callback::onStaleApplicationRetrieved,
            throwable -> Timber.e(throwable, "onError retrieveStaleApplications"));
  }

  public void deleteStale(@NonNull String packageName, @NonNull DeleteCallback callback) {
    final Disposable subscription = interactor.deleteEntry(packageName)
        .subscribeOn(getSubscribeScheduler())
        .observeOn(getObserveScheduler())
        .subscribe(deleteResult -> {
          Timber.d("Delete result :%d", deleteResult);
          if (deleteResult > 0) {
            callback.onDeleted(packageName);
          }
        }, throwable -> {
          Timber.e(throwable, "onError deleteStale");
        });
    compositeDisposable.add(subscription);
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
