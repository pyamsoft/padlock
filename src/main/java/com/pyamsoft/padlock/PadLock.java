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

import android.app.Application;
import android.os.Build;
import android.os.StrictMode;
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
import com.pyamsoft.pydroid.base.app.ApplicationBase;
import com.pyamsoft.pydroid.crash.CrashHandler;
import timber.log.Timber;

public final class PadLock extends ApplicationBase {

  private static volatile PadLock instance = null;
  private PadLockComponent padLockComponent;
  private JobManager jobManager;

  @CheckResult @NonNull public static PadLock getInstance() {
    if (instance == null) {
      throw new NullPointerException("PadLock instance is NULL");
    } else {
      return instance;
    }
  }

  public static void setInstance(PadLock instance) {
    PadLock.instance = instance;
  }

  @CheckResult @NonNull
  private static JobManager createJobManager(@NonNull Application application) {
    final Configuration.Builder builder = new Configuration.Builder(application).minConsumerCount(1)
        .maxConsumerCount(4)
        .loadFactor(4)
        .consumerKeepAlive(120);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      Timber.d("Create scheduler using JobScheduler framework");
      builder.scheduler(FrameworkJobSchedulerService.createSchedulerFor(application,
          PadLockFrameworkJobSchedulerService.class));
    } else {
      final int googleAvailable =
          GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(application);
      if (googleAvailable == ConnectionResult.SUCCESS) {
        Timber.d("Create scheduler using Google play services");

        // Batch by default
        builder.scheduler(GcmJobSchedulerService.createSchedulerFor(application,
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

  @CheckResult @NonNull public final PadLockComponent getPadLockComponent() {
    if (padLockComponent == null) {
      throw new NullPointerException("PadLock component is NULL");
    } else {
      return padLockComponent;
    }
  }

  @NonNull @CheckResult public synchronized final JobManager getJobManager() {
    if (jobManager == null) {
      throw new NullPointerException("JobManager is NULL");
    } else {
      return jobManager;
    }
  }

  @Override protected boolean buildConfigDebug() {
    return BuildConfig.DEBUG;
  }

  @NonNull @Override public String appName() {
    return getString(R.string.app_name);
  }

  @NonNull @Override public String buildConfigApplicationId() {
    return BuildConfig.APPLICATION_ID;
  }

  @NonNull @Override public String buildConfigVersionName() {
    return BuildConfig.VERSION_NAME;
  }

  @Override public int buildConfigVersionCode() {
    return BuildConfig.VERSION_CODE;
  }

  @NonNull @Override public String getApplicationPackageName() {
    return getApplicationContext().getPackageName();
  }

  @Override public String crashLogSubject() {
    return "PadLock Crash Log Report";
  }

  @Override public void onCreate() {
    super.onCreate();

    if (buildConfigDebug()) {
      new CrashHandler(getApplicationContext(), this).register();
      setStrictMode();
    }

    // Init the Dagger graph
    padLockComponent =
        DaggerPadLockComponent.builder().padLockModule(new PadLockModule(this)).build();

    // Job Manager
    jobManager = createJobManager(this);
    Timber.d("Created new JobManager with scheduler: %s", jobManager.getScheduler());

    setInstance(this);
  }

  private void setStrictMode() {
    StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().detectAll()
        .penaltyLog()
        .penaltyDeath()
        .permitDiskReads()
        .permitDiskWrites()
        .penaltyFlashScreen()
        .build());
    StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().detectAll().penaltyLog().build());
  }
}
