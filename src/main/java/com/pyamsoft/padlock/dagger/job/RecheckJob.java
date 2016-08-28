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
import android.support.annotation.Nullable;
import com.birbit.android.jobqueue.Job;
import com.birbit.android.jobqueue.Params;
import com.birbit.android.jobqueue.RetryConstraint;
import com.pyamsoft.padlock.app.service.PadLockService;
import java.util.Arrays;
import java.util.Set;
import timber.log.Timber;

public class RecheckJob extends Job {

  public static final int PRIORITY = 1;
  @NonNull public static final String TAG_ALL = "ALL";
  @NonNull public static final String TAG_PACKAGE_PREFIX = "P: ";
  @NonNull public static final String TAG_CLASS_PREFIX = "C: ";

  @NonNull final String packageName;
  @NonNull final String className;

  RecheckJob(@NonNull String packageName, @NonNull String className, long delay) {
    super(new Params(PRIORITY).setDelayMs(delay)
        .setRequiresNetwork(false)
        .addTags(TAG_PACKAGE_PREFIX + packageName, TAG_CLASS_PREFIX + className, TAG_ALL));
    this.packageName = packageName;
    this.className = className;
  }

  @CheckResult @NonNull
  public static Job create(@NonNull String packageName, @NonNull String className, long delay) {
    return new RecheckJob(packageName, className, delay);
  }

  @Override public void onAdded() {
    Timber.d("New recheck job added for: %s %s", packageName, className);
    Timber.d("To run in: %d", getDelayInMs());
  }

  @Override public void onRun() throws Throwable {
    PadLockService.recheck(packageName, className);
  }

  @Override protected void onCancel(int cancelReason, @Nullable Throwable throwable) {
    final Set<String> tags = getTags();
    if (tags != null) {
      Timber.w("Job is cancelled %s %s", getId(), Arrays.toString(tags.toArray()));
    }
    if (throwable != null) {
      Timber.e(throwable, "JOB CANCELLED");
    }
  }

  @Override
  protected RetryConstraint shouldReRunOnThrowable(@NonNull Throwable throwable, int runCount,
      int maxRunCount) {
    Timber.w("Cancel job on retry attempt");
    return RetryConstraint.CANCEL;
  }
}
