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

package com.pyamsoft.padlock.app.service;

import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import com.pyamsoft.padlock.model.sql.PadLockEntry;
import com.pyamsoft.pydroid.dagger.presenter.Presenter;

public interface LockServicePresenter extends Presenter<LockServicePresenter.LockService> {

  void setLockScreenPassed();

  @NonNull @CheckResult String getActiveClassName();

  @NonNull @CheckResult String getActivePackageName();

  void processAccessibilityEvent(@NonNull String packageName, @NonNull String className,
      boolean forcedRecheck);

  interface LockService {

    void startLockScreen1(@NonNull PadLockEntry entry, @NonNull String realName);

    void startLockScreen2(@NonNull PadLockEntry entry, @NonNull String realName);
  }
}
