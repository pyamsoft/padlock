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

package com.pyamsoft.padlock.lock;

import android.os.Bundle;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.pyamsoft.padlock.databinding.FragmentLockScreenPatternBinding;
import com.pyamsoft.padlock.list.ErrorDialog;
import com.pyamsoft.padlock.service.PadLockService;
import com.pyamsoft.padlock.uicommon.LockCellUtils;
import com.pyamsoft.pydroid.util.AppUtil;
import java.util.List;
import me.zhanghai.android.patternlock.PatternView;
import timber.log.Timber;

import static com.pyamsoft.padlock.lock.LockScreenActivity.ENTRY_LOCK_UNTIL_TIME;

public class LockScreenPatternFragment extends LockScreenBaseFragment {

  @NonNull static final String TAG = "LockScreenPatternFragment";
  FragmentLockScreenPatternBinding binding;

  @CheckResult @NonNull
  public static LockScreenPatternFragment newInstance(@NonNull String lockedPackageName,
      @NonNull String lockedActivityName, @Nullable String lockedCode,
      @NonNull String lockedRealName, boolean lockedSystem) {
    LockScreenPatternFragment fragment = new LockScreenPatternFragment();
    fragment.setArguments(
        buildBundle(lockedPackageName, lockedActivityName, lockedCode, lockedRealName,
            lockedSystem));
    return fragment;
  }

  @Nullable @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    binding = FragmentLockScreenPatternBinding.inflate(inflater, container, false);
    return binding.getRoot();
  }

  @Override public void onDestroyView() {
    super.onDestroyView();
    binding.unbind();
  }

  @Override public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    LockSubmitCallback submitCallback = new LockSubmitCallback() {

      @NonNull final LockScreenPresenter.LockErrorCallback lockErrorCallback = () -> {
        Timber.e("LOCK ERROR");
        AppUtil.guaranteeSingleDialogFragment(getActivity(), new ErrorDialog(), "lock_error");
      };

      @Override public void onSubmitSuccess() {
        Timber.d("Unlocked!");
        presenter.postUnlock(getLockedPackageName(), getLockedActivityName(), getLockedRealName(),
            getLockedCode(), isLockedSystem(), isExcluded(), getSelectedIgnoreTime(),
            new LockScreenEntryPresenter.PostUnlockCallback() {
              @Override public void onPostUnlock() {
                Timber.d("POST Unlock Finished! 1");
                PadLockService.passLockScreen();
                getActivity().finish();
              }

              @Override public void onLockedError() {
                lockErrorCallback.onLockedError();
              }
            });
      }

      @Override public void onSubmitFailure() {
        Timber.e("Failed to unlock");
        showSnackbarWithText("Error: Invalid PIN");

        // Once fail count is tripped once, continue to update it every time following until time elapses
        presenter.lockEntry(getLockedPackageName(), getLockedActivityName(),
            new LockScreenEntryPresenter.LockCallback() {
              @Override public void onLocked(long lockUntilTime) {
                setLockedUntilTime(lockUntilTime);
                getActivity().getIntent().removeExtra(ENTRY_LOCK_UNTIL_TIME);
                getActivity().getIntent().putExtra(ENTRY_LOCK_UNTIL_TIME, lockUntilTime);
                showSnackbarWithText("This entry is temporarily locked");
              }

              @Override public void onLockedError() {
                lockErrorCallback.onLockedError();
              }
            });
      }

      @Override public void onSubmitError() {
        AppUtil.guaranteeSingleDialogFragment(getActivity(), new ErrorDialog(), "unlock_error");
      }
    };

    binding.patternLock.setOnPatternListener(new PatternView.OnPatternListener() {
      @Override public void onPatternStart() {

      }

      @Override public void onPatternCleared() {

      }

      @Override public void onPatternCellAdded(List<PatternView.Cell> pattern) {

      }

      @Override public void onPatternDetected(List<PatternView.Cell> pattern) {
        presenter.submit(getLockedPackageName(), getLockedActivityName(), getLockedCode(),
            getLockedUntilTime(), LockCellUtils.cellPatternToString(pattern), submitCallback);
        binding.patternLock.clearPattern();
      }
    });
  }

  @Override public void onStart() {
    super.onStart();
    binding.patternLock.clearPattern();
  }
}
