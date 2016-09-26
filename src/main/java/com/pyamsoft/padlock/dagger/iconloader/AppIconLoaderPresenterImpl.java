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

package com.pyamsoft.padlock.dagger.iconloader;

import android.support.annotation.NonNull;
import com.pyamsoft.padlock.app.iconloader.AppIconLoaderPresenter;
import com.pyamsoft.padlock.app.iconloader.AppIconLoaderView;
import com.pyamsoft.pydroid.presenter.SchedulerPresenter;
import javax.inject.Inject;
import javax.inject.Named;
import rx.Scheduler;
import rx.Subscription;
import rx.subscriptions.Subscriptions;
import timber.log.Timber;

class AppIconLoaderPresenterImpl<I extends AppIconLoaderView> extends SchedulerPresenter<I>
    implements AppIconLoaderPresenter<I> {

  @NonNull private final AppIconLoaderInteractor interactor;
  @NonNull private Subscription loadIconSubscription = Subscriptions.empty();

  @Inject AppIconLoaderPresenterImpl(@NonNull AppIconLoaderInteractor interactor,
      @NonNull @Named("obs") Scheduler obsScheduler,
      @NonNull @Named("io") Scheduler subScheduler) {
    super(obsScheduler, subScheduler);
    this.interactor = interactor;
  }

  @Override protected void onUnbind() {
    super.onUnbind();
    unsubLoadIcon();
  }

  @Override public void loadApplicationIcon(@NonNull String packageName) {
    unsubLoadIcon();
    loadIconSubscription = interactor.loadPackageIcon(packageName)
        .subscribeOn(getSubscribeScheduler())
        .observeOn(getObserveScheduler())
        .subscribe(drawable -> getView(i -> i.onApplicationIconLoadedSuccess(drawable)),
            throwable -> {
              Timber.e(throwable, "onError");
              getView(I::onApplicationIconLoadedError);
            }, this::unsubLoadIcon);
  }

  @SuppressWarnings("WeakerAccess") void unsubLoadIcon() {
    if (!loadIconSubscription.isUnsubscribed()) {
      loadIconSubscription.unsubscribe();
    }
  }
}
