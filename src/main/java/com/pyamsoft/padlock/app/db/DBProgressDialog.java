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
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import com.pyamsoft.padlock.model.AppEntry;
import com.pyamsoft.pydroid.tool.RxBus;
import com.pyamsoft.pydroid.util.AppUtil;

public final class DBProgressDialog extends DialogFragment {

  @NonNull private static final String DB_PROGRESS_TAG = "db_progress";
  @NonNull private static final String APP_NAME = "app_name";
  private String name;

  private static DBProgressDialog newInstance(final String name) {
    final DBProgressDialog fragment = new DBProgressDialog();
    final Bundle args = new Bundle();
    args.putString(APP_NAME, name);
    fragment.setArguments(args);
    return fragment;
  }

  public static void add(final @NonNull FragmentManager fragmentManager,
      final @NonNull String name) {
    AppUtil.guaranteeSingleDialogFragment(fragmentManager, newInstance(name), DB_PROGRESS_TAG);
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

  public static class Bus extends RxBus<Bus.DisplayEvent> {

    @NonNull private static final Bus instance = new Bus();

    @CheckResult @NonNull public static Bus get() {
      return instance;
    }

    public static class DisplayEvent {
      private final int position;
      private final boolean checked;
      @NonNull private final AppEntry entry;

      public DisplayEvent(int position, boolean checked, @NonNull AppEntry entry) {
        this.position = position;
        this.checked = checked;
        this.entry = entry;
      }

      @CheckResult public int getPosition() {
        return position;
      }

      @CheckResult public boolean isChecked() {
        return checked;
      }

      @CheckResult @NonNull public AppEntry getEntry() {
        return entry;
      }
    }
  }
}
