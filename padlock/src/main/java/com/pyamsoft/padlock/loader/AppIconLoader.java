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
import android.support.annotation.NonNull;
import android.widget.ImageView;
import com.pyamsoft.padlock.Injector;
import com.pyamsoft.padlock.base.wrapper.PackageManagerWrapper;
import com.pyamsoft.pydroid.helper.SchedulerHelper;
import com.pyamsoft.pydroid.loader.GenericLoader;
import com.pyamsoft.pydroid.loader.loaded.Loaded;
import com.pyamsoft.pydroid.loader.loaded.RxLoaded;
import com.pyamsoft.pydroid.loader.targets.DrawableImageTarget;
import com.pyamsoft.pydroid.loader.targets.Target;
import io.reactivex.Scheduler;
import javax.inject.Inject;
import javax.inject.Named;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import timber.log.Timber;

public class AppIconLoader extends GenericLoader<AppIconLoader, Drawable> {

  @NonNull private final String packageName;
  @SuppressWarnings("WeakerAccess") @Inject PackageManagerWrapper packageManagerWrapper;
  @SuppressWarnings("WeakerAccess") @Inject @Named("obs") Scheduler obsScheduler;
  @SuppressWarnings("WeakerAccess") @Inject @Named("sub") Scheduler subScheduler;

  private AppIconLoader(@NonNull String packageName) {
    this.packageName = packageName;
    Injector.get().provideComponent().inject(this);
    if (this.packageName.isEmpty()) {
      throw new IllegalArgumentException("AppIconLoader packageName must be non-empty");
    }

    SchedulerHelper.enforceForegroundScheduler(obsScheduler);
    SchedulerHelper.enforceBackgroundScheduler(subScheduler);
  }

  @CheckResult @NonNull public static AppIconLoader forPackageName(@NonNull String packageName) {
    return new AppIconLoader(packageName);
  }

  @Override public AppIconLoader withCompleteAction(
      Function1<? super Target<? super Drawable>, Unit> function1) {
    setCompleteAction(function1);
    return this;
  }

  @Override public AppIconLoader withErrorAction(
      Function1<? super Target<? super Drawable>, Unit> function1) {
    setErrorAction(function1);
    return this;
  }

  @Override public AppIconLoader withStartAction(
      Function1<? super Target<? super Drawable>, Unit> function1) {
    setStartAction(function1);
    return this;
  }

  @NonNull @Override public Loaded into(@NonNull ImageView imageView) {
    return into(DrawableImageTarget.forImageView(imageView));
  }

  @NotNull @Override public Loaded into(Target<? super Drawable> target) {
    return load(target, packageName);
  }

  @CheckResult @NonNull
  private Loaded load(@NonNull Target<? super Drawable> target, @NonNull String packageName) {
    return new RxLoaded(packageManagerWrapper.loadDrawableForPackageOrDefault(packageName)
        .subscribeOn(subScheduler)
        .observeOn(obsScheduler)
        .subscribe(target::loadImage,
            throwable -> Timber.e(throwable, "Error loading Drawable AppIconLoader for: %s",
                packageName)));
  }

  @Override public AppIconLoader tint(int i) {
    setTint(i);
    return this;
  }
}
