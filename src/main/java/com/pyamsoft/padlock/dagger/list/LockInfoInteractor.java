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

package com.pyamsoft.padlock.dagger.list;

import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.pyamsoft.padlock.model.sql.PadLockEntry;
import java.util.List;
import rx.Observable;

interface LockInfoInteractor extends LockCommonInteractor {

  @CheckResult @NonNull Observable<List<PadLockEntry.WithPackageName>> getActivityEntries(
      @NonNull String packageName);

  @CheckResult @NonNull Observable<String> getPackageActivities(@NonNull String packageName);

  /**
   * Returns a boolean observable where True means created and False means deleted
   */
  @CheckResult @NonNull Observable<Boolean> modifyDatabaseGroup(boolean allCreate,
      @NonNull String packageName, @Nullable String code, boolean system);

  void setShownOnBoarding();

  @CheckResult @NonNull Observable<Boolean> hasShownOnBoarding();
}
