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

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.view.ContextThemeWrapper;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import com.pyamsoft.padlock.PadLock;
import com.pyamsoft.padlock.R;
import com.pyamsoft.padlock.app.base.AppIconLoaderPresenter;
import com.pyamsoft.padlock.model.RxBus;
import com.pyamsoft.padlock.model.event.PinEntryEvent;
import com.pyamsoft.pydroid.model.AsyncDrawable;
import com.pyamsoft.pydroid.tool.AsyncTaskMap;
import com.pyamsoft.pydroid.tool.AsyncVectorDrawableTask;
import javax.inject.Inject;
import timber.log.Timber;

public class PinEntryDialog extends DialogFragment implements PinScreen {

  @NonNull public static final String ENTRY_PACKAGE_NAME = "entry_packagename";
  @NonNull public static final String ENTRY_ACTIVITY_NAME = "entry_activityname";
  @NonNull private static final String CODE_DISPLAY = "CODE_DISPLAY";
  @NonNull private static final String CODE_REENTRY_DISPLAY = "CODE_REENTRY_DISPLAY";
  @NonNull private static final String HINT_DISPLAY = "HINT_DISPLAY";
  @NonNull private final AsyncTaskMap taskMap = new AsyncTaskMap();
  @Inject PinEntryPresenter presenter;
  @Inject AppIconLoaderPresenter<PinScreen> appIconLoaderPresenter;
  @BindView(R.id.pin_entry_toolbar) Toolbar toolbar;
  @BindView(R.id.pin_entry_close) ImageView close;
  @BindView(R.id.pin_image) ImageView image;
  @BindView(R.id.pin_image_go) ImageView go;
  @BindView(R.id.pin_entry_code) TextInputLayout pinEntry;
  @BindView(R.id.pin_reentry_code) TextInputLayout pinReentry;
  @BindView(R.id.pin_hint) TextInputLayout pinHint;
  private Unbinder unbinder;
  private String packageName;
  private InputMethodManager imm;
  private EditText pinEntryText;
  private EditText pinReentryText;
  private EditText pinHintText;

  public static PinEntryDialog newInstance(final @NonNull String packageName,
      final @NonNull String activityName) {
    final PinEntryDialog fragment = new PinEntryDialog();
    final Bundle args = new Bundle();
    args.putString(ENTRY_PACKAGE_NAME, packageName);
    args.putString(ENTRY_ACTIVITY_NAME, activityName);
    fragment.setArguments(args);
    return fragment;
  }

  @Override public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    PadLock.getInstance().getPadLockComponent().plusPinEntry().inject(this);
    packageName = getArguments().getString(ENTRY_PACKAGE_NAME);
    if (packageName == null) {
      throw new NullPointerException("Package name is NULL");
    }

