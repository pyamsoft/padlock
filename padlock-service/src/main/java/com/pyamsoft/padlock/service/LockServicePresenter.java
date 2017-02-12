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

package com.pyamsoft.padlock.service;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.pyamsoft.padlock.model.sql.PadLockEntry;
import com.pyamsoft.pydroid.helper.SubscriptionHelper;
import com.pyamsoft.pydroid.presenter.Presenter;
import com.pyamsoft.pydroid.presenter.SchedulerPresenter;
import javax.inject.Inject;
import rx.Scheduler;
import rx.Subscription;
import rx.subscriptions.Subscriptions;
import timber.log.Timber;

class LockServicePresenter extends SchedulerPresenter<Presenter.Empty> {

  @SuppressWarnings("WeakerAccess") @NonNull final LockServiceInteractor interactor;
  @SuppressWarnings("WeakerAccess") @NonNull Subscription lockedEntrySubscription =
      Subscriptions.empty();
  @Nullable @SuppressWarnings("WeakerAccess") Subscription recheckSubscription;

  @Inject LockServicePresenter(@NonNull LockServiceInteractor interactor,
      @NonNull Scheduler obsScheduler, @NonNull Scheduler subScheduler) {
    super(obsScheduler, subScheduler);
    this.interactor = interactor;
    interactor.reset();
  }

  @Override protected void onUnbind() {
    super.onUnbind();
    SubscriptionHelper.unsubscribe(lockedEntrySubscription, recheckSubscription);
    interactor.cleanup();
  }

  public void processActiveApplicationIfMatching(@NonNull String packageName,
      @NonNull String className, @NonNull ProcessCallback callback) {
    SubscriptionHelper.unsubscribe(recheckSubscription);
    recheckSubscription = interactor.processActiveIfMatching(packageName, className)
        .subscribeOn(getSubscribeScheduler())
        .observeOn(getObserveScheduler())
        .subscribe(recheck -> {
              if (recheck) {
                processAccessibilityEvent(packageName, className,
                    RecheckStatus.FORCE, callback);
              }
            }, throwable -> Timber.e(throwable, "onError processActiveApplicationIfMatching"),
            () -> SubscriptionHelper.unsubscribe(recheckSubscription));
  }

  public void processAccessibilityEvent(@NonNull String packageName, @NonNull String className,
      @NonNull RecheckStatus forcedRecheck,
      @NonNull ProcessCallback callback) {
    SubscriptionHelper.unsubscribe(lockedEntrySubscription);
    lockedEntrySubscription = interactor.processEvent(packageName, className, forcedRecheck)
        .subscribeOn(getSubscribeScheduler())
        .observeOn(getObserveScheduler())
        .subscribe(padLockEntry -> {
              Timber.d("Got PadLockEntry for LockScreen: %s %s", padLockEntry.packageName(),
                  padLockEntry.activityName());
              callback.startLockScreen(padLockEntry, className);
            }, throwable -> Timber.e(throwable, "Error getting PadLockEntry for LockScreen"),
            () -> SubscriptionHelper.unsubscribe(lockedEntrySubscription));
  }

  public void setLockScreenPassed() {
    interactor.setLockScreenPassed(true);
  }

  interface ProcessCallback {

    void startLockScreen(@NonNull PadLockEntry entry, @NonNull String realName);
  }
}
