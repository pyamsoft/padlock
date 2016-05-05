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
package com.pyamsoft.padlock.app.lock.delegate;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import com.pyamsoft.padlock.R;
import com.pyamsoft.padlock.app.lock.LockPresenter;
import com.pyamsoft.padlock.app.lock.LockView;
import timber.log.Timber;

public final class LockViewDelegateImpl<I extends LockView, P extends LockPresenter<I>>
    implements LockViewDelegate {

  @NonNull private static final String CODE_DISPLAY = "CODE_DISPLAY";

  @NonNull private final P presenter;
  @NonNull private final TextView.OnEditorActionListener editActionListener;

  @BindView(R.id.lock_image) ImageView image;
  @BindView(R.id.lock_text_entry) EditText editText;

  private String activityName;
  private String packageName;
  private Unbinder unbinder;

  public LockViewDelegateImpl(final @NonNull P presenter,
      @NonNull final TextView.OnEditorActionListener editorActionListener) {
    this.presenter = presenter;
    this.editActionListener = editorActionListener;
  }

  @Override public void onCreate(final Activity activity, final View rootView) {
    onCreate(rootView, activity.getIntent().getExtras());
  }

  @Override public void onCreate(final Fragment fragment, final View rootView) {
    onCreate(rootView, fragment.getArguments());
  }

  private void onCreate(final View rootView, final Bundle bundle) {
    unbinder = ButterKnife.bind(this, rootView);
    getValuesFromBundle(bundle);

    editText.setOnEditorActionListener(editActionListener);
  }

  private void getValuesFromBundle(Bundle bundle) {
    activityName = bundle.getString(ENTRY_ACTIVITY_NAME);
    packageName = bundle.getString(ENTRY_PACKAGE_NAME);
    Timber.d("Got value activityName: %s", activityName);
    Timber.d("Got value packageName: %s", packageName);
  }

  @Override public void clearDisplay() {
    editText.setText("");
  }

  @Override public void onStart() {
    presenter.loadPackageIcon(packageName);
  }

  @Override public void setImageSuccess(@NonNull Drawable drawable) {
    image.setImageDrawable(drawable);
    clearDisplay();
  }

  @Override public void setImageError() {
    // TODO handle error
  }

  @Override public void onDestroy() {
    if (unbinder != null) {
      unbinder.unbind();
    }
  }

  @NonNull @Override public String getCurrentAttempt() {
    return editText.getText().toString();
  }

  @NonNull @Override public String getPackageName() {
    return packageName;
  }

  @NonNull @Override public String getActivityName() {
    return activityName;
  }

  @Override public void onRestoreInstanceState(Bundle savedInstanceState) {
    Timber.d("onRestoreInstanceState");
    final String attempt = savedInstanceState.getString(CODE_DISPLAY, null);
    if (attempt == null) {
      clearDisplay();
    } else {
      editText.setText(attempt);
    }
  }

  @Override public void onSaveInstanceState(Bundle outState) {
    Timber.d("onSaveInstanceState");
    final String attempt = getCurrentAttempt();
    outState.putString(CODE_DISPLAY, attempt);
  }
}
