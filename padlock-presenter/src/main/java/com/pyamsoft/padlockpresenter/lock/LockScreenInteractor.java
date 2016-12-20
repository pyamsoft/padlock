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

package com.pyamsoft.padlockpresenter.lock;

import android.app.IntentService;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import rx.Observable;

interface LockScreenInteractor extends LockInteractor {

  int DEFAULT_MAX_FAIL_COUNT = 2;

  @CheckResult @NonNull Observable<Boolean> unlockEntry(@NonNull String attempt,
      @NonNull String pin);

  @CheckResult @NonNull Observable<Long> lockEntry(long lockUntilTime, @NonNull String packageName,
      @NonNull String activityName);

  @CheckResult @NonNull Observable<String> getDisplayName(@NonNull String packageName);

  @CheckResult @NonNull Observable<Long> getDefaultIgnoreTime();

  @CheckResult @NonNull Observable<Long> getTimeoutPeriodMinutesInMillis();

  @CheckResult @NonNull Observable<String> getMasterPin();

  @CheckResult @NonNull Observable<Long> whitelistEntry(@NonNull String packageName,
      @NonNull String activityName, @NonNull String realName, @Nullable String lockCode,
      boolean isSystem);

  @CheckResult @NonNull Observable<Integer> queueRecheckJob(@NonNull String packageName,
      @NonNull String activityName, long recheckTime);

  @NonNull @CheckResult Observable<Integer> ignoreEntryForTime(long ignoreMinutesInMillis,
      @NonNull String packageName, @NonNull String activityName);

  @CheckResult @NonNull Observable<Integer> incrementAndGetFailCount();

  @CheckResult @NonNull Observable<String> getHint();

  void resetFailCount();
}
