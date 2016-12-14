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

import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import com.pyamsoft.padlock.model.sql.PadLockEntry;
import java.util.List;
import rx.Observable;

interface PurgeInteractor {

  @CheckResult @NonNull Observable<String> getActiveApplicationPackageNames();

  @CheckResult @NonNull Observable<List<PadLockEntry.AllEntries>> getAppEntryList();

  @CheckResult @NonNull Observable<Integer> deleteEntry(@NonNull String packageName);

  @CheckResult boolean isCacheEmpty();

  @CheckResult @NonNull Observable<String> getCachedEntries();

  void clearCache();

  void cacheEntry(@NonNull String entry);

  void removeFromCache(@NonNull String entry);
}
