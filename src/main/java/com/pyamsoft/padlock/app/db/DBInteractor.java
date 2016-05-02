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

package com.pyamsoft.padlock.app.db;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import com.pyamsoft.padlock.app.lock.LockCommonInteractor;

public interface DBInteractor extends LockCommonInteractor {

  @WorkerThread void createEntry(@NonNull String packageName, @NonNull String name,
      @Nullable String code, boolean system);

  @WorkerThread void createEntry(@NonNull String packageName, @NonNull String activityName,
      @NonNull String name, @Nullable String code, boolean system);

  @WorkerThread void deleteEntry(@NonNull String packageName);

  @WorkerThread void deleteEntry(@NonNull String packageName, @NonNull String activityName);
}
