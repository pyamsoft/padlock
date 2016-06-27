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

package com.pyamsoft.padlock.app.list;

import android.graphics.drawable.Drawable;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import com.pyamsoft.padlock.app.base.AppIconLoaderPresenter;
import com.pyamsoft.padlock.app.base.AppIconLoaderView;
import com.pyamsoft.padlock.dagger.list.AdapterInteractor;
import java.lang.ref.WeakReference;
import javax.inject.Named;
import rx.Scheduler;
import rx.Subscription;
import rx.subscriptions.CompositeSubscription;

public abstract class AdapterPresenter<I, VH extends RecyclerView.ViewHolder>
    extends AppIconLoaderPresenter<AdapterPresenter.AdapterView<VH>> {

  @NonNull private final AdapterInteractor<I> adapterInteractor;
  @NonNull private final CompositeSubscription compositeSubscription;

  protected AdapterPresenter(@NonNull AdapterInteractor<I> adapterInteractor,
      @NonNull @Named("main") Scheduler mainScheduler,
      @NonNull @Named("io") Scheduler ioScheduler) {
    super(adapterInteractor, mainScheduler, ioScheduler);
    this.adapterInteractor = adapterInteractor;
    compositeSubscription = new CompositeSubscription();
  }

  @Override protected void onUnbind() {
    super.onUnbind();
    if (!compositeSubscription.isUnsubscribed()) {
      compositeSubscription.clear();
    }
  }

  protected void set(int position, @NonNull I entry) {
    adapterInteractor.set(position, entry);
  }

  @CheckResult @NonNull public final I get(int position) {
    return adapterInteractor.get(position).toBlocking().first();
  }

  @CheckResult public final int add(@NonNull I entry) {
    return adapterInteractor.add(entry).toBlocking().first();
  }

  @CheckResult public final int remove() {
    return adapterInteractor.remove().toBlocking().first();
  }

  @CheckResult public final int size() {
    return adapterInteractor.size().toBlocking().first();
  }

  public final void loadApplicationIcon(@NonNull VH holder, @NonNull String packageName) {
    final WeakReference<VH> weakViewHolder = new WeakReference<>(holder);
    final Subscription subscription = adapterInteractor.loadPackageIcon(packageName)
        .subscribeOn(getSubscribeScheduler())
        .observeOn(getObserveScheduler())
        .subscribe(drawable -> {
          final AdapterView<VH> view = getView();
          final VH viewHolder = weakViewHolder.get();
          if (viewHolder != null) {
            view.onApplicationIconLoadedSuccess(viewHolder, drawable);
          }
        }, throwable -> {
          final AdapterView<VH> view = getView();
          final VH viewHolder = weakViewHolder.get();
          if (viewHolder != null) {
            view.onApplicationIconLoadedError(viewHolder);
          }
        });
    compositeSubscription.add(subscription);
  }

  public abstract void setLocked(int position, boolean locked);

  public interface AdapterView<VH extends RecyclerView.ViewHolder> extends AppIconLoaderView {

    void onApplicationIconLoadedSuccess(@NonNull VH holder, @NonNull Drawable drawable);

    void onApplicationIconLoadedError(@NonNull VH holder);
  }
}
