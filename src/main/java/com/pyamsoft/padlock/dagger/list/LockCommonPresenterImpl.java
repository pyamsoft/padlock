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

package com.pyamsoft.padlock.dagger.list;

import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.pyamsoft.padlock.model.LockState;
import com.pyamsoft.pydroidrx.SchedulerPresenter;
import rx.Observable;
import rx.Scheduler;
import timber.log.Timber;

abstract class LockCommonPresenterImpl<I> extends SchedulerPresenter<I> {

  @NonNull private final LockCommonInteractor interactor;

  LockCommonPresenterImpl(@NonNull LockCommonInteractor interactor,
      @NonNull Scheduler observeScheduler, @NonNull Scheduler subscribeScheduler) {
    super(observeScheduler, subscribeScheduler);
    this.interactor = interactor;
  }

  @CheckResult @NonNull LockCommonInteractor getInteractor() {
    return interactor;
  }

  @NonNull @CheckResult Observable<LockState> modifySingleDatabaseEntry(boolean notInDatabase,
      @NonNull String packageName, @NonNull String activityName, @Nullable String code,
      boolean system, boolean whitelist, boolean forceLock) {
    if (whitelist) {
      return Observable.defer(() -> {
        final Observable<LockState> lockState;
        if (notInDatabase) {
          Timber.d("Add new as whitelisted");
          lockState = getInteractor().createNewEntry(packageName, activityName, code, system, true);
        } else {
          // Update existing entry
          lockState = Observable.just(LockState.NONE);
        }
        return lockState;
      });
    } else if (forceLock) {
      return Observable.defer(() -> {
        final Observable<LockState> lockState;
        if (notInDatabase) {
          Timber.d("Add new as force locked");
          lockState =
              getInteractor().createNewEntry(packageName, activityName, code, system, false);
        } else {
          // Update existing entry
          lockState = Observable.just(LockState.NONE);
        }
        return lockState;
      });
    } else {
      return Observable.defer(() -> {
        final Observable<LockState> lockState;
        if (notInDatabase) {
          Timber.d("Add new entry");
          lockState = interactor.createNewEntry(packageName, activityName, code, system, false);
        } else {
          Timber.d("Delete existing entry");
          lockState = interactor.deleteEntry(packageName, activityName);
        }
        return lockState;
      });
    }
  }
}
