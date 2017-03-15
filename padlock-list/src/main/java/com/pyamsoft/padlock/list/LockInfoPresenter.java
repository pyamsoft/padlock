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
import com.pyamsoft.padlock.model.ActivityEntry;
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

class LockInfoPresenter extends SchedulerPresenter<Presenter.Empty> {

  @NonNull private final LockInfoInteractor lockInfoInteractor;
  @NonNull private final CompositeDisposable compositeDisposable;
  @NonNull private Disposable populateListDisposable = Disposables.empty();
  @NonNull private Disposable onboardDisposable = Disposables.empty();

  @Inject LockInfoPresenter(final @NonNull LockInfoInteractor lockInfoInteractor,
      final @NonNull @Named("obs") Scheduler obsScheduler,
      final @NonNull @Named("io") Scheduler subScheduler) {
    super(obsScheduler, subScheduler);
    this.lockInfoInteractor = lockInfoInteractor;
    compositeDisposable = new CompositeDisposable();
  }

  @Override protected void onUnbind() {
    super.onUnbind();
    onboardDisposable = DisposableHelper.dispose(onboardDisposable);
    populateListDisposable = DisposableHelper.dispose(populateListDisposable);
  }

  public void populateList(@NonNull String packageName, @NonNull PopulateListCallback callback,
      boolean forceRefresh) {
    populateListDisposable = DisposableHelper.dispose(populateListDisposable);
    populateListDisposable = lockInfoInteractor.populateList(packageName, forceRefresh)
        .subscribeOn(getSubscribeScheduler())
        .observeOn(getObserveScheduler())
        .doAfterTerminate(callback::onListPopulated)
        .subscribe(callback::onEntryAddedToList, throwable -> {
          Timber.e(throwable, "LockInfoPresenterImpl populateList onError");
          callback.onListPopulateError();
        });
  }

  public void modifyDatabaseEntry(boolean isNotDefault, int position, @NonNull String packageName,
      @NonNull String activityName, @SuppressWarnings("SameParameterValue") @Nullable String code,
      boolean system, boolean whitelist, boolean forceDelete,
      @NonNull ModifyDatabaseCallback callback) {
    Disposable databaseDisposable =
        lockInfoInteractor.modifySingleDatabaseEntry(isNotDefault, packageName, activityName, code,
            system, whitelist, forceDelete)
            .subscribeOn(getSubscribeScheduler())
            .observeOn(getObserveScheduler())
            .subscribe(lockState -> {
              switch (lockState) {
                case DEFAULT:
                  callback.onDatabaseEntryDeleted(position);
                  break;
                case WHITELISTED:
                  callback.onDatabaseEntryWhitelisted(position);
                  break;
                case LOCKED:
                  callback.onDatabaseEntryCreated(position);
                  break;
                default:
                  throw new IllegalStateException("Unsupported lock state: " + lockState);
              }
            }, throwable -> {
              Timber.e(throwable, "onError modifyDatabaseEntry");
              callback.onDatabaseEntryError(position);
            });
    compositeDisposable.add(databaseDisposable);
  }

  public void showOnBoarding(@NonNull OnBoardingCallback callback) {
    onboardDisposable = DisposableHelper.dispose(onboardDisposable);
    onboardDisposable = lockInfoInteractor.hasShownOnBoarding()
        .subscribeOn(getSubscribeScheduler())
        .observeOn(getObserveScheduler())
        .subscribe(onboard -> {
          if (onboard) {
            callback.onOnboardingComplete();
          } else {
            callback.onShowOnboarding();
          }
        }, throwable -> {
          Timber.e(throwable, "onError");
        });
  }

  public interface ModifyDatabaseCallback extends LockDatabaseErrorView, LockDatabaseWhitelistView {

  }

  public interface OnBoardingCallback {

    void onShowOnboarding();

    void onOnboardingComplete();
  }

  public interface PopulateListCallback extends LockCommon {

    void onEntryAddedToList(@NonNull ActivityEntry entry);
  }
}
