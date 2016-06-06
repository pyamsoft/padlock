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

package com.pyamsoft.padlock.app.lock;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;

public final class InfoDialog extends DialogFragment {

  @NonNull private static final String PKG_NAME = "pkgname";
  @NonNull private static final String ACT_NAME = "actname";

  @Nullable private String activityName;
  @Nullable private String packageName;

  public static InfoDialog newInstance(final @NonNull String packageName,
      final @NonNull String activityName) {
    final InfoDialog fragment = new InfoDialog();
    final Bundle args = new Bundle();
    args.putString(PKG_NAME, packageName);
    args.putString(ACT_NAME, activityName);
    fragment.setArguments(args);
    return fragment;
  }

  @Override public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    packageName = getArguments().getString(PKG_NAME);
    activityName = getArguments().getString(ACT_NAME);
  }

  @NonNull @Override public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
    return new AlertDialog.Builder(getActivity()).setTitle("Locked Info")
        .setMessage(packageName + '\n' + activityName)
        .setPositiveButton("Okay", (dialogInterface, i) -> {
          dialogInterface.dismiss();
        })
        .setCancelable(true)
        .create();
  }
}
