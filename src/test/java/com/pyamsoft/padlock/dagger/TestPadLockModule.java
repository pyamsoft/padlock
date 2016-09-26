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

package com.pyamsoft.padlock.dagger;

import android.content.Context;
import android.support.annotation.NonNull;
import com.birbit.android.jobqueue.JobManager;
import com.pyamsoft.padlock.PadLockPreferences;
import com.pyamsoft.padlock.dagger.wrapper.JobSchedulerCompat;
import dagger.Module;
import dagger.Provides;
import javax.inject.Named;
import org.mockito.Mockito;
import rx.Scheduler;
import rx.schedulers.Schedulers;

@Module public class TestPadLockModule {

  @NonNull private final Context appContext;

  public TestPadLockModule(@NonNull Context context) {
    this.appContext = context.getApplicationContext();
  }

  @Provides Context provideContext() {
    return appContext;
  }

  @Named("computation") @Provides Scheduler provideIOScheduler() {
    return Schedulers.computation();
  }

  @Named("main") @Provides Scheduler provideMainThreadScheduler() {
    return Schedulers.immediate();
  }

  @Provides PadLockPreferences providePreferences() {
    final PadLockPreferences mockPreferences = Mockito.mock(PadLockPreferences.class);
    return mockPreferences;
  }

  @Provides JobSchedulerCompat provideJobSchedulerCompat() {
    // KLUDGE Should not really be mocking this, we dont own it
    final JobSchedulerCompat mockJobSchedulerCompat = Mockito.mock(JobSchedulerCompat.class);
    return mockJobSchedulerCompat;
  }

  @Provides PadLockDB providePadLockDB() {
    final PadLockDB mockPadLockDB = Mockito.mock(PadLockDB.class);
    return mockPadLockDB;
  }
}
