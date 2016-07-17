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

package com.pyamsoft.padlock.dagger.lock;

import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import rx.Observable;

public interface LockScreenInteractor extends LockInteractor {

  int DEFAULT_MAX_FAIL_COUNT = 2;

  @CheckResult @NonNull Observable<Boolean> unlockEntry(@NonNull String packageName,
      @NonNull String activityName, @NonNull String attempt);

  @CheckResult @NonNull Observable<Boolean> postUnlock(@NonNull String packageName,
      @NonNull String activityName, boolean exclude, long ignoreTime, long recheckTime);

  @CheckResult @NonNull Observable<Boolean> lockEntry(@NonNull String packageName,
      @NonNull String activityName);

  @CheckResult @NonNull Observable<Long> getDefaultIgnoreTime();

  @CheckResult @NonNull Observable<String> getDisplayName(@NonNull String packageName);

  @CheckResult @NonNull Observable<Long> getIgnoreTimeForIndex(int index);

  @CheckResult @NonNull Observable<Long> getIgnoreTimeOne();

  @CheckResult @NonNull Observable<Long> getIgnoreTimeFive();

  @CheckResult @NonNull Observable<Long> getIgnoreTimeTen();

  @CheckResult @NonNull Observable<Long> getIgnoreTimeFifteen();

  @CheckResult @NonNull Observable<Long> getIgnoreTimeTwenty();

  @CheckResult @NonNull Observable<Long> getIgnoreTimeThirty();

  @CheckResult @NonNull Observable<Long> getIgnoreTimeFourtyFive();

  @CheckResult @NonNull Observable<Long> getIgnoreTimeSixty();

  @CheckResult @NonNull Observable<Integer> incrementAndGetFailCount();

  void resetFailCount();
}
