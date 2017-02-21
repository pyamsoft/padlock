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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.pyamsoft.padlock.databinding.PinEntryPatternBinding;
import java.util.ArrayList;
import java.util.List;
import me.zhanghai.android.patternlock.PatternView;
import timber.log.Timber;

public class PinEntryPatternFragment extends PinEntryBaseFragment {

  @NonNull static final String TAG = "PinEntryPatternFragment";
  @NonNull private static final String REPEAT_CELL_PATTER = "repeat_cell_pattern";
  @NonNull final List<PatternView.Cell> cellPattern = new ArrayList<>();
  @NonNull final List<PatternView.Cell> repeatCellPattern = new ArrayList<>();
  boolean repeatPattern = false;
  private PinEntryPatternBinding binding;

  @Override public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    repeatPattern =
        savedInstanceState != null && savedInstanceState.getBoolean(REPEAT_CELL_PATTER, false);
  }

  @Nullable @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    binding = PinEntryPatternBinding.inflate(inflater, container, false);
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
        clearPattern();
      }

      @Override public void onPatternCleared() {
        clearPattern();
      }

      @Override public void onPatternCellAdded(List<PatternView.Cell> pattern) {
      }

      @Override public void onPatternDetected(List<PatternView.Cell> pattern) {
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
    outState.putBoolean(REPEAT_CELL_PATTER, repeatPattern);
    super.onSaveInstanceState(outState);
  }

  @Override public void onStart() {
    super.onStart();
    binding.patternLock.clearPattern();
  }
}
