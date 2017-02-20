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

package com.pyamsoft.padlock.lock.common;

import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import com.pyamsoft.pydroid.helper.SubscriptionHelper;
import com.pyamsoft.pydroid.presenter.Presenter;
import com.pyamsoft.pydroid.presenter.SchedulerPresenter;
import javax.inject.Inject;
import rx.Scheduler;
import rx.Subscription;
import rx.subscriptions.Subscriptions;
import timber.log.Timber;

public class LockTypePresenter extends SchedulerPresenter<Presenter.Empty> {

  @NonNull private final LockTypeInteractor interactor;
  @NonNull private Subscription typeSubscription = Subscriptions.empty();

  @Inject protected LockTypePresenter(@NonNull LockTypeInteractor interactor,
      @NonNull Scheduler observeScheduler, @NonNull Scheduler subscribeScheduler) {
    super(observeScheduler, subscribeScheduler);
    this.interactor = interactor;
  }

  @CallSuper @Override protected void onUnbind() {
    super.onUnbind();
    typeSubscription = SubscriptionHelper.unsubscribe(typeSubscription);
  }

  public void initializeLockScreenType(@NonNull LockScreenTypeCallback callback) {
    typeSubscription = SubscriptionHelper.unsubscribe(typeSubscription);
    typeSubscription = interactor.getLockScreenType()
        .subscribeOn(getSubscribeScheduler())
        .observeOn(getObserveScheduler())
        .subscribe(lockScreenType -> {
          switch (lockScreenType) {
            case TYPE_PATTERN:
              callback.onTypePattern();
              break;
            case TYPE_TEXT:
              callback.onTypeText();
              break;
            default:
              throw new IllegalStateException("Invalid lock screen type: " + lockScreenType);
          }
        }, throwable -> {
          Timber.e(throwable, "onError");
        });
  }

  public interface LockScreenTypeCallback {
    void onTypeText();

    void onTypePattern();
  }
}
