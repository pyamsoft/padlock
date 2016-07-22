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
import android.support.v4.app.DialogFragment;
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
import com.pyamsoft.padlock.app.base.AppIconLoaderPresenter;
import com.pyamsoft.padlock.model.RxBus;
import com.pyamsoft.padlock.model.event.PinEntryEvent;
import javax.inject.Inject;
import timber.log.Timber;

public class PinEntryDialog extends DialogFragment implements PinScreen, LockViewDelegate.Callback {

  @NonNull private static final String ARG_PACKAGE = LockViewDelegate.ENTRY_PACKAGE_NAME;
  @NonNull private static final String ARG_ACTIVITY = LockViewDelegate.ENTRY_ACTIVITY_NAME;

  @Inject PinEntryPresenter presenter;
  @Inject AppIconLoaderPresenter<PinScreen> appIconLoaderPresenter;
  @BindView(R.id.lock_pin_entry_toolbar) Toolbar toolbar;
  @BindView(R.id.lock_pin_entry_close) ImageView close;
  @Inject LockViewDelegate lockViewDelegate;
  private Unbinder unbinder;

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
    PadLock.getInstance().getPadLockComponent().plusPinEntry().inject(this);

    lockViewDelegate.setTextColor(android.R.color.black);
    setCancelable(true);
  }

  @NonNull @Override public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
    Timber.d("Init dialog");
    final ContextWrapper themedContext =
        new ContextThemeWrapper(getContext(), R.style.Theme_PadLock_Light_PINEntry);
    @SuppressLint("InflateParams") final View rootView =
        LayoutInflater.from(themedContext).inflate(R.layout.layout_pin_entry, null, false);
    unbinder = ButterKnife.bind(this, rootView);

    presenter.bindView(this);

    appIconLoaderPresenter.bindView(this);

    lockViewDelegate.onCreateView(this, this, rootView);

    if (savedInstanceState != null) {
      lockViewDelegate.onRestoreInstanceState(savedInstanceState);
    }

    setupToolbar();
    return new AlertDialog.Builder(getActivity()).setView(rootView).create();
  }

  @Override public void onSaveInstanceState(@NonNull Bundle outState) {
    Timber.d("onSaveInstanceState");
    lockViewDelegate.onSaveInstanceState(outState);
    super.onSaveInstanceState(outState);
  }

  @Override public void onStart() {
    super.onStart();
    lockViewDelegate.onStart(appIconLoaderPresenter);
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
    presenter.unbindView();

    appIconLoaderPresenter.unbindView();

    lockViewDelegate.onDestroyView();

    unbinder.unbind();
  }

  @Override public void onApplicationIconLoadedError() {
    Toast.makeText(getActivity(), "Failed to load image", Toast.LENGTH_SHORT).show();
    dismiss();
  }

  @Override public void onApplicationIconLoadedSuccess(@NonNull Drawable icon) {
    lockViewDelegate.onApplicationIconLoadedSuccess(icon);
  }

  @Override public void onSubmitSuccess() {
    lockViewDelegate.clearDisplay();
    dismiss();
  }

  @Override public void onSubmitFailure() {
    lockViewDelegate.clearDisplay();
    dismiss();
  }

  @Override public void onSubmitError() {
    lockViewDelegate.clearDisplay();
    dismiss();
  }

  @Override public void onSubmitPressed() {
    presenter.submit(lockViewDelegate.getCurrentAttempt());
  }

  public static final class PinEntryBus extends RxBus<PinEntryEvent> {

    @NonNull private static final PinEntryBus instance = new PinEntryBus();

    @CheckResult @NonNull public static PinEntryBus get() {
      return instance;
    }
  }
}
