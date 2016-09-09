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
import android.os.Build;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import com.birbit.android.jobqueue.JobManager;
import com.birbit.android.jobqueue.config.Configuration;
import com.birbit.android.jobqueue.network.NetworkUtil;
import com.birbit.android.jobqueue.scheduling.FrameworkJobSchedulerService;
import com.birbit.android.jobqueue.scheduling.GcmJobSchedulerService;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.pyamsoft.padlock.PadLockPreferences;
import com.pyamsoft.padlock.app.service.job.PadLockFrameworkJobSchedulerService;
import com.pyamsoft.padlock.app.service.job.PadLockGCMJobSchedulerService;
import dagger.Module;
import dagger.Provides;
import javax.inject.Named;
import javax.inject.Singleton;
import rx.Scheduler;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import timber.log.Timber;

@Module public class PadLockModule {

  @NonNull private final Context appContext;
  @NonNull private final JobManager jobManager;
  @NonNull private final PadLockDB padLockDB;
  @NonNull private final PadLockPreferences preferences;

  public PadLockModule(final @NonNull Context context) {
    appContext = context.getApplicationContext();
    preferences = new PadLockPreferencesImpl(appContext);
    jobManager = createJobManager(context.getApplicationContext());
    padLockDB = new PadLockDB(appContext, Schedulers.io());
  }

  @VisibleForTesting @CheckResult @NonNull JobManager createJobManager(@NonNull Context context) {
    final Configuration.Builder builder = new Configuration.Builder(context).minConsumerCount(1)
        .maxConsumerCount(4)
        .loadFactor(4)
        .consumerKeepAlive(120);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      Timber.d("Create scheduler using JobScheduler framework");
      builder.scheduler(FrameworkJobSchedulerService.createSchedulerFor(context,
          PadLockFrameworkJobSchedulerService.class));
    } else {
      final int googleAvailable =
          GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context);
      if (googleAvailable == ConnectionResult.SUCCESS) {
        Timber.d("Create scheduler using Google play services");

        // Batch by default
        builder.scheduler(GcmJobSchedulerService.createSchedulerFor(context,
            PadLockGCMJobSchedulerService.class));
      } else {
        Timber.e("Could not create a scheduler to use with the JobScheduler");
      }
    }

    // We don't actually use the network
    builder.networkUtil(context1 -> NetworkUtil.DISCONNECTED);

    Timber.d("Create a new JobManager");
    return new JobManager(builder.build());
  }

  @Singleton @Provides JobManager provideJobManager() {
    return jobManager;
  }

  @Singleton @Provides Context provideContext() {
    return appContext;
  }

  @Singleton @Provides PadLockPreferences providePreferences() {
    return preferences;
  }

  @Singleton @Provides PadLockDB providePadLockDB() {
    return padLockDB;
  }

  @Singleton @Provides @Named("io") Scheduler provideIOScheduler() {
    return Schedulers.io();
  }

  @Singleton @Provides @Named("main") Scheduler provideMainThreadScheduler() {
    return AndroidSchedulers.mainThread();
  }
}
