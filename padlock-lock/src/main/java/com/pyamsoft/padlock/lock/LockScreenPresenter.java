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

package com.pyamsoft.padlock.lock;

import android.support.annotation.NonNull;
import com.pyamsoft.padlock.lock.common.LockTypePresenter;
import com.pyamsoft.pydroid.helper.DisposableHelper;
import io.reactivex.Scheduler;
import io.reactivex.disposables.Disposable;
import io.reactivex.disposables.Disposables;
import javax.inject.Inject;
import javax.inject.Named;
import timber.log.Timber;

class LockScreenPresenter extends LockTypePresenter {

  @NonNull private final LockScreenInteractor interactor;
  @NonNull private Disposable displayNameDisposable = Disposables.empty();
  @NonNull private Disposable ignoreTimeDisposable = Disposables.empty();

  @Inject LockScreenPresenter(@NonNull LockScreenInteractor lockScreenInteractor,
      @NonNull @Named("obs") Scheduler obsScheduler, @NonNull @Named("io") Scheduler subScheduler) {
    super(lockScreenInteractor, obsScheduler, subScheduler);
    this.interactor = lockScreenInteractor;
  }

  @Override protected void onStop() {
    super.onStop();
    displayNameDisposable = DisposableHelper.dispose(displayNameDisposable);
    ignoreTimeDisposable = DisposableHelper.dispose(ignoreTimeDisposable);
  }

  public void createWithDefaultIgnoreTime(@NonNull IgnoreTimeCallback callback) {
    ignoreTimeDisposable = DisposableHelper.dispose(ignoreTimeDisposable);
    ignoreTimeDisposable = interactor.getDefaultIgnoreTime()
        .subscribeOn(getSubscribeScheduler())
        .observeOn(getObserveScheduler())
        .subscribe(callback::onInitializeWithIgnoreTime,
            throwable -> Timber.e(throwable, "onError createWithDefaultIgnoreTime"));
  }

  public void loadDisplayNameFromPackage(@NonNull String packageName,
      @NonNull DisplayNameLoadCallback callback) {
    displayNameDisposable = DisposableHelper.dispose(displayNameDisposable);
    displayNameDisposable = interactor.getDisplayName(packageName)
        .subscribeOn(getSubscribeScheduler())
        .observeOn(getObserveScheduler())
        .subscribe(callback::setDisplayName, throwable -> {
          Timber.e(throwable, "Error loading display name from package");
          callback.setDisplayName("");
        });
  }

  interface LockErrorCallback {

    void onLockedError();
  }

  interface DisplayNameLoadCallback {

    void setDisplayName(@NonNull String name);
  }

  interface IgnoreTimeCallback {
    void onInitializeWithIgnoreTime(long time);
  }
}
