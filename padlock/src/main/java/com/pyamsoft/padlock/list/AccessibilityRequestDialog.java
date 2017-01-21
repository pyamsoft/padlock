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

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import com.pyamsoft.padlock.PadLock;
import com.pyamsoft.padlock.R;
import com.pyamsoft.padlock.uicommon.AccessibilityRequestDelegate;

public class AccessibilityRequestDialog extends DialogFragment {

  @NonNull private final AccessibilityRequestDelegate delegate = new AccessibilityRequestDelegate();

  @NonNull @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
    return new AlertDialog.Builder(getActivity()).setTitle("Enable PadLock AccessibilityService")
        .setMessage(R.string.explain_accessibility_service)
        .setPositiveButton("Let's Go", (dialogInterface, i) -> {
          onPositionButtonClicked();
          dismiss();
        })
        .setNegativeButton("No Thanks", (dialogInterface, i) -> dismiss())
        .create();
  }

  void onPositionButtonClicked() {
    delegate.launchAccessibilityIntent(getActivity());
  }

  @Override public void onDestroy() {
    super.onDestroy();
    PadLock.getRefWatcher(this).watch(this);
  }
}
