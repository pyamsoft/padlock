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

package com.pyamsoft.padlock.app.lock;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.pyamsoft.padlock.app.iconloader.AppIconLoaderPresenter;

public interface LockScreenPresenter
    extends LockPresenter<LockScreen>, AppIconLoaderPresenter<LockScreen> {

  void displayLockedHint();

  void createWithDefaultIgnoreTime();

  void lockEntry(@NonNull String packageName, @NonNull String activityName,
      @Nullable String lockCode, long lockUntilTime, long ignoreUntilTime, boolean isSystem);

  void submit(@NonNull String packageName, @NonNull String activityName,
      @NonNull String currentAttempt);

  void loadDisplayNameFromPackage(@NonNull String packageName);

  void postUnlock(@NonNull String packageName, @NonNull String activityName,
      @NonNull String realName, @Nullable String lockCode, boolean isSystem, boolean shouldExclude,
      long ignoreTime);
}
