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
package com.pyamsoft.padlock.dagger.lock.delegate;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.CheckResult;
import android.support.annotation.ColorRes;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputEditText;
import android.support.v4.app.Fragment;
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
import com.pyamsoft.padlock.app.lock.LockPresenter;
import com.pyamsoft.padlock.app.lock.delegate.LockViewDelegate;
import com.pyamsoft.pydroid.model.AsyncDrawable;
import com.pyamsoft.pydroid.tool.AsyncVectorDrawableTask;
import javax.inject.Inject;
import timber.log.Timber;

final class LockViewDelegateImpl implements LockViewDelegate {

  @NonNull private static final String CODE_DISPLAY = "CODE_DISPLAY";

  @BindView(R.id.lock_image) ImageView image;
  @BindView(R.id.lock_text_entry) TextInputEditText editText;
  @BindView(R.id.lock_image_go) ImageView imageGo;

  private @ColorRes int textColor;
  private View rootView;
  private String activityName;
  private String packageName;
  private AsyncVectorDrawableTask arrowGoTask;
  private Unbinder unbinder;
  private InputMethodManager imm;

  @Inject public LockViewDelegateImpl() {
    this.textColor = R.color.orange500;
  }

  @Override public void setTextColor(@ColorRes int textColor) {
    this.textColor = textColor;
  }

  @Override
  public void onCreateView(@NonNull final LockPresenter presenter, @NonNull final Activity activity,
      @NonNull final View rootView) {
    onCreateView(presenter, rootView, activity.getIntent().getExtras());
  }

  @Override
  public void onCreateView(@NonNull final LockPresenter presenter, @NonNull final Fragment fragment,
      @NonNull final View rootView) {
    onCreateView(presenter, rootView, fragment.getArguments());
  }

  private void onCreateView(final @NonNull LockPresenter presenter, @NonNull final View rootView,
      @NonNull final Bundle bundle) {
    Timber.d("onCreateView");
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
        presenter.submit();
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
      presenter.submit();
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

  @Override public void clearDisplay() {
    editText.setText("");
  }

  @Override public void onStart(@NonNull final LockPresenter presenter) {
    presenter.loadPackageIcon(packageName);
  }

  @Override public void setImageSuccess(@NonNull Drawable drawable) {
    image.setImageDrawable(drawable);
  }

  @Override public void setImageError() {
    // TODO handle error
  }

  @Override public void onDestroyView() {
    Timber.d("onDestroyView");
    if (unbinder != null) {
      unbinder.unbind();
    }
    if (arrowGoTask != null) {
      arrowGoTask.cancel(true);
    }

    imm.toggleSoftInputFromWindow(rootView.getWindowToken(), 0, 0);
    rootView = null;
  }

  @CheckResult @NonNull @Override public String getCurrentAttempt() {
    return editText.getText().toString();
  }

  @CheckResult @NonNull @Override public String getPackageName() {
    return packageName;
  }

  @CheckResult @NonNull @Override public String getActivityName() {
    return activityName;
  }

  @Override public void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
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

  @Override public void onSaveInstanceState(@NonNull Bundle outState) {
    Timber.d("onSaveInstanceState");
    final String attempt = getCurrentAttempt();
    if (!attempt.isEmpty()) {
      outState.putString(CODE_DISPLAY, attempt);
    } else {
      outState.remove(CODE_DISPLAY);
    }
  }
}
