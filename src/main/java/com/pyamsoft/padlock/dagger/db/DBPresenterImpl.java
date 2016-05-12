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
import com.pyamsoft.padlock.app.db.DBPresenter;
import com.pyamsoft.pydroid.base.PresenterImpl;
import javax.inject.Inject;
import javax.inject.Named;
import rx.Observable;
import rx.Scheduler;
import rx.Subscription;
import rx.subscriptions.Subscriptions;
import timber.log.Timber;

final class DBPresenterImpl extends PresenterImpl<DBPresenter.DBView> implements DBPresenter {

  @NonNull private final DBInteractor dbInteractor;
  @NonNull private final Scheduler mainScheduler;
  @NonNull private final Scheduler ioScheduler;

  @NonNull private Subscription dbPackageSubscription = Subscriptions.empty();
  @NonNull private Subscription dbActivitySubscription = Subscriptions.empty();

  @Inject public DBPresenterImpl(final @NonNull DBInteractor dbInteractor,
      final @NonNull @Named("main") Scheduler mainScheduler,
      final @NonNull @Named("io") Scheduler ioScheduler) {
    this.dbInteractor = dbInteractor;
    this.mainScheduler = mainScheduler;
    this.ioScheduler = ioScheduler;
  }

  @Override public void onDestroyView() {
    super.onDestroyView();
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

  @Override
  public void attemptDBModification(int position, boolean newState, String packageName, String code,
      boolean system) throws NullPointerException {
    unsubPackageSubscription();
    dbPackageSubscription = Observable.defer(() -> {
      if (newState) {
        Timber.d("Cursor does not have existing DB data, this is an add call");
        dbInteractor.createEntry(packageName, code, system);
      } else {
        Timber.d("Cursor has existing DB data, this is a delete call");
        dbInteractor.deleteEntry(packageName);
      }
      return Observable.just(newState);
    }).subscribeOn(ioScheduler).observeOn(mainScheduler).subscribe(created -> {
      Timber.d("onNext in DBPresenterImpl with data: ", created);
      final DBView dbView = getView();
      if (dbView != null) {
        if (created) {
          dbView.onDBCreateEvent(position);
        } else {
          dbView.onDBDeleteEvent(position);
        }
      }
    }, throwable -> {
      Timber.e(throwable, "Error in DBPresenterImpl attemptDBModification");
      final DBView dbView = getView();
      if (dbView != null) {
        dbView.onDBError();
      }
    });
  }

  @Override public void attemptDBModification(int position, boolean checked, String packageName,
      String activity, String code, boolean system) throws NullPointerException {
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
    }).subscribeOn(ioScheduler).observeOn(mainScheduler).subscribe(created -> {
      Timber.d("onNext in DBPresenterImpl with data: ", created);
      final DBView dbView = getView();
      if (dbView != null) {
        if (created) {
          dbView.onDBCreateEvent(position);
        } else {
          dbView.onDBDeleteEvent(position);
        }
      }
    }, throwable -> {
      Timber.e(throwable, "Error in DBPresenterImpl attemptDBModification");
      final DBView dbView = getView();
      if (dbView != null) {
        dbView.onDBError();
      }
    });
  }
}
