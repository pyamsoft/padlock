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

package com.pyamsoft.padlock.app.lock;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.CheckResult;
import android.support.annotation.ColorRes;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.View;
import com.pyamsoft.padlock.app.base.AppIconLoaderPresenter;
import com.pyamsoft.padlock.app.base.AppIconLoaderView;

public interface LockViewDelegate extends AppIconLoaderView {

  @NonNull String ENTRY_PACKAGE_NAME = "entry_packagename";
  @NonNull String ENTRY_ACTIVITY_NAME = "entry_activityname";

  @CheckResult @NonNull String getCurrentAttempt();

  @CheckResult @NonNull String getPackageName();

  @CheckResult @NonNull String getActivityName();

  void setTextColor(@ColorRes int color);

  void clearDisplay();

  void onCreateView(@NonNull LockPresenter presenter, @NonNull Activity activity,
      @NonNull View rootView);

  void onCreateView(@NonNull LockPresenter presenter, @NonNull Fragment fragment,
      @NonNull View rootView);

  void onStart(@NonNull AppIconLoaderPresenter presenter);

  void onDestroyView();

  void onRestoreInstanceState(@NonNull Bundle savedInstanceState);

  void onSaveInstanceState(@NonNull Bundle outState);
}
