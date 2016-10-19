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

package com.pyamsoft.padlock.dagger.receiver;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import com.pyamsoft.padlock.app.receiver.ApplicationInstallReceiver;
import com.pyamsoft.padlock.dagger.PadLockDB;
import javax.inject.Inject;
import timber.log.Timber;

class ApplicationInstallReceiverImpl extends BroadcastReceiver
    implements ApplicationInstallReceiver {

  @NonNull private final IntentFilter filter;
  @NonNull private final Context appContext;
  @NonNull private final PadLockDB padlockDb;
  private boolean registered;

  @Inject ApplicationInstallReceiverImpl(@NonNull Context context, @NonNull PadLockDB padLockDB) {
    appContext = context.getApplicationContext();
    padlockDb = padLockDB;
    filter = new IntentFilter(Intent.ACTION_PACKAGE_ADDED);
    filter.addDataScheme("package");
    registered = false;
  }

  @Override @CheckResult public boolean isEnabled() {
    final ComponentName cmp = new ComponentName(appContext, ApplicationInstallReceiverImpl.class);
    final int componentState = appContext.getPackageManager().getComponentEnabledSetting(cmp);
    return componentState == PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
  }

  @Override public void setEnabled(boolean enabled) {
    final ComponentName cmp = new ComponentName(appContext, ApplicationInstallReceiverImpl.class);
    final int componentState = enabled ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        : PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
    appContext.getPackageManager()
        .setComponentEnabledSetting(cmp, componentState, PackageManager.DONT_KILL_APP);
  }

  @Override public void onReceive(Context context, Intent intent) {
    if (intent == null) {
      Timber.e("NULL Intent");
      return;
    }

    final Uri data = intent.getData();
    final String packageName = data.getSchemeSpecificPart();
    Timber.d("Package Added: %s", packageName);
  }

  @Override public void register() {
    if (!registered) {
      if (isEnabled()) {
        appContext.registerReceiver(this, filter);
        registered = true;
      } else {
        Timber.e("Cannot register, not enabled in Manifest");
      }
    }
  }

  @Override public void unregister() {
    if (registered) {
      appContext.unregisterReceiver(this);
      registered = false;
    }
  }
}
