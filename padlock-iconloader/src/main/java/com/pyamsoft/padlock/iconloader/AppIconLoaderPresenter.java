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

package com.pyamsoft.padlock.iconloader;

import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import com.pyamsoft.pydroid.helper.DisposableHelper;
import com.pyamsoft.pydroid.presenter.Presenter;
import com.pyamsoft.pydroid.presenter.SchedulerPresenter;
import io.reactivex.Scheduler;
import io.reactivex.disposables.Disposable;
import io.reactivex.disposables.Disposables;
import javax.inject.Inject;
import javax.inject.Named;
import timber.log.Timber;

public class AppIconLoaderPresenter extends SchedulerPresenter<Presenter.Empty> {

  @NonNull private final AppIconLoaderInteractor interactor;
  @NonNull private Disposable loadIconDisposable = Disposables.empty();

  @Inject AppIconLoaderPresenter(@NonNull AppIconLoaderInteractor interactor,
      @NonNull @Named("obs") Scheduler obsScheduler, @NonNull @Named("io") Scheduler subScheduler) {
    super(obsScheduler, subScheduler);
    this.interactor = interactor;
  }

  @Override protected void onUnbind() {
    super.onUnbind();
    loadIconDisposable = DisposableHelper.unsubscribe(loadIconDisposable);
  }

  public void loadApplicationIcon(@NonNull String packageName, @NonNull LoadCallback callback) {
    loadIconDisposable = DisposableHelper.unsubscribe(loadIconDisposable);
    loadIconDisposable = interactor.loadPackageIcon(packageName)
        .subscribeOn(getSubscribeScheduler())
        .observeOn(getObserveScheduler())
        .subscribe(callback::onApplicationIconLoadedSuccess, throwable -> {
          Timber.e(throwable, "onError");
          callback.onApplicationIconLoadedError();
        });
  }

  public interface LoadCallback {
    void onApplicationIconLoadedSuccess(@NonNull Drawable icon);

    void onApplicationIconLoadedError();
  }
}
