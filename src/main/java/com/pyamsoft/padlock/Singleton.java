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

package com.pyamsoft.padlock;

import android.content.Context;
import android.os.Build;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import com.birbit.android.jobqueue.JobManager;
import com.birbit.android.jobqueue.config.Configuration;
import com.birbit.android.jobqueue.network.NetworkUtil;
import com.birbit.android.jobqueue.scheduling.FrameworkJobSchedulerService;
import com.birbit.android.jobqueue.scheduling.GcmJobSchedulerService;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.pyamsoft.padlock.app.service.job.PadLockFrameworkJobSchedulerService;
import com.pyamsoft.padlock.app.service.job.PadLockGCMJobSchedulerService;
import com.pyamsoft.padlock.dagger.DaggerPadLockComponent;
import com.pyamsoft.padlock.dagger.PadLockComponent;
import com.pyamsoft.padlock.dagger.PadLockModule;
import timber.log.Timber;

public class Singleton {

  private Singleton() {
    throw new RuntimeException("No instances");
  }

  public static final class Jobs {

    private static volatile Jobs instance = null;
    @NonNull private final JobManager jobManager;

    private Jobs(@NonNull Context context) {
      this.jobManager = createJobManager(context.getApplicationContext());
    }

    @CheckResult @NonNull public static JobManager with(@NonNull Context context) {
      if (instance == null) {
        synchronized (Jobs.class) {
          if (instance == null) {
            instance = new Jobs(context.getApplicationContext());
          }
        }
      }

      if (instance == null) {
        throw new NullPointerException("Jobs instance is NULL");
      } else {
        return instance.getJobManager();
      }
    }

    @CheckResult @NonNull static JobManager createJobManager(@NonNull Context appContext) {
      final Configuration.Builder builder =
          new Configuration.Builder(appContext).minConsumerCount(1)
              .maxConsumerCount(4)
              .loadFactor(4)
              .consumerKeepAlive(120);
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        Timber.d("Create scheduler using JobScheduler framework");
        builder.scheduler(FrameworkJobSchedulerService.createSchedulerFor(appContext,
            PadLockFrameworkJobSchedulerService.class));
      } else {
        final int googleAvailable =
            GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(appContext);
        if (googleAvailable == ConnectionResult.SUCCESS) {
          Timber.d("Create scheduler using Google play services");

          // Batch by default
          builder.scheduler(GcmJobSchedulerService.createSchedulerFor(appContext,
              PadLockGCMJobSchedulerService.class));
        } else {
          Timber.e("Could not create a scheduler to use with the JobScheduler");
        }
      }

      // We don't actually use the network
      builder.networkUtil(context -> NetworkUtil.DISCONNECTED);

      Timber.d("Create a new JobManager");
      return new JobManager(builder.build());
    }

    @NonNull @CheckResult JobManager getJobManager() {
      return jobManager;
    }
  }

  public static final class Dagger {

    private static volatile Dagger instance = null;
    @NonNull private final PadLockComponent component;

    private Dagger(@NonNull Context context) {
      component = DaggerPadLockComponent.builder()
          .padLockModule(new PadLockModule(context.getApplicationContext()))
          .build();
    }

    @CheckResult @NonNull public static PadLockComponent with(@NonNull Context context) {
      if (instance == null) {
        synchronized (Dagger.class) {
          if (instance == null) {
            instance = new Dagger(context.getApplicationContext());
          }
        }
      }

      if (instance == null) {
        throw new NullPointerException("Dagger instance is NULL");
      } else {
        return instance.component;
      }
    }
  }
}
