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
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import com.pyamsoft.padlock.PadLock;

public class PurgeAllDialog extends DialogFragment {

  @NonNull @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
    return new AlertDialog.Builder(getActivity()).setMessage("Really delete all old entries?")
        .setPositiveButton("Delete", (dialogInterface, i) -> {
          sendDeleteAllEvent();
          dismiss();
        })
        .setNegativeButton("Cancel", (dialogInterface, i) -> dismiss())
        .create();
  }

  @SuppressWarnings("WeakerAccess") void sendDeleteAllEvent() {
    final FragmentManager fragmentManager = getFragmentManager();
    final Fragment purgeFragment = fragmentManager.findFragmentByTag(PurgeFragment.TAG);
    if (purgeFragment instanceof PurgeFragment) {
      ((PurgeFragment) purgeFragment).purgeAll();
    } else {
      throw new ClassCastException("Fragment is not PurgeFragment");
    }
  }

  @Override public void onDestroy() {
    super.onDestroy();
    PadLock.getRefWatcher(this).watch(this);
  }
}