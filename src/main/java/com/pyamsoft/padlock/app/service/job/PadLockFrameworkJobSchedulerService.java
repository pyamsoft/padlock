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

package com.pyamsoft.padlock.app.service.job;

import android.annotation.SuppressLint;
import android.support.annotation.NonNull;
import com.birbit.android.jobqueue.JobManager;
import com.birbit.android.jobqueue.scheduling.FrameworkJobSchedulerService;
import com.pyamsoft.padlock.PadLock;
import javax.inject.Inject;

@SuppressLint("Registered") public class PadLockFrameworkJobSchedulerService
    extends FrameworkJobSchedulerService {

  @Inject JobManager jobManager;

  @NonNull @Override protected JobManager getJobManager() {
    return jobManager;
  }

  @Override public void onCreate() {
    super.onCreate();
    PadLock.get(this).provideComponent().plusJobComponent().inject(this);
  }
}
