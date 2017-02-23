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

package com.pyamsoft.padlock.lock;

import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;
import com.pyamsoft.padlock.base.PadLockPreferences;
import com.pyamsoft.padlock.base.wrapper.PackageManagerWrapper;
import com.pyamsoft.padlock.lock.common.LockTypeInteractor;
import javax.inject.Inject;
import rx.Observable;

class LockScreenInteractor extends LockTypeInteractor {

  @NonNull private final PackageManagerWrapper packageManagerWrapper;

  @Inject LockScreenInteractor(@NonNull final PadLockPreferences preferences,
      @NonNull PackageManagerWrapper packageManagerWrapper) {
    super(preferences);
    this.packageManagerWrapper = packageManagerWrapper;
  }

  @NonNull @CheckResult public Observable<Long> getDefaultIgnoreTime() {
    return Observable.fromCallable(getPreferences()::getDefaultIgnoreTime);
  }

  @WorkerThread @NonNull @CheckResult
  public Observable<String> getDisplayName(@NonNull String packageName) {
    return packageManagerWrapper.loadPackageLabel(packageName);
  }
}
