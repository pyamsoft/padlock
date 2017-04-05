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

package com.pyamsoft.padlock.settings;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import com.pyamsoft.padlock.PadLock;
import com.pyamsoft.pydroid.bus.EventBus;

public class ConfirmationDialog extends DialogFragment {
  @NonNull private static final String WHICH = "which_type";

  @SuppressWarnings("WeakerAccess") ConfirmEvent.Type type;

  @CheckResult @NonNull
  public static ConfirmationDialog newInstance(@NonNull ConfirmEvent.Type type) {
    final ConfirmationDialog fragment = new ConfirmationDialog();
    final Bundle args = new Bundle();
    args.putString(WHICH, type.name());
    fragment.setArguments(args);
    return fragment;
  }

  @Override public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    type = ConfirmEvent.Type.valueOf(getArguments().getString(WHICH, null));
  }

  @NonNull @Override public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
    return new AlertDialog.Builder(getActivity()).setMessage(type == ConfirmEvent.Type.DATABASE
        ? "Really clear entire database?\n\nYou will have to re-configure all locked applications again"
        : "Really clear all application settings?\n\nYou will have to manually restart the Accessibility Service component of PadLock")
        .setPositiveButton("Yes", (dialog, which) -> {
          EventBus.get().publish(ConfirmEvent.create(type));
          dismiss();
        })
        .setNegativeButton("No", (dialogInterface, i) -> dismiss())
        .create();
  }

  @Override public void onDestroy() {
    super.onDestroy();
    PadLock.getRefWatcher(this).watch(this);
  }
}
