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

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import com.pyamsoft.padlock.BuildConfig;
import com.pyamsoft.padlock.PadLock;
import com.pyamsoft.padlock.app.base.PackageManagerWrapper;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import rx.Observable;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 23, application = PadLock.class)
public class PackageManagerWrapperTest {

  @CheckResult @NonNull public static PackageManagerWrapper getPackageManagerWrapper() {
    RuntimeEnvironment.setRobolectricPackageManager(
        new ShadowPackageManager(RuntimeEnvironment.getSystemResourceLoader()));
    final Context context = RuntimeEnvironment.application.getApplicationContext();
    return new PackageManagerWrapperImpl(context);
  }

  @Test public void test_getActiveApplications() {
    final PackageManagerWrapper packageManagerWrapper = getPackageManagerWrapper();
    final Observable<List<ApplicationInfo>> applicationInfoList =
        packageManagerWrapper.getActiveApplications().toList();

    // List is not null
    Assert.assertNotNull(applicationInfoList);

    // There should be at least one valid package, at least ourselves
    Assert.assertNotEquals(0, applicationInfoList.toBlocking().first().size());

    // Test case gives a fake which has 2 valid applications where one is system
    // System is filtered out at a later point by the presenter / specific interactor
    Assert.assertEquals(2, applicationInfoList.toBlocking().first().size());
  }

  @Test public void test_loadDrawableForPackage() {
    final PackageManagerWrapper packageManagerWrapper = getPackageManagerWrapper();
  }

  @Test public void test_getActivityListForPackage() {
    final PackageManagerWrapper packageManagerWrapper = getPackageManagerWrapper();
  }

  @Test public void test_loadPackageLabel() {
    final PackageManagerWrapper packageManagerWrapper = getPackageManagerWrapper();
  }
}
