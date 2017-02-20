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

package com.pyamsoft.padlock.pin;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;
import com.pyamsoft.padlock.Injector;
import com.pyamsoft.padlock.PadLock;
import com.pyamsoft.padlock.R;
import com.pyamsoft.padlock.databinding.DialogPinEntryBinding;
import com.pyamsoft.padlock.iconloader.AppIconLoaderPresenter;
import com.pyamsoft.padlock.lock.common.LockTypePresenter;
import com.pyamsoft.pydroid.drawable.AsyncDrawable;
import com.pyamsoft.pydroid.drawable.AsyncMap;
import com.pyamsoft.pydroid.drawable.AsyncMapEntry;
import javax.inject.Inject;
import timber.log.Timber;

public class PinEntryDialog extends DialogFragment {

  @NonNull public static final String TAG = "PinEntryDialog";
  @NonNull static final String CHILD_TAG = "Child_PinEntry";
  @NonNull private static final String ENTRY_PACKAGE_NAME = "entry_packagename";
  @NonNull private static final String ENTRY_ACTIVITY_NAME = "entry_activityname";
  @NonNull private final AsyncMap taskMap = new AsyncMap();
  @SuppressWarnings("WeakerAccess") @Inject LockTypePresenter presenter;
  @SuppressWarnings("WeakerAccess") @Inject AppIconLoaderPresenter appIconLoaderPresenter;
  DialogPinEntryBinding binding;
  private String packageName;

  public static PinEntryDialog newInstance(final @NonNull String packageName,
      final @NonNull String activityName) {
    final PinEntryDialog fragment = new PinEntryDialog();
    final Bundle args = new Bundle();
    args.putString(ENTRY_PACKAGE_NAME, packageName);
    args.putString(ENTRY_ACTIVITY_NAME, activityName);
    fragment.setArguments(args);
    return fragment;
  }

  @NonNull @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
    final Dialog dialog = super.onCreateDialog(savedInstanceState);
    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
    return dialog;
  }

  @Override public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    packageName = getArguments().getString(ENTRY_PACKAGE_NAME);
    if (packageName == null) {
      throw new NullPointerException("Package name is NULL");
    }

    setCancelable(true);

    Injector.get().provideComponent().plusPinEntryComponent().inject(this);
  }

  @Override public void onResume() {
    super.onResume();
    // The dialog is super small for some reason. We have to set the size manually, in onResume
    final Window window = getDialog().getWindow();
    if (window != null) {
      window.setLayout(WindowManager.LayoutParams.MATCH_PARENT,
          WindowManager.LayoutParams.WRAP_CONTENT);
    }
  }

  @Nullable @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    binding = DialogPinEntryBinding.inflate(inflater, container, false);
    return binding.getRoot();
  }

  @Override public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    setupCloseButton();
    setupToolbar();

    presenter.initializeLockScreenType(new LockTypePresenter.LockScreenTypeCallback() {
      @Override public void onTypeText() {
        // Push text as child fragment
        getChildFragmentManager().beginTransaction()
            .replace(R.id.pin_entry_dialog_container, new PinEntryTextFragment(), CHILD_TAG)
            .commitNow();
      }

      @Override public void onTypePattern() {
        // TODO
      }
    });
  }

  private void setupCloseButton() {
    binding.pinEntryClose.setOnClickListener(view -> {
      Timber.d("onClick Arrow");
      dismiss();
    });

    final AsyncMapEntry task = AsyncDrawable.load(R.drawable.ic_close_24dp)
        .tint(android.R.color.black)
        .into(binding.pinEntryClose);
    taskMap.put("close", task);
  }

  @Override public void onStart() {
    super.onStart();
    appIconLoaderPresenter.bindView(null);
    presenter.bindView(null);
    appIconLoaderPresenter.loadApplicationIcon(packageName,
        new AppIconLoaderPresenter.LoadCallback() {
          @Override public void onApplicationIconLoadedSuccess(@NonNull Drawable icon) {
            binding.pinImage.setImageDrawable(icon);
          }

          @Override public void onApplicationIconLoadedError() {
            Toast.makeText(getActivity(), "Failed to load image", Toast.LENGTH_SHORT).show();
            dismiss();
          }
        });
  }

  @Override public void onStop() {
    super.onStop();
    presenter.unbindView();
    appIconLoaderPresenter.unbindView();
  }

  @SuppressLint("SetTextI18n") private void setupToolbar() {
    // Maybe something more descriptive
    binding.pinEntryToolbar.setText("PIN");
  }

  @Override public void onDestroyView() {
    super.onDestroyView();

    Timber.d("Destroy AlertDialog");
    taskMap.clear();
    binding.unbind();
  }

  @Override public void onDestroy() {
    super.onDestroy();
    PadLock.getRefWatcher(this).watch(this);
  }
}
