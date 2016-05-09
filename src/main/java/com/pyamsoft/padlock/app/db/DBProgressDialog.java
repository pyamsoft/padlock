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

package com.pyamsoft.padlock.app.db;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import com.pyamsoft.pydroid.base.RetainedDialogFragment;

public final class DBProgressDialog extends RetainedDialogFragment {

  @NonNull public static final String DB_PROGRESS_TAG = "db_progress";
  @NonNull private static final String APP_NAME = "app_name";
  private String name;

  public static DBProgressDialog newInstance(final String name) {
    final DBProgressDialog fragment = new DBProgressDialog();
    final Bundle args = new Bundle();
    args.putString(APP_NAME, name);
    fragment.setArguments(args);
    return fragment;
  }

  @SuppressLint("CommitTransaction")
  public static void remove(final @NonNull FragmentManager fragmentManager) {
    final FragmentTransaction ft = fragmentManager.beginTransaction();
    final Fragment dialog = fragmentManager.findFragmentByTag(DB_PROGRESS_TAG);
    if (dialog != null) {
      ft.remove(dialog);
    }
    ft.commitAllowingStateLoss();
  }

  @Override public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setCancelable(false);

    name = getArguments().getString(APP_NAME);
  }

  @NonNull @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
    final ProgressDialog dialog = new ProgressDialog(getActivity(), getTheme());
    dialog.setTitle("Please Wait");
    dialog.setMessage("Locking " + name);
    dialog.setIndeterminate(true);
    dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
    return dialog;
  }
}
