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
import android.support.annotation.WorkerThread;
import com.pyamsoft.padlock.model.sql.PadLockEntry;
import rx.Observable;

public interface LockServiceInteractor {

  // Android Packages
  @NonNull String ANDROID_SYSTEM_UI_PACKAGE = "com.android.systemui";
  @NonNull String ANDROID_PACKAGE = "android";

  @CheckResult boolean isEventCausedByNotificationShade(@NonNull String packageName,
      @NonNull String className);

  @CheckResult boolean hasNameChanged(@NonNull String name, @NonNull String oldName);

  @CheckResult boolean isLiveEvent(@NonNull String packageName, @NonNull String className);

  @CheckResult boolean isWindowFromLockScreen(@NonNull String packageName, @NonNull String className);

  @CheckResult boolean isWindowFromKeyboard(@NonNull String packageName, @NonNull String className);

  @CheckResult boolean isDeviceLocked();

  @NonNull @CheckResult @WorkerThread Observable<PadLockEntry> getEntry(@NonNull String packageName,
      @NonNull String activityName);
}
