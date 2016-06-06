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

package com.pyamsoft.padlock.dagger.lock;

import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import com.pyamsoft.padlock.app.lock.LockPresenter;
import com.pyamsoft.pydroid.base.PresenterImpl;
import javax.inject.Named;
import rx.Scheduler;
import rx.Subscription;
import rx.subscriptions.Subscriptions;
import timber.log.Timber;

public abstract class LockPresenterImpl<I extends LockPresenter.LockView> extends PresenterImpl<I>
    implements LockPresenter<I> {

  @NonNull private final IconLoadInteractor lockInteractor;
  @NonNull private final Scheduler mainScheduler;
  @NonNull private final Scheduler ioScheduler;

  @NonNull private Subscription imageSubscription = Subscriptions.empty();

  protected LockPresenterImpl(@NonNull final IconLoadInteractor lockInteractor,
      @NonNull @Named("main") Scheduler mainScheduler,
      @NonNull @Named("io") Scheduler ioScheduler) {
    this.mainScheduler = mainScheduler;
    this.ioScheduler = ioScheduler;
    this.lockInteractor = lockInteractor;
  }

  @CheckResult @NonNull protected final Scheduler getMainScheduler() {
    return mainScheduler;
  }

  @CheckResult @NonNull protected final Scheduler getIoScheduler() {
    return ioScheduler;
  }

  @Override public void onDestroyView() {
    super.onDestroyView();
    unsubImageSubscription();
  }

  @Override public final void loadPackageIcon(final @NonNull String packageName) {
    unsubImageSubscription();
    imageSubscription = lockInteractor.loadPackageIcon(packageName)
        .subscribeOn(ioScheduler)
        .observeOn(mainScheduler)
        .subscribe(drawable -> {
          final LockView lockView = getView();
          if (lockView != null) {
            lockView.setImageSuccess(drawable);
          }
        }, throwable -> {
          Timber.e(throwable, "onError");
          final LockView lockView = getView();
          if (lockView != null) {
            lockView.setImageError();
          }
        });
  }

  private void unsubImageSubscription() {
    if (!imageSubscription.isUnsubscribed()) {
      imageSubscription.unsubscribe();
    }
  }
}
