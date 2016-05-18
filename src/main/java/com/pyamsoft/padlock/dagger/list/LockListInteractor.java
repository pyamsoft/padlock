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

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;
import com.pyamsoft.padlock.model.sql.PadLockEntry;
import java.util.List;
import rx.Observable;

interface LockListInteractor {

  @CheckResult @WorkerThread @NonNull Observable<List<PackageInfo>> getPackageInfoList();

  @CheckResult @WorkerThread @NonNull Observable<List<PadLockEntry>> getAppEntryList();

  @CheckResult @WorkerThread @NonNull PackageManager getPackageManager();

  @CheckResult boolean isSystemApplication(@NonNull ApplicationInfo info);

  @CheckResult boolean hasShownOnBoarding();

  @CheckResult boolean isSystemVisible();

  void setSystemVisible(boolean visible);

  void setShownOnBoarding();
}
