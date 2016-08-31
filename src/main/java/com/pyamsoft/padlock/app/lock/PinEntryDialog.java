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
import android.view.ContextThemeWrapper;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import com.pyamsoft.padlock.R;
import com.pyamsoft.pydroid.persist.PersistLoader;
import com.pyamsoft.pydroid.persist.PersistentCache;
import com.pyamsoft.pydroid.tool.AsyncDrawable;
import com.pyamsoft.pydroid.tool.AsyncDrawableMap;
import rx.Subscription;
import timber.log.Timber;

public class PinEntryDialog extends DialogFragment implements PinScreen {

  @NonNull private static final String ENTRY_PACKAGE_NAME = "entry_packagename";
  @NonNull private static final String ENTRY_ACTIVITY_NAME = "entry_activityname";
  @NonNull private static final String CODE_DISPLAY = "CODE_DISPLAY";
  @NonNull private static final String CODE_REENTRY_DISPLAY = "CODE_REENTRY_DISPLAY";
  @NonNull private static final String HINT_DISPLAY = "HINT_DISPLAY";
  @NonNull private static final String KEY_PIN_ENTRY = "key_pin_entry";
  @NonNull private final AsyncDrawableMap taskMap = new AsyncDrawableMap();
  @BindView(R.id.pin_entry_toolbar) TextView toolbar;
  @BindView(R.id.pin_entry_close) ImageView close;
  @BindView(R.id.pin_image) ImageView image;
  @BindView(R.id.pin_image_go) ImageView go;
  @BindView(R.id.pin_entry_code) TextInputLayout pinEntry;
  @BindView(R.id.pin_reentry_code) TextInputLayout pinReentry;
  @BindView(R.id.pin_hint) TextInputLayout pinHint;
  @SuppressWarnings("WeakerAccess") InputMethodManager imm;
  @SuppressWarnings("WeakerAccess") PinEntryPresenter presenter;
  private Unbinder unbinder;
  private String packageName;
  private EditText pinEntryText;
  private EditText pinReentryText;
  private EditText pinHintText;
  private long loadedKey;

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
    packageName = getArguments().getString(ENTRY_PACKAGE_NAME);
    if (packageName == null) {
      throw new NullPointerException("Package name is NULL");
    }

    setCancelable(true);

    loadedKey = PersistentCache.load(savedInstanceState, KEY_PIN_ENTRY,
        new PersistLoader.Callback<PinEntryPresenter>() {
          @NonNull @Override public PersistLoader<PinEntryPresenter> createLoader() {
            return new PinScreenPresenterLoader(getContext());
          }

          @Override public void onPersistentLoaded(@NonNull PinEntryPresenter persist) {
            presenter = persist;
          }
        });
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
    if (pinReentryText == null) {
      throw new NullPointerException("No pin re-entry edit text");
    }

    pinHintText = pinHint.getEditText();
    if (pinHintText == null) {
      throw new NullPointerException("No pin hint edit text");
    }

    // Force the keyboard
    imm = (InputMethodManager) getContext().getApplicationContext()
        .getSystemService(Context.INPUT_METHOD_SERVICE);
    imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);

    setupCloseButton();
    setupGoArrow();
    clearDisplay();
    setupToolbar();

    if (savedInstanceState != null) {
      onRestoreInstanceState(savedInstanceState);
    }

    return new AlertDialog.Builder(getActivity()).setView(rootView).create();
  }

  private void setupCloseButton() {
    close.setOnClickListener(view -> {
      Timber.d("onClick Arrow");
      dismiss();
    });

    final Subscription task = AsyncDrawable.with(getContext())
        .load(R.drawable.ic_close_24dp)
        .tint(android.R.color.black)
        .into(close);
    taskMap.put("close", task);
  }

  @Override public void showExtraPinEntryViews() {
    Timber.d("No active master, show extra views");
    pinReentry.setVisibility(View.VISIBLE);
    pinHint.setVisibility(View.VISIBLE);
    setupSubmissionView(pinHintText);
  }

  @Override public void hideExtraPinEntryViews() {
    Timber.d("Active master, hide extra views");
    pinReentry.setVisibility(View.GONE);
    pinHint.setVisibility(View.GONE);
    setupSubmissionView(pinEntryText);
  }

  private void setupSubmissionView(@NonNull EditText view) {
    view.setOnEditorActionListener((textView, actionId, keyEvent) -> {
      if (keyEvent == null) {
        Timber.e("KeyEvent was not caused by keypress");
        return false;
      }

      if (keyEvent.getAction() == KeyEvent.ACTION_DOWN && actionId == EditorInfo.IME_NULL) {
        Timber.d("KeyEvent is Enter pressed");
        presenter.submit(getCurrentAttempt(), getCurrentReentry(), getCurrentHint());
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

    final Subscription task =
        AsyncDrawable.with(getContext()).load(R.drawable.ic_arrow_forward_24dp).into(go);
    taskMap.put("arrow", task);
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
    PersistentCache.saveKey(outState, loadedKey, KEY_PIN_ENTRY);
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

  @SuppressWarnings("WeakerAccess") @CheckResult @NonNull String getCurrentAttempt() {
    return pinEntryText.getText().toString();
  }

  @SuppressWarnings("WeakerAccess") @CheckResult @NonNull String getCurrentReentry() {
    return pinReentryText.getText().toString();
  }

  @SuppressWarnings("WeakerAccess") @CheckResult @NonNull String getCurrentHint() {
    return pinHintText.getText().toString();
  }

  @Override public void onStart() {
    super.onStart();
    presenter.bindView(this);
    presenter.loadApplicationIcon(packageName);
    presenter.hideUnimportantViews();
  }

  @Override public void onStop() {
    super.onStop();
    presenter.unbindView();
  }

  private void setupToolbar() {
    toolbar.setText("PIN");
  }

  @Override public void onDestroyView() {
    super.onDestroyView();

    Timber.d("Destroy AlertDialog");
    taskMap.clear();
    imm.toggleSoftInputFromWindow(getActivity().getWindow().getDecorView().getWindowToken(), 0, 0);
    unbinder.unbind();
  }

  @Override public void onDestroy() {
    super.onDestroy();
    if (!getActivity().isChangingConfigurations()) {
      PersistentCache.unload(loadedKey);
    }
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
}
