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

package com.pyamsoft.padlock.app.receiver;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import timber.log.Timber;

public class ApplicationInstallReceiver extends BroadcastReceiver {

  @NonNull private final IntentFilter filter;
  private boolean registered;

  public ApplicationInstallReceiver() {
    filter = new IntentFilter(Intent.ACTION_PACKAGE_ADDED);
    filter.addDataScheme("package");
    registered = false;
  }

  public static void setEnabled(final Context c, final boolean bootEnabled) {
    final Context context = c.getApplicationContext();
    final ComponentName cmp = new ComponentName(context, ApplicationInstallReceiver.class);
    final int componentState = bootEnabled ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        : PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
    context.getPackageManager()
        .setComponentEnabledSetting(cmp, componentState, PackageManager.DONT_KILL_APP);
  }

  @CheckResult public static boolean isEnabled(final Context c) {
    final Context context = c.getApplicationContext();
    final ComponentName cmp = new ComponentName(context, ApplicationInstallReceiver.class);
    final int componentState = context.getPackageManager().getComponentEnabledSetting(cmp);
    return componentState == PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
  }

  @Override public void onReceive(Context context, Intent intent) {
    if (intent == null) {
      Timber.e("NULL Intent");
      return;
    }

    final Uri data = intent.getData();
    Timber.d("Package Added DATA: %s", data);
  }

  public void register(@NonNull Context context) {
    if (!registered) {
      final Context appContext = context.getApplicationContext();
      if (isEnabled(appContext)) {
        appContext.registerReceiver(this, filter);
        registered = true;
      } else {
        Timber.e("Cannot register, not enabled in Manifest");
      }
    }
  }

  public void unregister(@NonNull Context context) {
    if (registered) {
      final Context appContext = context.getApplicationContext();
      appContext.unregisterReceiver(this);
      registered = false;
    }
  }
}
