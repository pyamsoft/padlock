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

package com.pyamsoft.padlock.app.lockscreen;

import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;
import com.pyamsoft.padlock.app.lock.LockInteractor;
import rx.Observable;

public interface LockScreenInteractor extends LockInteractor {

  @WorkerThread @NonNull Observable<Boolean> unlockEntry(String packageName, String activityName,
      String attempt, boolean shouldExclude, long ignoreForPeriod);

  @WorkerThread @NonNull Observable<Boolean> lockEntry(String packageName, String activityName);

  @WorkerThread @NonNull Observable<Long> getDefaultIgnoreTime();

  @WorkerThread @NonNull Observable<Long> setDefaultIgnoreTime(long ignoreTime);
}
