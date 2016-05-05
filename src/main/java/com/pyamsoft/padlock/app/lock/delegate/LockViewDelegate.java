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
package com.pyamsoft.padlock.app.lock.delegate;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.ColorRes;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.View;
import com.pyamsoft.padlock.app.lock.LockPresenter;

public interface LockViewDelegate {

  @NonNull String ENTRY_PACKAGE_NAME = "entry_packagename";
  @NonNull String ENTRY_ACTIVITY_NAME = "entry_activityname";

  @NonNull String getCurrentAttempt();

  @NonNull String getPackageName();

  @NonNull String getActivityName();

  void setTextColor(@ColorRes int color);

  void clearDisplay();

  void setImageSuccess(@NonNull Drawable drawable);

  void setImageError();

  void onCreateView(LockPresenter presenter, Activity activity, View rootView);

  void onCreateView(LockPresenter presenter, Fragment fragment, View rootView);

  void onStart(LockPresenter presenter);

  void onDestroyView();

  void onRestoreInstanceState(Bundle savedInstanceState);

  void onSaveInstanceState(Bundle outState);
}
