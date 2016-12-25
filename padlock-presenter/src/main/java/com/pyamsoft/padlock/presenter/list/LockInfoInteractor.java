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

package com.pyamsoft.padlock.presenter.list;

import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import com.pyamsoft.padlock.model.ActivityEntry;
import com.pyamsoft.padlock.model.LockState;
import com.pyamsoft.padlock.model.sql.PadLockEntry;
import java.util.List;
import rx.Observable;

interface LockInfoInteractor extends LockCommonInteractor {

  @CheckResult @NonNull Observable<List<PadLockEntry.WithPackageName>> getLockedActivityEntries(
      @NonNull String packageName);

  @CheckResult @NonNull Observable<List<String>> getPackageActivities(@NonNull String packageName);

  @CheckResult @NonNull Observable<LockState> updateExistingEntry(@NonNull String packageName,
      @NonNull String activityName, boolean whitelist);

  void setShownOnBoarding();

  @CheckResult @NonNull Observable<Boolean> hasShownOnBoarding();

  @CheckResult boolean isCacheEmpty();

  @CheckResult @NonNull Observable<ActivityEntry> getCachedEntries();

  void clearCache();

  void updateCacheEntry(@NonNull String name, @NonNull LockState lockState);

  void cacheEntry(@NonNull ActivityEntry entry);
}