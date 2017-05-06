/*
 * Copyright 2017 Peter Kenji Yamanaka
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

package com.pyamsoft.padlock.loader;

import android.graphics.drawable.Drawable;
import android.support.annotation.CheckResult;
import android.support.annotation.ColorRes;
import android.support.annotation.NonNull;
import android.widget.ImageView;
import com.pyamsoft.padlock.Injector;
import com.pyamsoft.padlock.base.wrapper.PackageManagerWrapper;
import com.pyamsoft.pydroid.function.ActionSingle;
import com.pyamsoft.pydroid.helper.Checker;
import com.pyamsoft.pydroid.helper.SchedulerHelper;
import com.pyamsoft.pydroid.loader.GenericLoader;
import com.pyamsoft.pydroid.loader.loaded.Loaded;
import com.pyamsoft.pydroid.loader.loaded.RxLoaded;
import com.pyamsoft.pydroid.loader.targets.DrawableImageTarget;
import com.pyamsoft.pydroid.loader.targets.Target;
import io.reactivex.Scheduler;
import javax.inject.Inject;
import javax.inject.Named;
import timber.log.Timber;

public class AppIconLoader extends GenericLoader<AppIconLoader, Drawable> {

  @NonNull private final String packageName;
  @SuppressWarnings("WeakerAccess") @Inject PackageManagerWrapper packageManagerWrapper;
  @SuppressWarnings("WeakerAccess") @Inject @Named("obs") Scheduler obsScheduler;
  @SuppressWarnings("WeakerAccess") @Inject @Named("sub") Scheduler subScheduler;

  private AppIconLoader(@NonNull String packageName) {
    this.packageName = Checker.checkNonNull(packageName);
    Injector.get().provideComponent().inject(this);
    if (this.packageName.isEmpty()) {
      throw new IllegalArgumentException("AppIconLoader packageName must be non-empty");
    }

    SchedulerHelper.enforceObserveScheduler(obsScheduler);
    SchedulerHelper.enforceSubscribeScheduler(subScheduler);
  }

  @CheckResult @NonNull public static AppIconLoader forPackageName(@NonNull String packageName) {
    return new AppIconLoader(packageName);
  }

  @NonNull @Override public AppIconLoader tint(@ColorRes int i) {
    this.tint = i;
    return this;
  }

  @NonNull @Override
  public AppIconLoader setStartAction(@NonNull ActionSingle<Target<Drawable>> actionSingle) {
    this.startAction = actionSingle;
    return this;
  }

  @NonNull @Override
  public AppIconLoader setErrorAction(@NonNull ActionSingle<Target<Drawable>> actionSingle) {
    this.errorAction = actionSingle;
    return this;
  }

  @NonNull @Override
  public AppIconLoader setCompleteAction(@NonNull ActionSingle<Target<Drawable>> actionSingle) {
    this.completeAction = actionSingle;
    return this;
  }

  @NonNull @Override public Loaded into(@NonNull ImageView imageView) {
    return into(DrawableImageTarget.forImageView(imageView));
  }

  @NonNull @Override public Loaded into(@NonNull Target<Drawable> target) {
    return load(target, packageName);
  }

  @CheckResult @NonNull
  private Loaded load(@NonNull Target<Drawable> target, @NonNull String packageName) {
    return new RxLoaded(packageManagerWrapper.loadDrawableForPackageOrDefault(packageName)
        .subscribeOn(subScheduler)
        .observeOn(obsScheduler)
        .subscribe(target::loadImage,
            throwable -> Timber.e(throwable, "Error loading Drawable AppIconLoader for: %s",
                packageName)));
  }
}
