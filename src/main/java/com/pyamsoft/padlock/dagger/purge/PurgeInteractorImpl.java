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

package com.pyamsoft.padlock.dagger.purge;

import android.support.annotation.NonNull;
import com.pyamsoft.padlock.app.wrapper.PackageManagerWrapper;
import com.pyamsoft.padlock.dagger.PadLockDB;
import com.pyamsoft.padlock.model.sql.PadLockEntry;
import java.util.List;
import javax.inject.Inject;
import rx.Observable;

class PurgeInteractorImpl implements PurgeInteractor {

  @NonNull private final PackageManagerWrapper packageManagerWrapper;
  @NonNull private final PadLockDB padLockDB;

  @Inject PurgeInteractorImpl(@NonNull PackageManagerWrapper packageManagerWrapper,
      @NonNull PadLockDB padLockDB) {
    this.packageManagerWrapper = packageManagerWrapper;
    this.padLockDB = padLockDB;
  }

  @NonNull @Override public Observable<String> getActiveApplicationPackageNames() {
    return packageManagerWrapper.getActiveApplications()
        .map(applicationInfo -> applicationInfo.packageName);
  }

  @NonNull @Override public Observable<List<PadLockEntry.AllEntries>> getAppEntryList() {
    return padLockDB.queryAll().first();
  }

  @NonNull @Override public Observable<Integer> deleteEntry(@NonNull String packageName) {
    return padLockDB.deleteWithPackageName(packageName);
  }
}
