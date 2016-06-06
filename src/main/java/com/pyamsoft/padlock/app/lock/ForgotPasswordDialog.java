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

public final class ForgotPasswordDialog extends DialogFragment {

  @NonNull @Override public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
    return new AlertDialog.Builder(getActivity()).setTitle("Forgot Passcode")
        .setMessage(
            "If the unfortunate happens and you forget your PadLock passcode you are put in a tough spot.\n\n"
                + "One thing you can attempt to do would be to go to the Settings of your phone and attempt to clear all of the application data."
                + " If you cannot perform this because you have locked Settings as well, you can attempt to uninstall PadLock via the Andrid Debug Bridge (adb):\n\n"
                + "adb uninstall com.pyamsoft.padlock")
        .setPositiveButton("Okay", (dialogInterface, i) -> {
          dialogInterface.dismiss();
        })
        .setCancelable(true)
        .create();
  }
}
