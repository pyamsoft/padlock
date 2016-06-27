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

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.CheckResult;
import android.support.annotation.ColorRes;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputEditText;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import com.pyamsoft.padlock.R;
import com.pyamsoft.padlock.app.base.AppIconLoaderPresenter;
import com.pyamsoft.padlock.app.base.AppIconLoaderView;
import com.pyamsoft.padlock.app.base.ErrorDialog;
import com.pyamsoft.pydroid.model.AsyncDrawable;
import com.pyamsoft.pydroid.tool.AsyncVectorDrawableTask;
import com.pyamsoft.pydroid.util.AppUtil;
import javax.inject.Inject;
import timber.log.Timber;

public final class LockViewDelegate implements AppIconLoaderView {

  @NonNull public static final String ENTRY_PACKAGE_NAME = "entry_packagename";
  @NonNull public static final String ENTRY_ACTIVITY_NAME = "entry_activityname";
  @NonNull private static final String CODE_DISPLAY = "CODE_DISPLAY";

  @BindView(R.id.lock_image) ImageView image;
  @BindView(R.id.lock_text_entry) TextInputEditText editText;
  @BindView(R.id.lock_image_go) ImageView imageGo;

  private @ColorRes int textColor;
  private FragmentManager fragmentManager;
  private View rootView;
  private String activityName;
  private String packageName;
  private AsyncVectorDrawableTask arrowGoTask;
  private Unbinder unbinder;
  private InputMethodManager imm;
  Callback callback;

  @Inject public LockViewDelegate() {
    this.textColor = R.color.orange500;
  }

  public final void setTextColor(@ColorRes int textColor) {
    this.textColor = textColor;
  }

  public final void onCreateView(@NonNull Callback callback, @NonNull FragmentActivity activity,
      @NonNull View rootView) {
    fragmentManager = activity.getSupportFragmentManager();
    onCreateView(callback, rootView, activity.getIntent().getExtras());
  }

  public final void onCreateView(@NonNull Callback callback, @NonNull Fragment fragment,
      @NonNull View rootView) {
    fragmentManager = fragment.getFragmentManager();
    onCreateView(callback, rootView, fragment.getArguments());
  }

  private void onCreateView(@NonNull Callback callback, @NonNull View rootView,
      @NonNull Bundle bundle) {
    Timber.d("bindView");
    this.callback = callback;
    this.rootView = rootView;
    unbinder = ButterKnife.bind(this, rootView);
    getValuesFromBundle(bundle);

    editText.setOnEditorActionListener((textView, actionId, keyEvent) -> {
      if (keyEvent == null) {
        Timber.e("KeyEvent was not caused by keypress");
        return false;
      }

      if (keyEvent.getAction() == KeyEvent.ACTION_DOWN && actionId == EditorInfo.IME_NULL) {
        Timber.d("KeyEvent is Enter pressed");
        callback.onSubmitPressed();
        return true;
      }

      Timber.d("Do not handle key event");
      return false;
    });

    editText.setTextColor(ContextCompat.getColor(rootView.getContext(), textColor));

    // Force the keyboard
    imm = (InputMethodManager) rootView.getContext()
        .getApplicationContext()
        .getSystemService(Context.INPUT_METHOD_SERVICE);
    imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);

    imageGo.setOnClickListener(view -> {
      callback.onSubmitPressed();
      imm.toggleSoftInputFromWindow(rootView.getWindowToken(), 0, 0);
    });

    // Force keyboard focus
    editText.requestFocus();

    editText.setOnFocusChangeListener((view, hasFocus) -> {
      if (hasFocus) {
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
      }
    });

    // Load the go arrow
    if (arrowGoTask != null) {
      arrowGoTask.cancel(true);
    }

    arrowGoTask = new AsyncVectorDrawableTask(imageGo);
    arrowGoTask.execute(new AsyncDrawable(rootView.getContext().getApplicationContext(),
        R.drawable.ic_arrow_forward_24dp));

    clearDisplay();
  }

  private void getValuesFromBundle(@NonNull Bundle bundle) {
    activityName = bundle.getString(ENTRY_ACTIVITY_NAME);
    packageName = bundle.getString(ENTRY_PACKAGE_NAME);
    Timber.d("Got value activityName: %s", activityName);
    Timber.d("Got value packageName: %s", packageName);
  }

  public final void clearDisplay() {
    editText.setText("");
  }

  public final void onStart(@NonNull final AppIconLoaderPresenter presenter) {
    presenter.loadApplicationIcon(packageName);
  }

  public final void onDestroyView() {
    Timber.d("unbindView");
    if (arrowGoTask != null) {
      arrowGoTask.cancel(true);
    }

    unbinder.unbind();
    callback = null;
    imm.toggleSoftInputFromWindow(rootView.getWindowToken(), 0, 0);
    rootView = null;
  }

  @CheckResult @NonNull public String getCurrentAttempt() {
    return editText.getText().toString();
  }

  @CheckResult @NonNull public String getAppPackageName() {
    return packageName;
  }

  @CheckResult @NonNull public String getAppActivityName() {
    return activityName;
  }

  public final void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
    Timber.d("onRestoreInstanceState");
    final String attempt = savedInstanceState.getString(CODE_DISPLAY, null);
    if (attempt == null) {
      Timber.d("Empty attempt");
      clearDisplay();
    } else {
      Timber.d("Set attempt %s", attempt);
      editText.setText(attempt);
    }
  }

  public final void onSaveInstanceState(@NonNull Bundle outState) {
    Timber.d("onSaveInstanceState");
    final String attempt = getCurrentAttempt();
    if (!attempt.isEmpty()) {
      outState.putString(CODE_DISPLAY, attempt);
    } else {
      outState.remove(CODE_DISPLAY);
    }
  }

  @Override public void onApplicationIconLoadedError() {
    AppUtil.guaranteeSingleDialogFragment(fragmentManager, new ErrorDialog(), "error");
  }

  @Override public void onApplicationIconLoadedSuccess(@NonNull Drawable icon) {
    image.setImageDrawable(icon);
  }

  public interface Callback {

    void onSubmitPressed();
  }
}
