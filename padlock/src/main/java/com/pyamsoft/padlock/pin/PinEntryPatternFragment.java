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

package com.pyamsoft.padlock.pin;

import android.os.Bundle;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.andrognito.patternlockview.PatternLockView;
import com.andrognito.patternlockview.listener.PatternLockViewListener;
import com.pyamsoft.padlock.Injector;
import com.pyamsoft.padlock.PadLock;
import com.pyamsoft.padlock.databinding.FragmentPinEntryPatternBinding;
import com.pyamsoft.padlock.uicommon.LockCellUtils;
import com.pyamsoft.pydroid.bus.EventBus;
import com.pyamsoft.pydroid.function.FuncNone;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import timber.log.Timber;

public class PinEntryPatternFragment extends PinEntryBaseFragment {

  @NonNull static final String TAG = "PinEntryPatternFragment";
  @NonNull private static final String REPEAT_CELL_PATTERN = "repeat_cell_pattern";
  @NonNull private static final String PATTERN_TEXT = "pattern_text";
  static int MINIMUM_PATTERN_LENGTH = 4;
  @NonNull final List<PatternLockView.Dot> cellPattern = new ArrayList<>();
  @NonNull final List<PatternLockView.Dot> repeatCellPattern = new ArrayList<>();
  boolean repeatPattern = false;
  FragmentPinEntryPatternBinding binding;
  @Inject PinEntryPresenter presenter;
  @Nullable FuncNone<Boolean> nextButtonOnClickRunnable;
  @NonNull String patternText = "";
  @Nullable private PatternLockViewListener listener;

  @Override public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Injector.get().provideComponent().inject(this);

    if (savedInstanceState == null) {
      repeatPattern = false;
      patternText = "";
    } else {
      repeatPattern = savedInstanceState.getBoolean(REPEAT_CELL_PATTERN, false);
      patternText = savedInstanceState.getString(PATTERN_TEXT, "");
    }
  }

  @Nullable @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    binding = FragmentPinEntryPatternBinding.inflate(inflater, container, false);
    return binding.getRoot();
  }

  @Override public void onDestroyView() {
    super.onDestroyView();
    if (listener != null) {
      binding.patternLock.removePatternLockListener(listener);
      listener = null;
    }
    binding.unbind();
  }

  @Override public void onDestroy() {
    super.onDestroy();
    presenter.destroy();
    PadLock.getRefWatcher(this).watch(this);
  }

  @Override public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    listener = new PatternLockViewListener() {

      private void clearPattern() {
        if (repeatPattern) {
          repeatCellPattern.clear();
        } else {
          cellPattern.clear();
        }
      }

      @Override public void onStarted() {
        binding.patternLock.setViewMode(PatternLockView.PatternViewMode.CORRECT);
        clearPattern();
      }

      @Override public void onProgress(List<PatternLockView.Dot> list) {

      }

      @Override public void onComplete(List<PatternLockView.Dot> list) {
        if (!repeatPattern) {
          if (list.size() < MINIMUM_PATTERN_LENGTH) {
            binding.patternLock.setViewMode(PatternLockView.PatternViewMode.WRONG);
          }
        }

        Timber.d("onPatternDetected");
        List<PatternLockView.Dot> cellList;
        if (repeatPattern) {
          cellList = repeatCellPattern;
        } else {
          cellList = cellPattern;
        }
        cellList.clear();
        cellList.addAll(list);
      }

      @Override public void onCleared() {
        clearPattern();
      }
    };

    binding.patternLock.setTactileFeedbackEnabled(false);
    binding.patternLock.addPatternLockListener(listener);
  }

  @Override public void onSaveInstanceState(Bundle outState) {
    outState.putBoolean(REPEAT_CELL_PATTERN, repeatPattern);
    outState.putString(PATTERN_TEXT, patternText);
    super.onSaveInstanceState(outState);
  }

  @Override public void onStart() {
    super.onStart();

    // Pattern gets visually screwed up in multiwindow mode, clear it
    binding.patternLock.clearPattern();

    presenter.checkMasterPinPresent(new PinEntryPresenter.MasterPinStatusCallback() {
      @Override public void onMasterPinMissing() {
        nextButtonOnClickRunnable = () -> {
          if (repeatPattern) {
            // Submit
            String repeatText = LockCellUtils.cellPatternToString(repeatCellPattern);
            submitPin(repeatText);
            // No follow up acton
            return false;
          } else {
            // process and show next
            if (cellPattern.size() < MINIMUM_PATTERN_LENGTH) {
              binding.patternLock.setViewMode(PatternLockView.PatternViewMode.WRONG);
              return false;
            } else {
              patternText = LockCellUtils.cellPatternToString(cellPattern);
              repeatPattern = true;
              binding.patternLock.clearPattern();
              return true;
            }
          }
        };
      }

      @Override public void onMasterPinPresent() {
        nextButtonOnClickRunnable = () -> {
          patternText = LockCellUtils.cellPatternToString(cellPattern);
          binding.patternLock.clearPattern();
          submitPin(null);
          return false;
        };
      }
    });
  }

  @Override public void onStop() {
    super.onStop();
    presenter.stop();
  }

  @CheckResult boolean onNextButtonClicked() {
    if (nextButtonOnClickRunnable == null) {
      Timber.w("onClick runnable is NULL");
      return false;
    } else {
      return nextButtonOnClickRunnable.call();
    }
  }

  void submitPin(@Nullable String repeatText) {
    if (repeatText == null) {
      repeatText = "";
    }

    presenter.submit(patternText, repeatText, "", new PinEntryPresenter.SubmitCallback() {

      @Override public void onSubmitSuccess(boolean creating) {
        if (creating) {
          EventBus.get().publish(CreatePinEvent.create(true));
        } else {
          EventBus.get().publish(ClearPinEvent.create(true));
        }
        dismissParent();
      }

      @Override public void onSubmitFailure(boolean creating) {
        if (creating) {
          EventBus.get().publish(CreatePinEvent.create(false));
        } else {
          EventBus.get().publish(ClearPinEvent.create(false));
        }
        dismissParent();
      }

      @Override public void onSubmitError() {
        dismissParent();
      }
    });
  }
}
