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
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import com.pyamsoft.padlock.R;

public class LockedStatDialog extends DialogFragment {

  @NonNull private static final String PKG_NAME = "pkgname";
  @NonNull private static final String ACT_NAME = "actname";
  @NonNull private static final String REAL_NAME = "realname";
  @NonNull private static final String SYSTEM = "system";
  @NonNull private static final String LABEL = "label";
  @NonNull private static final String IMAGE = "image";

  @BindView(R.id.stat_image) ImageView statImage;
  @BindView(R.id.state_display_name) TextView statDisplayName;
  @BindView(R.id.stat_package_name) TextView statPackageName;
  @BindView(R.id.stat_real_name) TextView statRealName;
  @BindView(R.id.stat_locked_by) TextView statLockedBy;
  @BindView(R.id.stat_system) TextView statSystem;

  private String displayedLabel;
  private String activityName;
  private String packageName;
  private String realName;
  private boolean system;
  @Nullable private Bitmap image;

  private Unbinder unbinder;

  @CheckResult @NonNull public static LockedStatDialog newInstance(@NonNull String displayedLabel,
      @NonNull String packageName, @NonNull String activityName, @NonNull String realName,
      boolean system, @NonNull Drawable drawable) {
    final LockedStatDialog fragment = new LockedStatDialog();
    final Bundle args = new Bundle();
    args.putString(LABEL, displayedLabel);
    args.putString(PKG_NAME, packageName);
    args.putString(ACT_NAME, activityName);
    args.putString(REAL_NAME, realName);
    args.putBoolean(SYSTEM, system);

    if (drawable instanceof BitmapDrawable) {
      final BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
      final Bitmap bitmap = bitmapDrawable.getBitmap();
      args.putParcelable(IMAGE, bitmap);
    }

    fragment.setArguments(args);
    return fragment;
  }

  @Override public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    displayedLabel = getArguments().getString(LABEL);
    packageName = getArguments().getString(PKG_NAME);
    activityName = getArguments().getString(ACT_NAME);
    realName = getArguments().getString(REAL_NAME);
    system = getArguments().getBoolean(SYSTEM);
    image = getArguments().getParcelable(IMAGE);
  }

  @NonNull @Override public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
    final View rootView =
        LayoutInflater.from(getActivity()).inflate(R.layout.dialog_lock_stat, null, false);
    unbinder = ButterKnife.bind(this, rootView);

    statImage.setImageBitmap(image);
    statDisplayName.setText(displayedLabel);
    statPackageName.setText(packageName);
    statRealName.setText(realName);
    statLockedBy.setText(activityName);
    statSystem.setText(system ? "Yes" : "No");

    return new AlertDialog.Builder(getActivity()).setView(rootView)
        .setPositiveButton("Okay", (dialogInterface, i) -> {
          dialogInterface.dismiss();
        })
        .setCancelable(true)
        .create();
  }

  @Override public void onDestroyView() {
    super.onDestroyView();
    unbinder.unbind();
  }
}
