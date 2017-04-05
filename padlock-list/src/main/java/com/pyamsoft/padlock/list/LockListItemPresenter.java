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

package com.pyamsoft.padlock.list;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.pyamsoft.padlock.model.LockState;
import com.pyamsoft.padlock.base.db.PadLockEntry;
import com.pyamsoft.pydroid.helper.DisposableHelper;
import com.pyamsoft.pydroid.presenter.SchedulerPresenter;
import io.reactivex.Scheduler;
import io.reactivex.disposables.Disposable;
import io.reactivex.disposables.Disposables;
import javax.inject.Inject;
import javax.inject.Named;
import timber.log.Timber;

class LockListItemPresenter extends SchedulerPresenter {

  @NonNull private final LockListItemInteractor interactor;
  @NonNull private Disposable databaseDisposable = Disposables.empty();

  @Inject LockListItemPresenter(@NonNull LockListItemInteractor interactor,
      @NonNull @Named("obs") Scheduler obsScheduler, @NonNull @Named("io") Scheduler subScheduler) {
    super(obsScheduler, subScheduler);
    this.interactor = interactor;
  }

  @Override protected void onStop() {
    super.onStop();
    databaseDisposable = DisposableHelper.dispose(databaseDisposable);
  }

  /**
   * public
   */
  void modifyDatabaseEntry(boolean isChecked, @NonNull String packageName,
      @Nullable String code, boolean system, @NonNull DatabaseCallback callback) {
    // No whitelisting for modifications from the List
    LockState oldState = (isChecked ? LockState.DEFAULT : LockState.LOCKED);
    LockState newState = (isChecked ? LockState.LOCKED : LockState.DEFAULT);

    databaseDisposable = DisposableHelper.dispose(databaseDisposable);
    databaseDisposable = interactor.modifySingleDatabaseEntry(oldState, newState, packageName,
        PadLockEntry.PACKAGE_ACTIVITY_NAME, code, system)
        .subscribeOn(getSubscribeScheduler())
        .observeOn(getObserveScheduler())
        .subscribe(lockState -> {
          switch (lockState) {
            case DEFAULT:
              callback.onDatabaseEntryDeleted();
              break;
            case LOCKED:
              callback.onDatabaseEntryCreated();
              break;
            default:
              throw new RuntimeException("Whitelist results are not handled");
          }
        }, throwable -> {
          Timber.e(throwable, "onError modifyDatabaseEntry");
          callback.onDatabaseEntryError();
        });
  }

  interface DatabaseCallback extends LockDatabaseErrorView {
  }
}
