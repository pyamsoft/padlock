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

import android.support.annotation.NonNull;
import com.birbit.android.jobqueue.JobManager;
import com.birbit.android.jobqueue.scheduling.FrameworkJobSchedulerService;
import com.pyamsoft.padlock.PadLockSingleInitProvider;
import com.pyamsoft.padlock.app.wrapper.JobSchedulerCompat;
import javax.inject.Inject;

public class PadLockFrameworkJobSchedulerService extends FrameworkJobSchedulerService {

  @Inject JobSchedulerCompat jobSchedulerCompat;

  @NonNull @Override protected JobManager getJobManager() {
    return jobSchedulerCompat.provideManagerToService(this);
  }

  @Override public void onCreate() {
    super.onCreate();
    PadLockSingleInitProvider.get().provideComponent().plusJobComponent().inject(this);
  }
}
