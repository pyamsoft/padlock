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

import android.content.pm.ApplicationInfo;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import com.pyamsoft.padlockmodel.AppEntry;
import com.pyamsoft.padlockmodel.sql.PadLockEntry;
import java.util.List;
import rx.Observable;

interface LockListInteractor extends LockCommonInteractor {

  @CheckResult @NonNull Observable<ApplicationInfo> getActiveApplications();

  @CheckResult @NonNull Observable<String> getActivityListForApplication(
      @NonNull ApplicationInfo info);

  @CheckResult @NonNull Observable<List<PadLockEntry.AllEntries>> getAppEntryList();

  @CheckResult @NonNull Observable<Boolean> hasShownOnBoarding();

  @CheckResult @NonNull Observable<Boolean> isSystemVisible();

  void setSystemVisible(boolean visible);

  void setShownOnBoarding();

  @NonNull @CheckResult Observable<AppEntry> createFromPackageInfo(@NonNull String packageName,
      boolean locked);

  @CheckResult boolean isSystemApplication(@NonNull ApplicationInfo info);

  @CheckResult boolean isCacheEmpty();

  @CheckResult @NonNull Observable<AppEntry> getCachedEntries();

  void clearCache();

  void updateCacheEntry(@NonNull String name, @NonNull String packageName, boolean newLockState);

  void cacheEntry(@NonNull AppEntry entry);
}