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
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.google.android.gms.common.GoogleApiAvailability;
import com.pyamsoft.padlock.app.receiver.ApplicationInstallReceiver;
import com.pyamsoft.padlock.dagger.DaggerPadLockComponent;
import com.pyamsoft.padlock.dagger.PadLockComponent;
import com.pyamsoft.padlock.dagger.PadLockModule;
import com.pyamsoft.pydroid.IPYDroidApp;
import com.pyamsoft.pydroid.SingleInitContentProvider;
import com.pyamsoft.pydroid.about.Licenses;

public class PadLockSingleInitProvider extends SingleInitContentProvider
    implements IPYDroidApp<PadLockComponent> {

  @Nullable private static volatile PadLockSingleInitProvider instance = null;
  @Nullable private PadLockComponent component;

  @NonNull @CheckResult public static IPYDroidApp<PadLockComponent> get() {
    if (instance == null) {
      throw new NullPointerException("Instance is NULL");
    }

    //noinspection ConstantConditions
    return instance;
  }

  private static void setInstance(@NonNull PadLockSingleInitProvider instance) {
    PadLockSingleInitProvider.instance = instance;
  }

  @Override protected void onFirstCreate(@NonNull Context context) {
    super.onFirstCreate(context);
    component = DaggerPadLockComponent.builder().padLockModule(new PadLockModule(context)).build();
  }

  @Override protected void onInstanceCreated(@NonNull Context context) {
    setInstance(this);
    final PadLockComponent comp = provideComponent();
    final ApplicationInstallReceiver receiver = comp.provideApplicationInstallReceiver();
    final PadLockPreferences preferences = comp.providePreferences();
    if (preferences.isInstallListenerEnabled()) {
      receiver.register();
    } else {
      receiver.unregister();
    }
  }

  @Nullable @Override public String provideGoogleOpenSourceLicenses(@NonNull Context context) {
    return GoogleApiAvailability.getInstance().getOpenSourceSoftwareLicenseInfo(context);
  }

  @Override public void insertCustomLicensesIntoMap() {
    Licenses.create("Android Priority Job Queue",
        "https://github.com/yigit/android-priority-jobqueue", "licenses/androidpriorityjobqueue");
    Licenses.create("SQLBrite", "https://github.com/square/sqlbrite", "licenses/sqlbrite");
    Licenses.create("SQLDelight", "https://github.com/square/sqldelight", "licenses/sqldelight");
  }

  @NonNull @Override public PadLockComponent provideComponent() {
    if (component == null) {
      throw new NullPointerException("Component is NULL");
    }

    return component;
  }
}
