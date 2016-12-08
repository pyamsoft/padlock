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

package com.pyamsoft.padlock.app.service;

import android.app.IntentService;
import android.content.Intent;
import android.support.annotation.NonNull;
import timber.log.Timber;

public class RecheckService extends IntentService {

  @NonNull public static final String EXTRA_PACKAGE_NAME = "extra_package_name";
  @NonNull public static final String EXTRA_CLASS_NAME = "extra_class_name";

  public RecheckService() {
    super(RecheckService.class.getName());
  }

  @Override protected void onHandleIntent(Intent intent) {
    if (intent == null) {
      Timber.e("Intent is NULL");
      return;
    }

    if (!intent.hasExtra(EXTRA_PACKAGE_NAME)) {
      Timber.e("No package name passed");
      return;
    }

    if (!intent.hasExtra(EXTRA_CLASS_NAME)) {
      Timber.e("No class name passed");
      return;
    }

    final String packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME);
    final String className = intent.getStringExtra(EXTRA_CLASS_NAME);
    try {
      PadLockService.recheck(packageName, className);
    } catch (NullPointerException e) {
      Timber.e(e, "ERROR");
    }
  }
}
