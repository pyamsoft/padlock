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

package com.pyamsoft.padlock.dagger.job;

import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import com.birbit.android.jobqueue.Job;
import com.pyamsoft.padlock.app.service.PadLockService;

public class RecheckJob extends BaseJob {

  @NonNull public static final String TAG_CLASS_PREFIX = "C: ";
  @NonNull private static final String TAG_PACKAGE_PREFIX = "P: ";
  @NonNull private final String packageName;
  @NonNull private final String className;

  private RecheckJob(@NonNull String packageName, @NonNull String className, long delay) {
    super(delay, TAG_PACKAGE_PREFIX + packageName, TAG_CLASS_PREFIX + className);
    this.packageName = packageName;
    this.className = className;
  }

  @CheckResult @NonNull
  public static Job create(@NonNull String packageName, @NonNull String className, long delay) {
    return new RecheckJob(packageName, className, delay);
  }

  @Override public void onRun() throws Throwable {
    PadLockService.recheck(packageName, className);
  }
}
