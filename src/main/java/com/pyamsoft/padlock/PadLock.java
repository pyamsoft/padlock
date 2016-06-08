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

import android.app.Activity;
import android.app.Application;
import android.app.Service;
import android.os.StrictMode;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import com.pyamsoft.padlock.dagger.DaggerPadLockComponent;
import com.pyamsoft.padlock.dagger.PadLockComponent;
import com.pyamsoft.padlock.dagger.PadLockModule;
import com.pyamsoft.pydroid.base.app.ApplicationBase;
import com.pyamsoft.pydroid.crash.CrashHandler;

public final class PadLock extends ApplicationBase {

  @Nullable private PadLockComponent padLockComponent;

  @NonNull @CheckResult
  private static PadLockComponent padLockComponent(final Application application) {
    if (application instanceof PadLock) {
      final PadLock padLock = (PadLock) application;
      final PadLockComponent component = padLock.padLockComponent;

      assert component != null;
      return component;
    } else {
      throw new ClassCastException("Cannot cast Application to PadLock");
    }
  }

  @NonNull @CheckResult public static PadLockComponent padLockComponent(final Activity activity) {
    return padLockComponent(activity.getApplication());
  }

  @NonNull @CheckResult public static PadLockComponent padLockComponent(final Fragment fragment) {
    return padLockComponent(fragment.getActivity());
  }

  @NonNull @CheckResult public static PadLockComponent padLockComponent(final Service service) {
    return padLockComponent(service.getApplication());
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
  }

  private void setStrictMode() {
    StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().detectAll()
        .penaltyLog()
        .penaltyDeath()
        .permitDiskReads()
        .penaltyFlashScreen()
        .build());
    StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().detectAll().penaltyLog().build());
  }
}
