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
import com.pyamsoft.pydroid.presenter.SchedulerPresenter;
import io.reactivex.Scheduler;
import javax.inject.Inject;
import javax.inject.Named;
import timber.log.Timber;

class LockInfoItemPresenter extends SchedulerPresenter {

  @NonNull private final LockInfoItemInteractor interactor;

  @Inject LockInfoItemPresenter(@NonNull LockInfoItemInteractor interactor,
      @NonNull @Named("obs") Scheduler observeScheduler,
      @NonNull @Named("sub") Scheduler subscribeScheduler) {
    super(observeScheduler, subscribeScheduler);
    this.interactor = interactor;
  }

  /**
   * public
   */
  void modifyDatabaseEntry(@NonNull LockState oldLockState, @NonNull LockState newLockState,
      @NonNull String packageName, @NonNull String activityName, @Nullable String code,
      boolean system, @NonNull ModifyDatabaseCallback callback) {
    disposeOnStop(
        interactor.modifySingleDatabaseEntry(oldLockState, newLockState, packageName, activityName,
            code, system)
            .subscribeOn(getSubscribeScheduler())
            .observeOn(getObserveScheduler())
            .subscribe(newState -> {
              switch (newState) {
                case DEFAULT:
                  callback.onDatabaseEntryDeleted();
                  break;
                case WHITELISTED:
                  callback.onDatabaseEntryWhitelisted();
                  break;
                case LOCKED:
                  callback.onDatabaseEntryCreated();
                  break;
                default:
                  throw new IllegalStateException("Unsupported lock state: " + newState);
              }
            }, throwable -> {
              Timber.e(throwable, "onError modifyDatabaseEntry");
              callback.onDatabaseEntryError();
            }));
  }

  interface ModifyDatabaseCallback extends LockDatabaseErrorView, LockDatabaseWhitelistView {

  }
}
