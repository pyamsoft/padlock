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
import com.pyamsoft.padlock.app.db.DBInteractor;
import com.pyamsoft.padlock.app.db.DBPresenter;
import com.pyamsoft.padlock.app.db.DBView;
import com.pyamsoft.pydroid.base.PresenterImpl;
import javax.inject.Inject;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subscriptions.Subscriptions;
import timber.log.Timber;

class DBPresenterImpl extends PresenterImpl<DBView> implements DBPresenter {

  @NonNull private final DBInteractor dbInteractor;

  @NonNull private Subscription dbPackageSubscription = Subscriptions.empty();
  @NonNull private Subscription dbActivitySubscription = Subscriptions.empty();

  @Inject public DBPresenterImpl(final @NonNull DBInteractor dbInteractor) {
    this.dbInteractor = dbInteractor;
  }

  @Override public void stop() {
    super.stop();
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

  @NonNull private Observable<Boolean> packageModificationObservable(boolean checked,
      @NonNull String packageName, @NonNull String name, @NonNull String code, boolean system)
      throws NullPointerException {
    return Observable.defer(() -> {
      if (checked) {
        Timber.d("Cursor does not have existing DB data, this is an add call");
        dbInteractor.createEntry(packageName, name, code, system);
      } else {
        Timber.d("Cursor has existing DB data, this is a delete call");
        dbInteractor.deleteEntry(packageName);
      }
      return Observable.just(checked);
    }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
  }

  @Override
  public void attemptDBModification(int position, boolean newState, String packageName, String name,
      String code, boolean system) throws NullPointerException {
    unsubPackageSubscription();
    dbPackageSubscription =
        packageModificationObservable(newState, packageName, name, code, system).subscribe(
            created -> {
              Timber.d("onNext in DBPresenterImpl with data: ", created);
              if (created) {
                get().onDBCreateEvent(position);
              } else {
                get().onDBDeleteEvent(position);
              }
            }, throwable -> {
              Timber.e(throwable, "Error in DBPresenterImpl attemptDBModification");
              get().onDBError();
            });
  }

  @NonNull
  private Observable<Boolean> activityModificationObservable(boolean checked, String packageName,
      String activityName, String name, String code, boolean system) throws NullPointerException {
    return Observable.defer(() -> {
      if (checked) {
        Timber.d("Cursor does not have existing DB data, this is an add call");
        dbInteractor.createEntry(packageName, activityName, name, code, system);
      } else {
        Timber.d("Cursor has existing DB data, this is a delete call");
        dbInteractor.deleteEntry(packageName, activityName);
      }
      return Observable.just(checked);
    }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
  }

  @Override public void attemptDBModification(int position, boolean checked, String packageName,
      String activity, String name, String code, boolean system) throws NullPointerException {
    unsubActivitySubscription();
    dbActivitySubscription =
        activityModificationObservable(checked, packageName, activity, name, code,
            system).subscribe(created -> {
          Timber.d("onNext in DBPresenterImpl with data: ", created);
          if (created) {
            get().onDBCreateEvent(position);
          } else {
            get().onDBDeleteEvent(position);
          }
        }, throwable -> {
          Timber.e(throwable, "Error in DBPresenterImpl attemptDBModification");
          get().onDBError();
        });
  }
}
