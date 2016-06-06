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
import android.support.v7.widget.RecyclerView;
import com.pyamsoft.padlock.app.list.AdapterPresenter;
import com.pyamsoft.pydroid.base.PresenterImpl;
import javax.inject.Named;
import rx.Scheduler;
import rx.Subscription;
import rx.subscriptions.CompositeSubscription;

public abstract class AdapterPresenterImpl<I, VH extends RecyclerView.ViewHolder>
    extends PresenterImpl<AdapterPresenter.AdapterView> implements AdapterPresenter<I, VH> {

  @NonNull private final Scheduler ioScheduler;
  @NonNull private final Scheduler mainScheduler;
  @NonNull private final AdapterInteractor<I> adapterInteractor;
  @NonNull private final CompositeSubscription compositeSubscription;

  protected AdapterPresenterImpl(@NonNull AdapterInteractor<I> adapterInteractor,
      @NonNull @Named("io") Scheduler ioScheduler,
      @NonNull @Named("main") Scheduler mainScheduler) {
    this.ioScheduler = ioScheduler;
    this.mainScheduler = mainScheduler;
    this.adapterInteractor = adapterInteractor;
    compositeSubscription = new CompositeSubscription();
  }

  @Override public void onDestroyView() {
    super.onDestroyView();
    if (!compositeSubscription.isUnsubscribed()) {
      compositeSubscription.clear();
    }
  }

  protected void set(int position, @NonNull I entry) {
    adapterInteractor.set(position, entry);
  }

  @CheckResult @NonNull @Override public I get(int position) {
    return adapterInteractor.get(position);
  }

  @CheckResult @Override public int add(@NonNull I entry) {
    return adapterInteractor.add(entry);
  }

  @CheckResult @Override public int remove() {
    return adapterInteractor.remove();
  }

  @CheckResult @Override public int size() {
    return adapterInteractor.size();
  }

  @Override public void loadApplicationIcon(@NonNull VH holder, @NonNull String packageName) {
    final Subscription subscription = adapterInteractor.loadPackageIcon(packageName)
        .subscribeOn(ioScheduler)
        .observeOn(mainScheduler)
        .subscribe(drawable -> {
          final AdapterView view = getView();
          if (view != null) {
            view.onApplicationIconLoadedSuccess(holder, drawable);
          }
        }, throwable -> {
          final AdapterView view = getView();
          if (view != null) {
            view.onApplicationIconLoadedError(holder);
          }
        });
    compositeSubscription.add(subscription);
  }
}
