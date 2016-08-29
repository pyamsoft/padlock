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

package com.pyamsoft.padlock.dagger.db;

import android.content.Context;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.pyamsoft.padlock.app.wrapper.PackageManagerWrapper;
import com.pyamsoft.padlock.app.sql.PadLockDB;
import javax.inject.Inject;
import rx.Observable;
import timber.log.Timber;

class DBInteractorImpl implements DBInteractor {

  @NonNull final Context appContext;
  @NonNull final PackageManagerWrapper packageManagerWrapper;

  @Inject DBInteractorImpl(final @NonNull Context context,
      @NonNull PackageManagerWrapper packageManagerWrapper) {
    this.packageManagerWrapper = packageManagerWrapper;
    appContext = context.getApplicationContext();
  }

  @NonNull @CheckResult @Override
  public Observable<Long> createActivityEntries(@NonNull String packageName, @Nullable String code,
      boolean system, boolean whitelist) {
    return packageManagerWrapper.getActivityListForPackage(packageName)
        .filter(s -> s != null && !s.isEmpty())
        .concatMap(activityName -> createEntry(packageName, activityName, code, system, whitelist));
  }

  @NonNull @CheckResult @Override
  public Observable<Long> createEntry(@NonNull String packageName, @NonNull String activityName,
      @Nullable String code, boolean system, boolean whitelist) {

    // To prevent double creations from occurring, first call a delete on the DB for packageName, activityName
    return deleteEntry(packageName, activityName).flatMap(integer -> {
      Timber.d("CREATE: %s %s", packageName, activityName);
      return PadLockDB.with(appContext).insert(packageName, activityName, code, 0, 0, system, whitelist);
    });
  }

  @NonNull @Override public Observable<Integer> deleteActivityEntries(@NonNull String packageName) {
    Timber.d("DELETE: all %s", packageName);
    return PadLockDB.with(appContext).deleteWithPackageName(packageName);
  }

  @NonNull @Override public Observable<Integer> deleteEntry(@NonNull String packageName,
      @NonNull String activityName) {
    Timber.d("DELETE: entry %s", packageName);
    return PadLockDB.with(appContext).deleteWithPackageActivityName(packageName, activityName);
  }
}
