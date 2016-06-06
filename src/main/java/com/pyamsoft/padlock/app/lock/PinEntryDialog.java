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
import android.content.ContextWrapper;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import com.pyamsoft.padlock.PadLock;
import com.pyamsoft.padlock.R;
import com.pyamsoft.padlock.dagger.lock.DaggerPinEntryComponent;
import com.pyamsoft.padlock.model.event.PinEntryEvent;
import com.pyamsoft.padlock.model.event.RxBus;
import com.pyamsoft.pydroid.base.RetainedDialogFragment;
import javax.inject.Inject;
import timber.log.Timber;

public class PinEntryDialog extends RetainedDialogFragment implements PinScreen {

  @NonNull private static final String ARG_PACKAGE = LockViewDelegate.ENTRY_PACKAGE_NAME;
  @NonNull private static final String ARG_ACTIVITY = LockViewDelegate.ENTRY_ACTIVITY_NAME;

  @Nullable @Inject PinEntryPresenter presenter;
  @Nullable @BindView(R.id.lock_pin_entry_toolbar) Toolbar toolbar;
  @Nullable @BindView(R.id.lock_pin_entry_close) ImageView close;
  @Nullable @Inject LockViewDelegate lockViewDelegate;
  @Nullable private Unbinder unbinder;

  public static PinEntryDialog newInstance(final @NonNull String packageName,
      final @NonNull String activityName) {
    final PinEntryDialog fragment = new PinEntryDialog();
    final Bundle args = new Bundle();
    args.putString(ARG_PACKAGE, packageName);
    args.putString(ARG_ACTIVITY, activityName);
    fragment.setArguments(args);
    return fragment;
  }

  @Override public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    DaggerPinEntryComponent.builder()
        .padLockComponent(PadLock.padLockComponent(this))
        .build()
        .inject(this);

    assert lockViewDelegate != null;
    lockViewDelegate.setTextColor(android.R.color.black);

    setCancelable(true);
    setRetainInstance(true);
  }

  @NonNull @Override public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
    Timber.d("Init dialog");
    final ContextWrapper themedContext =
        new ContextThemeWrapper(getContext(), R.style.Theme_PadLock_Light_PINEntry);
    @SuppressLint("InflateParams") final View rootView =
        LayoutInflater.from(themedContext).inflate(R.layout.layout_pin_entry, null, false);
    unbinder = ButterKnife.bind(this, rootView);

    assert presenter != null;
    presenter.onCreateView(this);

    assert lockViewDelegate != null;
    lockViewDelegate.onCreateView(presenter, this, rootView);

    if (savedInstanceState != null) {
      lockViewDelegate.onRestoreInstanceState(savedInstanceState);
    }

    setupToolbar();
    return new AlertDialog.Builder(getActivity()).setView(rootView).create();
  }

  @Override public void onSaveInstanceState(@NonNull Bundle outState) {
    Timber.d("onSaveInstanceState");
    assert lockViewDelegate != null;
    lockViewDelegate.onSaveInstanceState(outState);
    super.onSaveInstanceState(outState);
  }

  @Override public void onStart() {
    super.onStart();
    assert lockViewDelegate != null;
    assert presenter != null;
    lockViewDelegate.onStart(presenter);
  }

  private void setupToolbar() {
    assert toolbar != null;
    toolbar.setTitle("PIN");

    assert close != null;
    close.setOnClickListener(view -> {
      Timber.d("onClick Arrow");
      dismiss();
    });
  }

  @Override public void onDestroyView() {
    super.onDestroyView();
    Timber.d("Destroy AlertDialog");
    if (lockViewDelegate != null) {
      lockViewDelegate.onDestroyView();
    }
    if (presenter != null) {
      presenter.onDestroyView();
    }
    if (unbinder != null) {
      unbinder.unbind();
    }
  }

  @NonNull @Override public String getCurrentAttempt() {
    assert lockViewDelegate != null;
    return lockViewDelegate.getCurrentAttempt();
  }

  @Override @NonNull public String getPackageName() {
    assert lockViewDelegate != null;
    return lockViewDelegate.getPackageName();
  }

  @Override @NonNull public String getActivityName() {
    assert lockViewDelegate != null;
    return lockViewDelegate.getActivityName();
  }

  @Override public void onApplicationIconLoadedError() {
    Toast.makeText(getActivity(), "Failed to load image", Toast.LENGTH_SHORT).show();
    dismiss();
  }

  @Override public void onApplicationIconLoadedSuccess(@NonNull Drawable icon) {
    assert lockViewDelegate != null;
    lockViewDelegate.onApplicationIconLoadedSuccess(icon);
  }

  @Override public void onSubmitSuccess() {
    assert lockViewDelegate != null;
    lockViewDelegate.clearDisplay();
    dismiss();
  }

  @Override public void onSubmitFailure() {
    assert lockViewDelegate != null;
    lockViewDelegate.clearDisplay();
    dismiss();
  }

  @Override public void onSubmitError() {
    assert lockViewDelegate != null;
    lockViewDelegate.clearDisplay();
    dismiss();
  }

  public static final class PinEntryBus extends RxBus<PinEntryEvent> {

    @NonNull private static final PinEntryBus instance = new PinEntryBus();

    @CheckResult @NonNull public static PinEntryBus get() {
      return instance;
    }
  }
}
