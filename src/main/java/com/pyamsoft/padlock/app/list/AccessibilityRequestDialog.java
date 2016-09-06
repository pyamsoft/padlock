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

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import com.pyamsoft.padlock.R;

public class AccessibilityRequestDialog extends DialogFragment {

  @SuppressWarnings("WeakerAccess") @NonNull final Intent accessibilityServiceIntent =
      new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);

  @NonNull @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
    return new AlertDialog.Builder(getActivity()).setTitle("Enable PadLock AccessibilityService")
        .setMessage(R.string.explain_accessibility_service)
        .setPositiveButton("Let's Go", (dialogInterface, i) -> {
          getActivity().startActivity(accessibilityServiceIntent);
          dismiss();
        })
        .setNegativeButton("No Thanks", (dialogInterface, i) -> {
          dismiss();
        })
        .create();
  }
}
