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

package com.pyamsoft.padlock.app.main;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import com.pyamsoft.padlock.model.event.AgreeTermsEvent;
import com.pyamsoft.pydroid.tool.RxBus;

public class AgreeTermsDialog extends DialogFragment {

  @NonNull public static final String TAG = "AgreeTermsDialog";

  @Override public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setCancelable(false);
  }

  @NonNull @Override public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
    return new AlertDialog.Builder(getActivity()).setTitle("Notice")
        .setMessage(
            "By using PadLock, you assume the responsibility of remembering your passcode for the applications you choose to protect."
                + " By continuing, you agree that forgetting the passcode used to lock your applications can lead to parts of your device becoming inaccessible,"
                + " and that pyamsoft can not be held liable.")
        .setPositiveButton("I Understand", (dialogInterface, i) -> {
          dialogInterface.dismiss();
          Bus.get().post(AgreeTermsEvent.create(true));
        })
        .setNegativeButton("Cancel", (dialogInterface, i) -> {
          dialogInterface.dismiss();
          Bus.get().post(AgreeTermsEvent.create(false));
        })
        .setCancelable(false)
        .create();
  }

  public static final class Bus extends RxBus<AgreeTermsEvent> {

    @NonNull private static final Bus instance = new Bus();

    @CheckResult @NonNull public static Bus get() {
      return instance;
    }
  }
}
