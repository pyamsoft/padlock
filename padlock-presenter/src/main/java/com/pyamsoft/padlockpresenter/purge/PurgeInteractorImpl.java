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

package com.pyamsoft.padlockpresenter.purge;

import android.support.annotation.NonNull;
import com.pyamsoft.padlockmodel.sql.PadLockEntry;
import com.pyamsoft.padlockpresenter.PadLockDB;
import com.pyamsoft.padlockpresenter.wrapper.PackageManagerWrapper;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import rx.Observable;

class PurgeInteractorImpl implements PurgeInteractor {

  @SuppressWarnings("WeakerAccess") @NonNull final List<String> stalePackageNameCache;
  @NonNull private final PackageManagerWrapper packageManagerWrapper;
  @NonNull private final PadLockDB padLockDB;

  @Inject PurgeInteractorImpl(@NonNull PackageManagerWrapper packageManagerWrapper,
      @NonNull PadLockDB padLockDB) {
    this.packageManagerWrapper = packageManagerWrapper;
    this.padLockDB = padLockDB;
    stalePackageNameCache = new ArrayList<>();
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

  @Override public boolean isCacheEmpty() {
    return stalePackageNameCache.isEmpty();
  }

  @NonNull @Override public Observable<String> getCachedEntries() {
    return Observable.defer(() -> Observable.from(stalePackageNameCache));
  }

  @Override public void clearCache() {
    stalePackageNameCache.clear();
  }

  @Override public void cacheEntry(@NonNull String entry) {
    stalePackageNameCache.add(entry);
  }

  @Override public void removeFromCache(@NonNull String entry) {
    stalePackageNameCache.remove(entry);
  }
}
