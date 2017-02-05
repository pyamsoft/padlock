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

import android.app.Application;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import com.pyamsoft.pydroid.PYDroidApplication;
import com.squareup.leakcanary.LeakCanary;
import com.squareup.leakcanary.RefWatcher;

public class PadLock extends PYDroidApplication {

  @Nullable private RefWatcher refWatcher;

  @CheckResult @NonNull public static RefWatcher getRefWatcher(@NonNull Fragment fragment) {
    final Application application = fragment.getActivity().getApplication();
    if (application instanceof PadLock) {
      return ((PadLock) application).getWatcher();
    } else {
      throw new IllegalStateException("Application is not PadLock");
    }
  }

  @Override protected boolean exitBeforeInitialization() {
    return LeakCanary.isInAnalyzerProcess(this);
  }

  @Override protected void onCreateInDebugMode() {
    super.onCreateInDebugMode();
    refWatcher = LeakCanary.install(this);
  }

  @Override protected void onCreateInReleaseMode() {
    super.onCreateInReleaseMode();
    refWatcher = RefWatcher.DISABLED;
  }

  @NonNull @CheckResult private RefWatcher getWatcher() {
    if (refWatcher == null) {
      throw new IllegalStateException("RefWatcher is NULL");
    }
    return refWatcher;
  }
}
