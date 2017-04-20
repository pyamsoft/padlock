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

package com.pyamsoft.padlock.base.wrapper;

import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.graphics.drawable.Drawable;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import io.reactivex.Maybe;
import io.reactivex.Single;
import java.util.List;

public interface PackageManagerWrapper {

  @CheckResult @NonNull Single<List<ApplicationInfo>> getActiveApplications();

  @CheckResult @NonNull Single<List<String>> getActivityListForPackage(@NonNull String packageName);

  @CheckResult @NonNull Maybe<String> loadPackageLabel(@NonNull ApplicationInfo info);

  @CheckResult @NonNull Maybe<String> loadPackageLabel(@NonNull String packageName);

  @CheckResult @NonNull Single<Drawable> loadDrawableForPackageOrDefault(
      @NonNull String packageName);

  @CheckResult @NonNull Maybe<ApplicationInfo> getApplicationInfo(@NonNull String packageName);

  @CheckResult @NonNull Maybe<ActivityInfo> getActivityInfo(@NonNull String packageName,
      @NonNull String activityName);
}
