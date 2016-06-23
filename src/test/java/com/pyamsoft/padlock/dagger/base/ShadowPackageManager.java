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

package com.pyamsoft.padlock.dagger.base;

import android.content.pm.ApplicationInfo;
import android.graphics.drawable.Drawable;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import com.pyamsoft.padlock.R;
import java.util.ArrayList;
import java.util.List;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.ShadowsAdapter;
import org.robolectric.res.builder.DefaultPackageManager;
import org.robolectric.res.builder.RobolectricPackageManager;

final class ShadowPackageManager extends DefaultPackageManager
    implements RobolectricPackageManager {

  ShadowPackageManager(ShadowsAdapter shadowsAdapter) {
    super(shadowsAdapter);

    // TODO add packages to PM
  }

  @Override public int[] getPackageGids(String s, int i) throws NameNotFoundException {
    System.out.println("Overriden method getPackageGids is STUB");
    return new int[0];
  }

  @Override public boolean hasSystemFeature(String s, int i) {
    System.out.println("Overriden method hasSystemFeature is STUB");
    return false;
  }

  @NonNull @CheckResult @Override public List<ApplicationInfo> getInstalledApplications(int flags) {
    final List<ApplicationInfo> list = new ArrayList<>();

    ApplicationInfo applicationInfo = new ApplicationInfo();
    applicationInfo.flags = 0;
    applicationInfo.enabled = true;
    applicationInfo.packageName = "com.example.test";
    list.add(applicationInfo);

    applicationInfo = new ApplicationInfo();
    applicationInfo.flags = ApplicationInfo.FLAG_SYSTEM;
    applicationInfo.enabled = true;
    applicationInfo.packageName = "com.system.test";
    list.add(applicationInfo);

    applicationInfo = new ApplicationInfo();
    applicationInfo.flags = ApplicationInfo.FLAG_SYSTEM;
    applicationInfo.enabled = false;
    applicationInfo.packageName = "com.system.disabled";
    list.add(applicationInfo);

    applicationInfo = new ApplicationInfo();
    applicationInfo.flags = 0;
    applicationInfo.enabled = false;
    applicationInfo.packageName = "com.example.disabled";
    list.add(applicationInfo);
    return list;
  }

  @Override public Drawable getDefaultActivityIcon() {
    return ContextCompat.getDrawable(RuntimeEnvironment.application.getApplicationContext(),
        R.drawable.google_play);
  }
}
