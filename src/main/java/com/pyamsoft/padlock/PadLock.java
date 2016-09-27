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
import com.pyamsoft.padlock.dagger.DaggerPadLockComponent;
import com.pyamsoft.padlock.dagger.PadLockComponent;
import com.pyamsoft.padlock.dagger.PadLockModule;
import com.pyamsoft.pydroid.IPYDroidApp;
import com.pyamsoft.pydroid.PYDroidApplication;
import com.pyamsoft.pydroid.about.Licenses;

public class PadLock extends PYDroidApplication implements IPYDroidApp<PadLockComponent> {

  private PadLockComponent component;

  @NonNull @CheckResult public static IPYDroidApp<PadLockComponent> get(@NonNull Context context) {
    final Context appContext = context.getApplicationContext();
    if (appContext instanceof PadLock) {
      return PadLock.class.cast(appContext);
    } else {
      throw new ClassCastException("Cannot cast Application Context to IPadLock");
    }
  }

  @Override protected void createApplicationComponents() {
    super.createApplicationComponents();
    component = DaggerPadLockComponent.builder()
        .padLockModule(new PadLockModule(getApplicationContext()))
        .build();
  }

  @NonNull @Override public PadLockComponent provideComponent() {
    if (component == null) {
      throw new NullPointerException("PadLockComponent is NULL");
    }
    return component;
  }

  @Nullable @Override public String provideGoogleOpenSourceLicenses() {
    return GoogleApiAvailability.getInstance().getOpenSourceSoftwareLicenseInfo(this);
  }

  @Override public void insertCustomLicensesIntoMap() {
    Licenses.create("Android Priority Job Queue",
        "https://github.com/yigit/android-priority-jobqueue", "licenses/androidpriorityjobqueue");
    Licenses.create("SQLBrite", "https://github.com/square/sqlbrite", "licenses/sqlbrite");
    Licenses.create("SQLDelight", "https://github.com/square/sqldelight", "licenses/sqldelight");
  }
}
