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

package com.pyamsoft.padlock.list;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.pyamsoft.padlock.model.AppEntry;
import com.pyamsoft.pydroid.presenter.Presenter;

interface LockListPresenter extends Presenter<Presenter.Empty> {

  void updateCachedEntryLockState(@NonNull String name, @NonNull String packageName,
      boolean newLockState);

  void clearList();

  void populateList(@NonNull PopulateListCallback callback);

  void setFABStateFromPreference(@NonNull FABStateCallback callback);

  void setSystemVisible();

  void setSystemInvisible();

  void setSystemVisibilityFromPreference(@NonNull SystemVisibilityCallback callback);

  void clickPinFABServiceRunning(@NonNull ShowPinCallback callback);

  void clickPinFABServiceIdle(@NonNull ShowAccessibilityCallback callback);

  void showOnBoarding(@NonNull OnboardingCallback callback);

  void modifyDatabaseEntry(boolean isChecked, int position, @NonNull String packageName,
      @SuppressWarnings("SameParameterValue") @Nullable String code, boolean system,
      @NonNull DatabaseCallback callback);

  interface OnboardingCallback {

    void onShowOnboarding();

    void onOnboardingComplete();
  }

  interface FABStateCallback {

    void onSetFABStateEnabled();

    void onSetFABStateDisabled();
  }

  interface SystemVisibilityCallback {

    void onSetSystemVisible();

    void onSetSystemInvisible();
  }

  interface PopulateListCallback extends LockCommon {

    void onEntryAddedToList(@NonNull AppEntry entry);
  }

  interface DatabaseCallback extends LockDatabaseErrorView {
  }

  interface ShowPinCallback {

    void onCreatePinDialog();
  }

  interface ShowAccessibilityCallback {

    void onCreateAccessibilityDialog();
  }
}
