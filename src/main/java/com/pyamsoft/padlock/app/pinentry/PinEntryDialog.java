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

package com.pyamsoft.padlock.app.pinentry;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import com.pyamsoft.padlock.PadLock;
import com.pyamsoft.padlock.R;
import com.pyamsoft.padlock.app.lock.delegate.LockViewDelegate;
import com.pyamsoft.padlock.app.lock.delegate.LockViewDelegateImpl;
import com.pyamsoft.padlock.dagger.pinentry.DaggerPinEntryComponent;
import com.pyamsoft.padlock.model.event.PinEntryEvent;
import com.pyamsoft.padlock.model.event.RxBus;
import com.pyamsoft.pydroid.base.RetainedDialogFragmentBase;
import javax.inject.Inject;
import timber.log.Timber;

public class PinEntryDialog extends RetainedDialogFragmentBase implements PinScreen {

  private static final String ARG_PACKAGE = LockViewDelegate.ENTRY_PACKAGE_NAME;
  private static final String ARG_ACTIVITY = LockViewDelegate.ENTRY_ACTIVITY_NAME;

  @Inject PinEntryPresenter presenter;
  @BindView(R.id.lock_pin_entry_toolbar) Toolbar toolbar;
  @BindView(R.id.lock_pin_entry_close) ImageView close;
  private LockViewDelegate lockViewDelegate;
  private Unbinder unbinder;

  public PinEntryDialog() {
  }

  public static PinEntryDialog newInstance(final String packageName, final String activityName) {
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
    lockViewDelegate = new LockViewDelegateImpl<>(presenter, android.R.color.black,
        () -> presenter.attemptPinSubmission());
    presenter.create();

    setCancelable(true);
    setRetainInstance(true);
  }

  @NonNull @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
    Timber.d("Init dialog");
    final ContextWrapper themedContext =
        new ContextThemeWrapper(getContext(), R.style.Theme_PadLock_Light_PINEntry);
    @SuppressLint("InflateParams") final View rootView =
        LayoutInflater.from(themedContext).inflate(R.layout.layout_pin_entry, null, false);
    unbinder = ButterKnife.bind(this, rootView);
    presenter.bind(this);
    lockViewDelegate.onCreate(this, rootView);

    setupToolbar();
    return new AlertDialog.Builder(getActivity()).setView(rootView).create();
  }

  @Override public void onStart() {
    super.onStart();
    lockViewDelegate.onStart();
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
    lockViewDelegate.onDestroy();
    presenter.unbind();
    if (unbinder != null) {
      unbinder.unbind();
    }
  }

  @Override public void onDestroy() {
    super.onDestroy();
    presenter.destroy();
  }

  @NonNull @Override public String getCurrentAttempt() {
    return lockViewDelegate.getCurrentAttempt();
  }

  @Override @NonNull public String getPackageName() {
    return lockViewDelegate.getPackageName();
  }

  @Override @NonNull public String getActivityName() {
    return lockViewDelegate.getActivityName();
  }

  @Override public void setImageSuccess(@NonNull Drawable drawable) {
    lockViewDelegate.setImageSuccess(drawable);
  }

  @Override public void setImageError() {
    lockViewDelegate.setImageError();
  }

  @Override public void onSubmitSuccess() {
    dismiss();
  }

  @Override public void onSubmitFailure() {
    dismiss();
  }

  @Override public void onSubmitError() {
    dismiss();
  }

  @NonNull @Override public Context getContext() {
    return super.getContext().getApplicationContext();
  }

  public static final class PinEntryBus extends RxBus<PinEntryEvent> {

    private static final PinEntryBus instance = new PinEntryBus();

    public static PinEntryBus get() {
      return instance;
    }
  }
}
