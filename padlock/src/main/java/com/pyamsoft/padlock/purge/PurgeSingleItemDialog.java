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

package com.pyamsoft.padlock.purge;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import com.pyamsoft.padlock.PadLock;
import com.pyamsoft.pydroid.bus.EventBus;

public class PurgeSingleItemDialog extends DialogFragment {

  @NonNull private static final String PACKAGE = "package_name";
  String packageName;

  @CheckResult @NonNull
  public static PurgeSingleItemDialog newInstance(@NonNull String packageName) {
    final Bundle args = new Bundle();
    final PurgeSingleItemDialog fragment = new PurgeSingleItemDialog();
    args.putString(PACKAGE, packageName);
    fragment.setArguments(args);
    return fragment;
  }

  @Override public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    packageName = getArguments().getString(PACKAGE, null);
    if (packageName == null) {
      throw new NullPointerException("Package Name is NULL");
    }
  }

  @NonNull @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
    return new AlertDialog.Builder(getActivity()).setMessage(
        "Really delete old entry for " + packageName + "?")
        .setPositiveButton("Delete", (dialogInterface, i) -> {
          EventBus.get().publish(PurgeEvent.create(packageName));
          dismiss();
        })
        .setNegativeButton("Cancel", (dialogInterface, i) -> dismiss())
        .create();
  }

  @Override public void onDestroy() {
    super.onDestroy();
    PadLock.getRefWatcher(this).watch(this);
  }
}
