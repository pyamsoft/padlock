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

package com.pyamsoft.padlock.app.list;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.pyamsoft.padlock.app.iconloader.AppIconLoaderPresenter;
import com.pyamsoft.padlock.app.iconloader.AppIconLoaderView;
import com.pyamsoft.padlock.model.ActivityEntry;
import com.pyamsoft.padlock.model.LockState;

public interface LockInfoPresenter extends AppIconLoaderPresenter<LockInfoPresenter.LockInfoView> {

  int GROUP_POSITION = -1;

  void populateList(@NonNull String packageName);

  void setToggleAllState(@NonNull String packageName);

  void modifyDatabaseEntry(boolean isNotDefault, int position, @NonNull String packageName,
      @NonNull String activityName, @Nullable String code, boolean system, boolean whitelist,
      boolean forceDelete);

  void modifyDatabaseGroup(boolean allCreate, @NonNull String packageName, @Nullable String code,
      boolean system);

  interface LockInfoView extends LockListCommon, AppIconLoaderView, LockListDatabaseErrorView,
      LockListDatabaseWhitelistView {

    void onEntryAddedToList(@NonNull ActivityEntry entry);

    void enableToggleAll();

    void disableToggleAll();

    void processDatabaseModifyEvent(int position, @NonNull String activityName,
        @NonNull LockState previousLockState, @NonNull LockState newLockState);
  }
}
