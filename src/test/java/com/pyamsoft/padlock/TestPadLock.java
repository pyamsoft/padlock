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
import android.content.Context;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import com.pyamsoft.padlock.dagger.DaggerTestPadLockComponent;
import com.pyamsoft.padlock.dagger.TestPadLockComponent;
import com.pyamsoft.padlock.dagger.TestPadLockModule;
import com.pyamsoft.padlock.dagger.wrapper.TestPackageManagerWrapperModule;

public class TestPadLock extends Application implements IPadLock<TestPadLockComponent> {

  private TestPadLockComponent component;

  @NonNull @CheckResult public static IPadLock<TestPadLockComponent> get(@NonNull Context context) {
    final Context appContext = context.getApplicationContext();
    if (appContext instanceof IPadLock) {
      return TestPadLock.class.cast(appContext);
    } else {
      throw new ClassCastException("Cannot cast Application Context to IPadLock");
    }
  }

  @Override public void onCreate() {
    super.onCreate();
    component = DaggerTestPadLockComponent.builder()
        .testPackageManagerWrapperModule(new TestPackageManagerWrapperModule())
        .testPadLockModule(new TestPadLockModule(getApplicationContext()))
        .build();
  }

  @NonNull @Override public TestPadLockComponent provideComponent() {
    if (component == null) {
      throw new NullPointerException("TestPadLockComponent is NULL");
    }
    return component;
  }
}
