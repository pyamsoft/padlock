/*
 * Copyright 2017 Peter Kenji Yamanaka
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
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import com.pyamsoft.padlock.base.PadLockModule;
import com.pyamsoft.padlock.base.preference.InstallListenerPreferences;
import com.pyamsoft.padlock.base.receiver.ApplicationInstallReceiver;
import com.pyamsoft.padlock.lock.LockHelper;
import com.pyamsoft.padlock.lock.LockScreenActivity;
import com.pyamsoft.padlock.lock.SHA256LockHelper;
import com.pyamsoft.padlock.main.MainActivity;
import com.pyamsoft.padlock.service.RecheckService;
import com.pyamsoft.pydroid.about.Licenses;
import com.pyamsoft.pydroid.ui.PYDroid;
import com.squareup.leakcanary.LeakCanary;
import com.squareup.leakcanary.RefWatcher;

public class PadLock extends Application implements ComponentProvider {

  @Nullable private RefWatcher refWatcher;
  @Nullable private PadLockComponent component;

  @CheckResult @NonNull public static RefWatcher getRefWatcher(@NonNull Fragment fragment) {
    return getRefWatcherInternal(fragment);
  }

  @CheckResult @NonNull
  private static RefWatcher getRefWatcherInternal(@NonNull Fragment fragment) {
    final Application application = fragment.getActivity().getApplication();
    if (application instanceof PadLock) {
      return ((PadLock) application).getWatcher();
    } else {
      throw new IllegalStateException("Application is not PadLock");
    }
  }

  @Override public void onCreate() {
    super.onCreate();
    if (LeakCanary.isInAnalyzerProcess(this)) {
      return;
    }
    PYDroid.initialize(this, BuildConfig.DEBUG);
    Licenses.create("SQLBrite", "https://github.com/square/sqlbrite", "licenses/sqlbrite");
    Licenses.create("SQLDelight", "https://github.com/square/sqldelight", "licenses/sqldelight");
    Licenses.create("Dagger", "https://github.com/google/dagger", "licenses/dagger2");
    Licenses.create("Firebase", "https://firebase.google.com", "licenses/firebase");
    Licenses.create("PatternLockView", "https://github.com/aritraroy/PatternLockView",
        "licenses/patternlock");

    LockHelper.Companion.set(SHA256LockHelper.newInstance());
    final PadLockModule padLockModule =
        new PadLockModule(getApplicationContext(), MainActivity.class, LockScreenActivity.class,
            RecheckService.class);
    final PadLockComponent dagger =
        DaggerPadLockComponent.builder().padLockModule(padLockModule).build();
    Injector.set(dagger);

    final ApplicationInstallReceiver receiver = dagger.provideApplicationInstallReceiver();
    final InstallListenerPreferences preferences = dagger.provideInstallListenerPreferences();
    if (preferences.isInstallListenerEnabled()) {
      receiver.register();
    } else {
      receiver.unregister();
    }
    Injector.set(dagger);
    component = dagger;

    if (BuildConfig.DEBUG) {
      refWatcher = LeakCanary.install(this);
    } else {
      refWatcher = RefWatcher.DISABLED;
    }
  }

  @NonNull @CheckResult private RefWatcher getWatcher() {
    if (refWatcher == null) {
      throw new IllegalStateException("RefWatcher is NULL");
    }
    return refWatcher;
  }

  @NonNull @Override public PadLockComponent getComponent() {
    final PadLockComponent obj = component;
    if (obj == null) {
      throw new IllegalStateException("PadLockComponent must be initialized before use");
    } else {
      return obj;
    }
  }
}