    setCancelable(true);
  }

  @NonNull @Override public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
    Timber.d("Init dialog");
    final ContextWrapper themedContext =
        new ContextThemeWrapper(getContext(), R.style.Theme_PadLock_Light_PINEntry);
    @SuppressLint("InflateParams") final View rootView =
        LayoutInflater.from(themedContext).inflate(R.layout.layout_pin_entry, null, false);
    unbinder = ButterKnife.bind(this, rootView);

    // Resolve TextInputLayout edit texts
    pinEntryText = pinEntry.getEditText();
    if (pinEntryText == null) {
      throw new NullPointerException("No pin entry edit text");
    }

    pinReentryText = pinReentry.getEditText();
    if (pinReentry == null) {
      throw new NullPointerException("No pin re-entry edit text");
    }

    pinHintText = pinHint.getEditText();
    if (pinHintText == null) {
      throw new NullPointerException("No pin hint edit text");
    }

    presenter.bindView(this);
    appIconLoaderPresenter.bindView(this);

    // Force the keyboard
    imm = (InputMethodManager) getContext().getApplicationContext()
        .getSystemService(Context.INPUT_METHOD_SERVICE);
    imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);

    // Only the hint needs to have its IME action set
    setupHintEntry();
    setupGoArrow();

    clearDisplay();

    if (savedInstanceState != null) {
      onRestoreInstanceState(savedInstanceState);
    }

    setupToolbar();
    return new AlertDialog.Builder(getActivity()).setView(rootView).create();
  }

  private void setupHintEntry() {
    pinHintText.setOnEditorActionListener((textView, actionId, keyEvent) -> {
      if (keyEvent == null) {
        Timber.e("KeyEvent was not caused by keypress");
        return false;
      }

      if (keyEvent.getAction() == KeyEvent.ACTION_DOWN && actionId == EditorInfo.IME_NULL) {
        presenter.submit(getCurrentAttempt(), getCurrentReentry(), getCurrentHint());
        Timber.d("KeyEvent is Enter pressed");
        return true;
      }

      Timber.d("Do not handle key event");
      return false;
    });
  }

  private void setupGoArrow() {
    go.setOnClickListener(view -> {
      presenter.submit(getCurrentAttempt(), getCurrentReentry(), getCurrentHint());
      imm.toggleSoftInputFromWindow(getActivity().getWindow().getDecorView().getWindowToken(), 0,
          0);
    });

    // Force keyboard focus
    pinEntryText.requestFocus();

    final AsyncVectorDrawableTask arrowGoTask = new AsyncVectorDrawableTask(go);
    arrowGoTask.execute(
        new AsyncDrawable(getContext().getApplicationContext(), R.drawable.ic_arrow_forward_24dp));
    taskMap.put("arrow", arrowGoTask);
  }

  private void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
    Timber.d("onRestoreInstanceState");
    final String attempt = savedInstanceState.getString(CODE_DISPLAY, null);
    final String reentry = savedInstanceState.getString(CODE_REENTRY_DISPLAY, null);
    final String hint = savedInstanceState.getString(HINT_DISPLAY, null);
    if (attempt == null || reentry == null || hint == null) {
      Timber.d("Empty attempt");
      clearDisplay();
    } else {
      Timber.d("Set attempt %s", attempt);
      pinEntryText.setText(attempt);
      Timber.d("Set reentry %s", reentry);
      pinReentryText.setText(reentry);
      Timber.d("Set hint %s", hint);
      pinHintText.setText(hint);
    }
  }

  @Override public void onSaveInstanceState(@NonNull Bundle outState) {
    Timber.d("onSaveInstanceState");
    outState.putString(CODE_DISPLAY, getCurrentAttempt());
    outState.putString(CODE_REENTRY_DISPLAY, getCurrentReentry());
    outState.putString(HINT_DISPLAY, getCurrentHint());
    super.onSaveInstanceState(outState);
  }

  /**
   * Clear the display of all text entry fields
   */
  private void clearDisplay() {
    pinEntryText.setText("");
    pinReentryText.setText("");
    pinHintText.setText("");
  }

  @CheckResult @NonNull private String getCurrentAttempt() {
    return pinEntryText.getText().toString();
  }

  @CheckResult @NonNull private String getCurrentReentry() {
    return pinReentryText.getText().toString();
  }

  @CheckResult @NonNull private String getCurrentHint() {
    return pinHintText.getText().toString();
  }

  @Override public void onStart() {
    super.onStart();
    appIconLoaderPresenter.loadApplicationIcon(packageName);
  }

  private void setupToolbar() {
    toolbar.setTitle("PIN");

    close.setOnClickListener(view -> {
      Timber.d("onClick Arrow");
      dismiss();
    });
  }

  @Override public void onDestroyView() {
    super.onDestroyView();
    Timber.d("Destroy AlertDialog");
    taskMap.clear();
    imm.toggleSoftInputFromWindow(getActivity().getWindow().getDecorView().getWindowToken(), 0, 0);
    presenter.unbindView();
    appIconLoaderPresenter.unbindView();
    unbinder.unbind();
  }

  @Override public void onApplicationIconLoadedError() {
    Toast.makeText(getActivity(), "Failed to load image", Toast.LENGTH_SHORT).show();
    dismiss();
  }

  @Override public void onApplicationIconLoadedSuccess(@NonNull Drawable icon) {
    image.setImageDrawable(icon);
  }

  @Override public void onSubmitSuccess() {
    clearDisplay();
    dismiss();
  }

  @Override public void onSubmitFailure() {
    clearDisplay();
    dismiss();
  }

  @Override public void onSubmitError() {
    clearDisplay();
    dismiss();
  }

  public static final class PinEntryBus extends RxBus<PinEntryEvent> {

    @NonNull private static final PinEntryBus instance = new PinEntryBus();

    @CheckResult @NonNull public static PinEntryBus get() {
      return instance;
    }
  }
}
