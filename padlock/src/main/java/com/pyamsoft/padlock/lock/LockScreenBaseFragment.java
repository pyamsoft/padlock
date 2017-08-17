/*
 * Copyright 2017 Peter Kenji Yamanaka
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

package com.pyamsoft.padlock.lock;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import com.pyamsoft.padlock.Injector;
import javax.inject.Inject;

import static com.pyamsoft.padlock.lock.LockScreenActivity.ENTRY_ACTIVITY_NAME;
import static com.pyamsoft.padlock.lock.LockScreenActivity.ENTRY_IS_SYSTEM;
import static com.pyamsoft.padlock.lock.LockScreenActivity.ENTRY_LOCK_CODE;
import static com.pyamsoft.padlock.lock.LockScreenActivity.ENTRY_PACKAGE_NAME;
import static com.pyamsoft.padlock.lock.LockScreenActivity.ENTRY_REAL_NAME;

public abstract class LockScreenBaseFragment extends Fragment {

  @SuppressWarnings("WeakerAccess") @Inject LockEntryPresenter presenter;
  private String lockedActivityName;
  private String lockedPackageName;
  private String lockedCode;
  private String lockedRealName;
  private boolean lockedSystem;

  @CheckResult @NonNull
  static Bundle buildBundle(@NonNull String lockedPackageName, @NonNull String lockedActivityName,
      @Nullable String lockedCode, @NonNull String lockedRealName, boolean lockedSystem) {
    Bundle args = new Bundle();
    args.putString(ENTRY_PACKAGE_NAME, lockedPackageName);
    args.putString(ENTRY_ACTIVITY_NAME, lockedActivityName);
    args.putString(ENTRY_LOCK_CODE, lockedCode);
    args.putString(ENTRY_REAL_NAME, lockedRealName);
    args.putBoolean(ENTRY_IS_SYSTEM, lockedSystem);
    return args;
  }

  @CheckResult String getLockedActivityName() {
    return lockedActivityName;
  }

  @CheckResult String getLockedPackageName() {
    return lockedPackageName;
  }

  @CheckResult @Nullable String getLockedCode() {
    return lockedCode;
  }

  @CheckResult String getLockedRealName() {
    return lockedRealName;
  }

  @CheckResult boolean isLockedSystem() {
    return lockedSystem;
  }

  void showSnackbarWithText(@NonNull String text) {
    Activity activity = getActivity();
    if (activity instanceof LockScreenActivity) {
      Snackbar.make(((LockScreenActivity) activity).binding.activityLockScreen, text,
          Snackbar.LENGTH_SHORT).show();
    }
  }

  @CheckResult boolean isExcluded() {
    Activity activity = getActivity();
    return activity instanceof LockScreenActivity
        && ((LockScreenActivity) activity).menuExclude.isChecked();
  }

  @CheckResult long getSelectedIgnoreTime() {
    Activity activity = getActivity();
    if (activity instanceof LockScreenActivity) {
      return ((LockScreenActivity) activity).getIgnoreTimeFromSelectedIndex();
    } else {
      return 0;
    }
  }

  @CheckResult long getLockedUntilTime() {
    Activity activity = getActivity();
    if (activity instanceof LockScreenActivity) {
      return ((LockScreenActivity) activity).lockUntilTime;
    } else {
      return 0;
    }
  }

  void setLockedUntilTime(long time) {
    Activity activity = getActivity();
    if (activity instanceof LockScreenActivity) {
      ((LockScreenActivity) activity).lockUntilTime = time;
    }
  }

  @CallSuper @Override public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    final Bundle bundle = getArguments();
    lockedPackageName = bundle.getString(ENTRY_PACKAGE_NAME);
    lockedActivityName = bundle.getString(ENTRY_ACTIVITY_NAME);
    lockedRealName = bundle.getString(ENTRY_REAL_NAME);
    lockedCode = bundle.getString(ENTRY_LOCK_CODE);
    lockedSystem = bundle.getBoolean(ENTRY_IS_SYSTEM, false);

    if (lockedPackageName == null || lockedActivityName == null || lockedRealName == null) {
      throw new NullPointerException("Missing required lock attributes");
    }

    Injector.get().provideComponent().inject(this);
  }

  @CallSuper @Override public void onStop() {
    super.onStop();
    presenter.stop();
  }

  @CallSuper @Override public void onDestroy() {
    super.onDestroy();
    presenter.destroy();
  }
}
