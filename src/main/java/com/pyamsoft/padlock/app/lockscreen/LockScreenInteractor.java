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
import rx.Observable;

public interface LockScreenInteractor {

  long getDefaultIgnoreTime();

  void setDefaultIgnoreTime(long ignoreTime);

  long getTimeoutPeriod();

  void setTimeoutPeriod(long ignoreTime);

  boolean isSubmittable(String attempt);

  @NonNull Observable<Boolean> unlockEntry(String packageName, String activityName, String attempt,
      boolean shouldExclude, long ignoreForPeriod);

  @NonNull Observable<Boolean> lockEntry(String packageName, String activityName);
}
