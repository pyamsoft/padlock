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

package com.pyamsoft.padlockpresenter.list;

import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.pyamsoft.padlockmodel.LockState;
import rx.Observable;

interface LockCommonInteractor {
  //
  //@CheckResult @NonNull Observable<LockState> createNewEntry(@NonNull String packageName,
  //    @NonNull String activityName, @Nullable String code, boolean system, boolean whitelist);
  //
  //@CheckResult @NonNull Observable<LockState> deleteEntry(@NonNull String packageName,
  //    @NonNull String activityName);

  @NonNull @CheckResult Observable<LockState> modifySingleDatabaseEntry(boolean notInDatabase,
      @NonNull String packageName, @NonNull String activityName, @Nullable String code,
      boolean system, boolean whitelist, boolean forceLock);
}
