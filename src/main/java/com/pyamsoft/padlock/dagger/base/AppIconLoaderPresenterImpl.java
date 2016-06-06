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

package com.pyamsoft.padlock.dagger.base;

import android.support.annotation.NonNull;
import com.pyamsoft.padlock.app.base.AppIconLoaderPresenter;
import com.pyamsoft.padlock.app.base.AppIconLoaderView;
import com.pyamsoft.pydroid.base.PresenterImpl;
import javax.inject.Named;
import rx.Scheduler;
import rx.Subscription;
import rx.subscriptions.Subscriptions;
import timber.log.Timber;

public abstract class AppIconLoaderPresenterImpl<I extends AppIconLoaderView>
    extends PresenterImpl<I> implements AppIconLoaderPresenter<I> {

  @NonNull private final AppIconLoaderInteractor interactor;
  @NonNull private final Scheduler mainScheduler;
  @NonNull private final Scheduler ioScheduler;

  @NonNull private Subscription loadIconSubscription = Subscriptions.empty();

  protected AppIconLoaderPresenterImpl(@NonNull AppIconLoaderInteractor interactor,
      @NonNull @Named("main") Scheduler mainScheduler,
      @NonNull @Named("io") Scheduler ioScheduler) {
    this.interactor = interactor;
    this.mainScheduler = mainScheduler;
    this.ioScheduler = ioScheduler;
  }

  @Override public void onDestroyView() {
    super.onDestroyView();
    unsubLoadIcon();
  }

  @Override public void loadApplicationIcon(@NonNull String packageName) {
    loadIconSubscription = interactor.loadPackageIcon(packageName)
        .subscribeOn(ioScheduler)
        .observeOn(mainScheduler)
        .subscribe(drawable -> {
          final AppIconLoaderView loaderView = getView();
          if (loaderView != null) {
            loaderView.onApplicationIconLoadedSuccess(drawable);
          }
        }, throwable -> {
          Timber.e(throwable, "onError");
          final AppIconLoaderView loaderView = getView();
          if (loaderView != null) {
            loaderView.onApplicationIconLoadedError();
          }
        });
  }

  private void unsubLoadIcon() {
    if (!loadIconSubscription.isUnsubscribed()) {
      Timber.d("Unsub from load icon event");
      loadIconSubscription.unsubscribe();
    }
  }
}
