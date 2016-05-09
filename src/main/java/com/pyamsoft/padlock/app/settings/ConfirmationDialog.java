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

package com.pyamsoft.padlock.app.settings;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import com.pyamsoft.padlock.model.event.ConfirmationEvent;
import com.pyamsoft.padlock.model.event.RxBus;
import com.pyamsoft.pydroid.base.RetainedDialogFragment;

public class ConfirmationDialog extends RetainedDialogFragment {
  private static final String WHICH = "which_type";

  private int which;

  public static ConfirmationDialog newInstance(final int which) {
    final ConfirmationDialog fragment = new ConfirmationDialog();
    final Bundle args = new Bundle();
    args.putInt(WHICH, which);
    fragment.setArguments(args);
    return fragment;
  }

  @Override public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    which = getArguments().getInt(WHICH, 0);
  }

  @NonNull @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
    return new AlertDialog.Builder(getActivity()).setMessage(
        which == 0 ? "Really clear entire database?" : "Really clear all application settings?")
        .setPositiveButton("Yes", (dialogInterface, i) -> {
          dialogInterface.dismiss();
          ConfirmationDialogBus.get().post(ConfirmationEvent.builder().type(which).build());
        })
        .setNegativeButton("No", (dialogInterface, i) -> {
          dialogInterface.dismiss();
        })
        .create();
  }

  public static final class ConfirmationDialogBus extends RxBus<ConfirmationEvent> {

    private static final ConfirmationDialogBus instance = new ConfirmationDialogBus();

    public static ConfirmationDialogBus get() {
      return instance;
    }
  }
}
