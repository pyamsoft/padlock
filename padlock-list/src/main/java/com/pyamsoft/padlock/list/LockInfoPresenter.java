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
import com.pyamsoft.padlock.model.ActivityEntry;
import com.pyamsoft.padlock.model.LockState;
import com.pyamsoft.pydroid.presenter.Presenter;

interface LockInfoPresenter extends Presenter<LockInfoPresenter.LockInfoView> {

  void updateCachedEntryLockState(@NonNull String name, @NonNull LockState lockState);

  void clearList();

  void populateList(@NonNull String packageName);

  void modifyDatabaseEntry(boolean isNotDefault, int position, @NonNull String packageName,
      @NonNull String activityName, @SuppressWarnings("SameParameterValue") @Nullable String code,
      boolean system, boolean whitelist, boolean forceDelete);

  void showOnBoarding();

  interface LockInfoView
      extends LockListCommon, LockListDatabaseErrorView, LockListDatabaseWhitelistView {

    void onEntryAddedToList(@NonNull ActivityEntry entry);

    void onShowOnboarding();

    void onOnboardingComplete();
  }
}
