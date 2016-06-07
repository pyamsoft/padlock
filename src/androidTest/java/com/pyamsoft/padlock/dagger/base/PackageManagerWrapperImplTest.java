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
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.support.v4.content.ContextCompat;
import com.pyamsoft.padlock.R;
import com.pyamsoft.padlock.app.base.PackageManagerWrapper;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class PackageManagerWrapperImplTest {

  @Test public void test_packageManagerHasDefaultIcon() {
    final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    final PackageManager packageManager = context.getPackageManager();

    Drawable defaultIcon = packageManager.getDefaultActivityIcon();
    Assert.assertNotNull(defaultIcon);
  }

  @Test public void test_loadApplicationIcon() {
    final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    final PackageManagerWrapper wrapper = new PackageManagerWrapperImpl(context);

    String packageName = "com.pyamsoft.padlock";
    Drawable expectDrawable = ContextCompat.getDrawable(context, R.mipmap.ic_launcher);
    Drawable resultDrawable = wrapper.loadDrawableForPackageOrDefault(packageName);
    Assert.assertEquals(expectDrawable, resultDrawable);
  }
}
