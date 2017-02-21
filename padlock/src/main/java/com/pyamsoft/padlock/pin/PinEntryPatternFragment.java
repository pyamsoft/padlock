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

package com.pyamsoft.padlock.pin;

import android.os.Bundle;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.pyamsoft.padlock.Injector;
import com.pyamsoft.padlock.databinding.FragmentPinEntryPatternBinding;
import com.pyamsoft.pydroid.FuncNone;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.inject.Inject;
import me.zhanghai.android.patternlock.PatternView;
import timber.log.Timber;

public class PinEntryPatternFragment extends PinEntryBaseFragment {

  @NonNull static final String TAG = "PinEntryPatternFragment";
  @NonNull private static final String REPEAT_CELL_PATTERN = "repeat_cell_pattern";
  @NonNull private static final String PATTERN_TEXT = "pattern_text";
  static int MINIMUM_PATTERN_LENGTH = 4;
  @NonNull final List<PatternView.Cell> cellPattern = new ArrayList<>();
  @NonNull final List<PatternView.Cell> repeatCellPattern = new ArrayList<>();
  boolean repeatPattern = false;
  FragmentPinEntryPatternBinding binding;
  @Inject PinEntryPresenter presenter;
  @Nullable FuncNone<Boolean> nextButtonOnClickRunnable;
  @NonNull String patternText = "";

  @NonNull @CheckResult static String cellPatternToString(@NonNull List<PatternView.Cell> cells) {
    StringBuilder builder = new StringBuilder(4);
    for (PatternView.Cell cell : cells) {
      String cellString =
          String.format(Locale.getDefault(), "%s%s", cell.getRow(), cell.getColumn());
      builder.append(cellString);
    }
    return builder.toString();
  }

  @Override public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Injector.get().provideComponent().plusPinEntryComponent().inject(this);

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
    binding.unbind();
  }

  @Override public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    binding.patternLock.setOnPatternListener(new PatternView.OnPatternListener() {

      private void clearPattern() {
        if (repeatPattern) {
          repeatCellPattern.clear();
        } else {
          cellPattern.clear();
        }
      }

      @Override public void onPatternStart() {
        binding.patternLock.setDisplayMode(PatternView.DisplayMode.Correct);
        clearPattern();
      }

      @Override public void onPatternCleared() {
        clearPattern();
      }

      @Override public void onPatternCellAdded(List<PatternView.Cell> pattern) {
      }

      @Override public void onPatternDetected(List<PatternView.Cell> pattern) {
        if (!repeatPattern) {
          if (pattern.size() < MINIMUM_PATTERN_LENGTH) {
            binding.patternLock.setDisplayMode(PatternView.DisplayMode.Wrong);
          }
        }

        Timber.d("onPatternDetected");
        List<PatternView.Cell> cellList;
        if (repeatPattern) {
          cellList = repeatCellPattern;
        } else {
          cellList = cellPattern;
        }
        cellList.clear();
        cellList.addAll(pattern);
      }
    });
  }

  @Override public void onSaveInstanceState(Bundle outState) {
    outState.putBoolean(REPEAT_CELL_PATTERN, repeatPattern);
    outState.putString(PATTERN_TEXT, patternText);
    super.onSaveInstanceState(outState);
  }

  @Override public void onStart() {
    super.onStart();
    presenter.bindView(null);

    // Pattern gets visually screwed up in multiwindow mode, clear it
    binding.patternLock.clearPattern();

    presenter.checkMasterPinPresent(new PinEntryPresenter.MasterPinStatusCallback() {
      @Override public void onMasterPinMissing() {
        nextButtonOnClickRunnable = () -> {
          if (repeatPattern) {
            // Submit
            String repeatText = cellPatternToString(repeatCellPattern);
            submitPin(repeatText);
            // No follow up acton
            return false;
          } else {
            // process and show next
            if (cellPattern.size() < MINIMUM_PATTERN_LENGTH) {
              binding.patternLock.setDisplayMode(PatternView.DisplayMode.Wrong);
              return false;
            } else {
              patternText = cellPatternToString(cellPattern);
              repeatPattern = true;
              binding.patternLock.clearPattern();
              return true;
            }
          }
        };
      }

      @Override public void onMasterPinPresent() {
        nextButtonOnClickRunnable = () -> {
          patternText = cellPatternToString(cellPattern);
          binding.patternLock.clearPattern();
          submitPin(null);
          return false;
        };
      }
    });
  }

  @Override public void onStop() {
    super.onStop();
    presenter.unbindView();
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
        actOnLockList(callback -> {
          if (creating) {
            callback.onCreateMasterPinSuccess();
          } else {
            callback.onClearMasterPinSuccess();
          }
        });
        dismissParent();
      }

      @Override public void onSubmitFailure(boolean creating) {
        actOnLockList(callback -> {
          if (creating) {
            callback.onCreateMasterPinFailure();
          } else {
            callback.onClearMasterPinFailure();
          }
        });
        dismissParent();
      }

      @Override public void onSubmitError() {
        dismissParent();
      }
    });
  }
}
