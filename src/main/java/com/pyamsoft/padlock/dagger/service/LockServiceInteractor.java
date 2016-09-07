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

package com.pyamsoft.padlock.dagger.service;

import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import com.pyamsoft.padlock.model.sql.PadLockEntry;
import rx.Observable;

public interface LockServiceInteractor {

  // Android Packages
  @NonNull String ANDROID_SYSTEM_UI_PACKAGE = "com.android.systemui";
  @NonNull String ANDROID_PACKAGE = "android";

  void cleanup();

  @NonNull @CheckResult Observable<Boolean> isEventFromActivity(@NonNull String packageName,
      @NonNull String className);

  @NonNull @CheckResult Observable<Boolean> hasNameChanged(@NonNull String name,
      @NonNull String oldName);

  @NonNull @CheckResult Observable<Boolean> isWindowFromLockScreen(@NonNull String packageName,
      @NonNull String className);

  @NonNull @CheckResult Observable<Boolean> isOnlyLockOnPackageChange();

  @NonNull @CheckResult Observable<PadLockEntry> getEntry(@NonNull String packageName,
      @NonNull String activityName);
}
