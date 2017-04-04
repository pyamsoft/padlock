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

package com.pyamsoft.padlock.list;

import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.pyamsoft.padlock.model.ActivityEntry;
import io.reactivex.Single;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton class LockInfoCacheInteractor {

  @NonNull private final Map<String, Single<List<ActivityEntry>>> cachedInfoObservableMap;

  @Inject LockInfoCacheInteractor() {
    cachedInfoObservableMap = new HashMap<>();
  }

  void putIntoCache(@NonNull String packageName, @NonNull Single<List<ActivityEntry>> dataSource) {
    cachedInfoObservableMap.put(packageName, dataSource);
  }

  @CheckResult @Nullable Single<List<ActivityEntry>> getFromCache(@NonNull String packageName) {
    return cachedInfoObservableMap.get(packageName);
  }

  void clearCache() {
    cachedInfoObservableMap.clear();
  }
}
