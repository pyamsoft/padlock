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

package com.pyamsoft.padlock.app.db;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.pyamsoft.padlock.app.base.SchedulerPresenter;
import com.pyamsoft.padlock.dagger.db.DBInteractor;
import javax.inject.Inject;
import javax.inject.Named;
import rx.Observable;
import rx.Scheduler;
import rx.Subscription;
import rx.subscriptions.Subscriptions;
import timber.log.Timber;

public final class DBPresenter extends SchedulerPresenter<DBPresenter.DBView> {

  @NonNull private static final String PACKAGE_TAG = "PACKAGE";
  @NonNull private final DBInteractor dbInteractor;

  @NonNull private Subscription dbPackageSubscription = Subscriptions.empty();
  @NonNull private Subscription dbActivitySubscription = Subscriptions.empty();

  @Inject public DBPresenter(final @NonNull DBInteractor dbInteractor,
      final @NonNull @Named("main") Scheduler mainScheduler,
      final @NonNull @Named("io") Scheduler ioScheduler) {
    super(mainScheduler, ioScheduler);
    this.dbInteractor = dbInteractor;
  }

  @Override protected void onUnbind() {
    super.onUnbind();
    unsubPackageSubscription();
    unsubActivitySubscription();
  }

  private void unsubActivitySubscription() {
    if (!dbActivitySubscription.isUnsubscribed()) {
      dbActivitySubscription.unsubscribe();
    }
  }

  private void unsubPackageSubscription() {
    if (!dbPackageSubscription.isUnsubscribed()) {
      dbPackageSubscription.unsubscribe();
    }
  }

  public final void attemptDBModification(int position, boolean newState,
      @NonNull String packageName, @Nullable String code, boolean system) {
    unsubPackageSubscription();
    dbPackageSubscription = Observable.defer(() -> {
      if (newState) {
        Timber.d("Cursor does not have existing DB data, this is an add call");
        dbInteractor.createEntry(packageName, PACKAGE_TAG, code, system);
      } else {
        Timber.d("Cursor has existing DB data, this is a delete call");
        dbInteractor.deleteEntry(packageName, PACKAGE_TAG);
      }
      return Observable.just(newState);
    }).subscribeOn(getSubscribeScheduler()).observeOn(getObserveScheduler()).subscribe(created -> {
      Timber.d("onNext in DBPresenterImpl with data: ", created);
      final DBView dbView = getView();
      if (created) {
        dbView.onDBCreateEvent(position);
      } else {
        dbView.onDBDeleteEvent(position);
      }
    }, throwable -> {
      Timber.e(throwable, "Error in DBPresenterImpl attemptDBModification");
      final DBView dbView = getView();
      dbView.onDBError();
    });
  }

  public final void attemptDBModification(int position, boolean checked,
      @NonNull String packageName, @NonNull String activity, @Nullable String code,
      boolean system) {
    unsubActivitySubscription();
    dbActivitySubscription = Observable.defer(() -> {
      if (checked) {
        Timber.d("Cursor does not have existing DB data, this is an add call");
        dbInteractor.createEntry(packageName, activity, code, system);
      } else {
        Timber.d("Cursor has existing DB data, this is a delete call");
        dbInteractor.deleteEntry(packageName, activity);
      }
      return Observable.just(checked);
    }).subscribeOn(getSubscribeScheduler()).observeOn(getObserveScheduler()).subscribe(created -> {
      Timber.d("onNext in DBPresenterImpl with data: ", created);
      final DBView dbView = getView();
      if (created) {
        dbView.onDBCreateEvent(position);
      } else {
        dbView.onDBDeleteEvent(position);
      }
    }, throwable -> {
      Timber.e(throwable, "Error in DBPresenterImpl attemptDBModification");
      final DBView dbView = getView();
      dbView.onDBError();
    });
  }

  public interface DBView {

    void onDBCreateEvent(int position);

    void onDBDeleteEvent(int position);

    void onDBError();
  }
}
