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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.pyamsoft.padlock.base.PadLockModule;
import com.pyamsoft.padlock.base.PadLockPreferences;
import com.pyamsoft.padlock.base.receiver.ApplicationInstallReceiver;
import com.pyamsoft.padlock.lock.LockHelper;
import com.pyamsoft.padlock.lock.LockScreenActivity;
import com.pyamsoft.padlock.lock.SHA256LockHelper;
import com.pyamsoft.padlock.main.MainActivity;
import com.pyamsoft.padlock.service.RecheckService;
import com.pyamsoft.pydroid.about.Licenses;
import com.pyamsoft.pydroid.helper.BuildConfigChecker;
import com.pyamsoft.pydroid.ui.SingleInitContentProvider;

public class PadLockSingleInitProvider extends SingleInitContentProvider {

  @NonNull @Override protected BuildConfigChecker initializeBuildConfigChecker() {
    return new BuildConfigChecker() {
      @Override public boolean isDebugMode() {
        return BuildConfig.DEBUG;
      }
    };
  }

  @Override protected void onInstanceCreated(@NonNull Context context) {
    LockHelper.set(SHA256LockHelper.newInstance());
    final PadLockModule padLockModule =
        new PadLockModule(context.getApplicationContext(), MainActivity.class,
            LockScreenActivity.class, RecheckService.class);
    final PadLockComponent component =
        DaggerPadLockComponent.builder().padLockModule(padLockModule).build();
    Injector.set(component);

    final ApplicationInstallReceiver receiver = component.provideApplicationInstallReceiver();
    final PadLockPreferences preferences = component.providePreferences();
    if (preferences.isInstallListenerEnabled()) {
      receiver.register();
    } else {
      receiver.unregister();
    }
  }

  @Nullable @Override public String provideGoogleOpenSourceLicenses(@NonNull Context context) {
    return null;
  }

  @Override public void insertCustomLicensesIntoMap() {
    super.insertCustomLicensesIntoMap();
    Licenses.create("SQLBrite", "https://github.com/square/sqlbrite", "licenses/sqlbrite");
    Licenses.create("SQLDelight", "https://github.com/square/sqldelight", "licenses/sqldelight");
    Licenses.create("Dagger", "https://github.com/google/dagger", "licenses/dagger2");
  }
}
