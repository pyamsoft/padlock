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
import com.pyamsoft.padlock.lock.LockScreenActivity;
import com.pyamsoft.padlock.main.MainActivity;
import com.pyamsoft.padlock.service.RecheckService;
import com.pyamsoft.padlock.presenter.DaggerPadLockComponent;
import com.pyamsoft.padlock.presenter.Injector;
import com.pyamsoft.padlock.presenter.PadLockComponent;
import com.pyamsoft.padlock.presenter.PadLockModule;
import com.pyamsoft.padlock.presenter.PadLockPreferences;
import com.pyamsoft.padlock.presenter.receiver.ApplicationInstallReceiver;
import com.pyamsoft.pydroid.BuildConfigChecker;
import com.pyamsoft.pydroid.IPYDroidApp;
import com.pyamsoft.pydroid.SingleInitContentProvider;
import com.pyamsoft.pydroid.about.Licenses;
import com.pyamsoft.pydroid.rx.RxLicenses;
import com.pyamsoft.pydroid.ui.UiLicenses;

public class PadLockSingleInitProvider extends SingleInitContentProvider
    implements IPYDroidApp<PadLockComponent> {

  @Nullable private PadLockComponent component;

  @Override protected void onFirstCreate(@NonNull Context context) {
    super.onFirstCreate(context);
    final PadLockModule module =
        new PadLockModule(context, MainActivity.class, LockScreenActivity.class,
            RecheckService.class);
    component = DaggerPadLockComponent.builder().padLockModule(module).build();
  }

  @NonNull @Override protected BuildConfigChecker initializeBuildConfigChecker() {
    return new BuildConfigChecker() {
      @Override public boolean isDebugMode() {
        return BuildConfig.DEBUG;
      }
    };
  }

  @Override protected void onInstanceCreated(@NonNull Context context) {
    Injector.set(component);
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
    return null;
  }

  @Override public void insertCustomLicensesIntoMap() {
    Licenses.create("SQLBrite", "https://github.com/square/sqlbrite", "licenses/sqlbrite");
    Licenses.create("SQLDelight", "https://github.com/square/sqldelight", "licenses/sqldelight");
    RxLicenses.addLicenses();
    UiLicenses.addLicenses();
  }

  @NonNull @Override public PadLockComponent provideComponent() {
    if (component == null) {
      throw new NullPointerException("Component is NULL");
    }

    return component;
  }
}
