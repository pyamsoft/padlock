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

import android.graphics.drawable.Drawable;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;
import com.pyamsoft.padlock.app.base.PackageManagerWrapper;
import javax.inject.Inject;
import rx.Observable;

public class AppIconLoaderInteractorImpl implements AppIconLoaderInteractor {

  @NonNull private final PackageManagerWrapper packageManagerWrapper;

  @Inject
  protected AppIconLoaderInteractorImpl(@NonNull PackageManagerWrapper packageManagerWrapper) {
    this.packageManagerWrapper = packageManagerWrapper;
  }

  @NonNull @WorkerThread @CheckResult
  public final Observable<Drawable> loadPackageIcon(final @NonNull String packageName) {
    return packageManagerWrapper.loadDrawableForPackageOrDefault(packageName);
  }
}
