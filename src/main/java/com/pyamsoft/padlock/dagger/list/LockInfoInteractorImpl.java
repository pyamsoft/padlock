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

import android.content.Context;
import android.support.annotation.NonNull;
import com.pyamsoft.padlock.app.PackageManagerWrapper;
import com.pyamsoft.padlock.app.lock.LockScreenActivity1;
import com.pyamsoft.padlock.app.lock.LockScreenActivity2;
import com.pyamsoft.padlock.app.sql.PadLockDB;
import com.pyamsoft.padlock.model.sql.PadLockEntry;
import com.pyamsoft.pydroid.crash.CrashLogActivity;
import java.util.List;
import javax.inject.Inject;
import rx.Observable;

class LockInfoInteractorImpl extends AppIconLoaderInteractorImpl
    implements LockInfoInteractor {

  @NonNull final Context appContext;
  @NonNull final PackageManagerWrapper packageManagerWrapper;

  @Inject LockInfoInteractorImpl(final @NonNull Context context,
      @NonNull PackageManagerWrapper packageManagerWrapper) {
    super(packageManagerWrapper);
    this.packageManagerWrapper = packageManagerWrapper;
    appContext = context.getApplicationContext();
  }

  @NonNull @Override
  public Observable<List<PadLockEntry>> getActivityEntries(@NonNull String packageName) {
    return PadLockDB.with(appContext).queryWithPackageName(packageName).first();
  }

  @NonNull @Override public Observable<String> getPackageActivities(@NonNull String packageName) {
    return packageManagerWrapper.getActivityListForPackage(packageName)
        .filter(
            activityEntry -> !activityEntry.equalsIgnoreCase(LockScreenActivity1.class.getName())
                && !activityEntry.equalsIgnoreCase(LockScreenActivity2.class.getName())
                && !activityEntry.equalsIgnoreCase(CrashLogActivity.class.getName()));
  }
}
