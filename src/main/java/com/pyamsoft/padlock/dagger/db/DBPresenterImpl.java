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

package com.pyamsoft.padlock.dagger.db;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.pyamsoft.padlock.model.sql.PadLockEntry;
import com.pyamsoft.pydroid.base.presenter.SchedulerPresenter;
import javax.inject.Inject;
import javax.inject.Named;
import rx.Observable;
import rx.Scheduler;
import rx.Subscription;
import rx.subscriptions.Subscriptions;
import timber.log.Timber;

final class DBPresenterImpl extends SchedulerPresenter<DBPresenter.DBView>
    implements DBPresenter {

  @NonNull final DBInteractor dbInteractor;
  @NonNull Subscription dbAllSubscription = Subscriptions.empty();
  @NonNull Subscription dbPackageSubscription = Subscriptions.empty();
  @NonNull Subscription dbActivitySubscription = Subscriptions.empty();

  @Inject DBPresenterImpl(final @NonNull DBInteractor dbInteractor,
      final @NonNull @Named("main") Scheduler mainScheduler,
      final @NonNull @Named("io") Scheduler ioScheduler) {
    super(mainScheduler, ioScheduler);
    this.dbInteractor = dbInteractor;
  }

  @Override protected void onUnbind(@NonNull DBView view) {
    super.onUnbind(view);
    unsubPackageSubscription();
    unsubActivitySubscription();
    unsubAllSubscription();
  }

  void unsubAllSubscription() {
    if (!dbAllSubscription.isUnsubscribed()) {
      dbAllSubscription.unsubscribe();
    }
  }

  void unsubActivitySubscription() {
    if (!dbActivitySubscription.isUnsubscribed()) {
      dbActivitySubscription.unsubscribe();
    }
  }

  void unsubPackageSubscription() {
    if (!dbPackageSubscription.isUnsubscribed()) {
      dbPackageSubscription.unsubscribe();
    }
  }

  @Override public void attemptDBAllModification(boolean create, @NonNull String packageName,
      @Nullable String code, boolean system) {
    unsubAllSubscription();
    dbAllSubscription = Observable.defer(() -> {
      if (create) {
        Timber.d("Cursor does not have existing DB data, this is an add call");
        return dbInteractor.createActivityEntries(packageName, code, system, false);
      } else {
        Timber.d("Cursor has existing DB data, this is a delete call");
        return dbInteractor.deleteActivityEntries(packageName).map(Integer::longValue);
      }
    }).map(aLong -> {
      // TODO do something with result
      return create;
    }).subscribeOn(getSubscribeScheduler()).observeOn(getObserveScheduler()).subscribe(created -> {
      Timber.d("onNext in DBPresenterImpl with data: %s", created);
      // KLUDGE do nothing for onnext
    }, throwable -> {
      Timber.e(throwable, "Error in DBPresenterImpl attemptDBModification");
      final DBView dbView = getView();
      dbView.onDBError();
    }, () -> {
      Timber.d("onComplete in DBPresenterImpl with data: %s", create);
      final DBView dbView = getView();
      final int position = -1;
      if (create) {
        dbView.onDBCreateEvent(position);
      } else {
        dbView.onDBDeleteEvent(position);
      }
      unsubAllSubscription();
    });
  }

  @Override
  public void attemptDBModification(int position, boolean create, @NonNull String packageName,
      @Nullable String code, boolean system) {
    unsubPackageSubscription();
    dbPackageSubscription = Observable.defer(() -> {
      if (create) {
        Timber.d("Cursor does not have existing DB data, this is an add call");
        return dbInteractor.createEntry(packageName, PadLockEntry.PACKAGE_TAG, code, system, false);
      } else {
        Timber.d("Cursor has existing DB data, this is a delete call");
        return dbInteractor.deleteEntry(packageName, PadLockEntry.PACKAGE_TAG)
            .map(Integer::longValue);
      }
    }).map(aLong -> {
      // TODO do something with result
      return create;
    }).subscribeOn(getSubscribeScheduler()).observeOn(getObserveScheduler()).subscribe(created -> {
      Timber.d("onNext in DBPresenterImpl with data: %s", created);
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
    }, this::unsubPackageSubscription);
  }

  @Override
  public void attemptDBModification(int position, boolean create, @NonNull String packageName,
      @NonNull String activity, @Nullable String code, boolean system) {
    unsubActivitySubscription();
    dbActivitySubscription = Observable.defer(() -> {
      if (create) {
        Timber.d("Cursor does not have existing DB data, this is an add call");
        return dbInteractor.createEntry(packageName, activity, code, system, false);
      } else {
        Timber.d("Cursor has existing DB data, this is a delete call");
        return dbInteractor.deleteEntry(packageName, activity).map(Integer::longValue);
      }
    }).map(aLong -> {
      // TODO do something with result
      return create;
    }).subscribeOn(getSubscribeScheduler()).observeOn(getObserveScheduler()).subscribe(created -> {
      Timber.d("onNext in DBPresenterImpl with data: %s", created);
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
    }, this::unsubActivitySubscription);
  }
}
