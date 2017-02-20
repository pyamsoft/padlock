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

package com.pyamsoft.padlock.lock;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import com.pyamsoft.padlock.Injector;
import com.pyamsoft.padlock.R;
import com.pyamsoft.padlock.databinding.FragmentLockScreenTextBinding;
import com.pyamsoft.padlock.list.ErrorDialog;
import com.pyamsoft.padlock.service.PadLockService;
import com.pyamsoft.pydroid.drawable.AsyncDrawable;
import com.pyamsoft.pydroid.drawable.AsyncMap;
import com.pyamsoft.pydroid.drawable.AsyncMapEntry;
import com.pyamsoft.pydroid.helper.AsyncMapHelper;
import com.pyamsoft.pydroid.util.AppUtil;
import javax.inject.Inject;
import timber.log.Timber;

import static com.pyamsoft.padlock.lock.LockScreenActivity.ENTRY_ACTIVITY_NAME;
import static com.pyamsoft.padlock.lock.LockScreenActivity.ENTRY_IS_SYSTEM;
import static com.pyamsoft.padlock.lock.LockScreenActivity.ENTRY_LOCK_CODE;
import static com.pyamsoft.padlock.lock.LockScreenActivity.ENTRY_LOCK_UNTIL_TIME;
import static com.pyamsoft.padlock.lock.LockScreenActivity.ENTRY_PACKAGE_NAME;
import static com.pyamsoft.padlock.lock.LockScreenActivity.ENTRY_REAL_NAME;
import static com.pyamsoft.padlock.service.PadLockService.finish;

public class LockScreenTextFragment extends Fragment {

  @NonNull static final String TAG = "LockScreenTextFragment";
  @NonNull private static final String CODE_DISPLAY = "CODE_DISPLAY";
  @NonNull final LockScreenPresenter.LockErrorCallback lockErrorCallback = () -> {
    Timber.e("LOCK ERROR");
    AppUtil.guaranteeSingleDialogFragment(getActivity(), new ErrorDialog(), "lock_error");
  };
  @SuppressWarnings("WeakerAccess") String lockedActivityName;
  @SuppressWarnings("WeakerAccess") String lockedPackageName;
  @SuppressWarnings("WeakerAccess") String lockedCode;
  String lockedRealName;
  boolean lockedSystem;
  @SuppressWarnings("WeakerAccess") InputMethodManager imm;
  @SuppressWarnings("WeakerAccess") @Inject LockScreenPresenter presenter;
  private FragmentLockScreenTextBinding binding;
  private EditText editText;
  @NonNull private AsyncMapEntry arrowGoTask = AsyncMap.emptyEntry();

  @CheckResult @NonNull
  public static LockScreenTextFragment newInstance(@NonNull String lockedPackageName,
      @NonNull String lockedActivityName, @Nullable String lockedCode,
      @NonNull String lockedRealName, boolean lockedSystem) {
    Bundle args = new Bundle();
    LockScreenTextFragment fragment = new LockScreenTextFragment();
    args.putString(ENTRY_PACKAGE_NAME, lockedPackageName);
    args.putString(ENTRY_ACTIVITY_NAME, lockedActivityName);
    args.putString(ENTRY_LOCK_CODE, lockedCode);
    args.putString(ENTRY_REAL_NAME, lockedRealName);
    args.putBoolean(ENTRY_IS_SYSTEM, lockedSystem);
    fragment.setArguments(args);
    return fragment;
  }

  @Override public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Injector.get().provideComponent().plusLockScreenComponent().inject(this);

    final Bundle bundle = getArguments();
    lockedPackageName = bundle.getString(ENTRY_PACKAGE_NAME);
    lockedActivityName = bundle.getString(ENTRY_ACTIVITY_NAME);
    lockedRealName = bundle.getString(ENTRY_REAL_NAME);
    lockedCode = bundle.getString(ENTRY_LOCK_CODE);
    lockedSystem = bundle.getBoolean(ENTRY_IS_SYSTEM, false);

    if (lockedPackageName == null || lockedActivityName == null || lockedRealName == null) {
      throw new NullPointerException("Missing required lock attributes");
    }
  }

  @Nullable @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    binding = FragmentLockScreenTextBinding.inflate(inflater, container, false);
    return binding.getRoot();
  }

  @Override public void onDestroyView() {
    super.onDestroyView();
    arrowGoTask = AsyncMapHelper.unsubscribe(arrowGoTask);
    imm.toggleSoftInputFromWindow(getActivity().getWindow().getDecorView().getWindowToken(), 0, 0);
    binding.unbind();
  }

  @Override public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    LockSubmitCallback submitCallback = new LockSubmitCallback() {

      @Override public void onSubmitSuccess() {
        Timber.d("Unlocked!");
        clearDisplay();

        presenter.postUnlock(lockedPackageName, lockedActivityName, lockedRealName, lockedCode,
            lockedSystem, LockScreenActivity.isExcluded(getActivity()),
            LockScreenActivity.getSelectedIgnoreTime(getActivity()),
            new LockScreenPresenter.PostUnlockCallback() {
              @Override public void onPostUnlock() {
                Timber.d("POST Unlock Finished! 1");
                PadLockService.passLockScreen();
                finish();
              }

              @Override public void onLockedError() {
                lockErrorCallback.onLockedError();
              }
            });
      }

      @Override public void onSubmitFailure() {
        Timber.e("Failed to unlock");
        clearDisplay();
        LockScreenActivity.showSnackbarWithText(getActivity(), "Error: Invalid PIN");
        LockScreenActivity.showHint(getActivity());

        // Once fail count is tripped once, continue to update it every time following until time elapses
        presenter.lockEntry(lockedPackageName, lockedActivityName,
            new LockScreenPresenter.LockCallback() {
              @Override public void onLocked(long lockUntilTime) {
                LockScreenActivity.setLockedUntilTime(getActivity(), lockUntilTime);
                getActivity().getIntent().removeExtra(ENTRY_LOCK_UNTIL_TIME);
                getActivity().getIntent().putExtra(ENTRY_LOCK_UNTIL_TIME, lockUntilTime);
                LockScreenActivity.showSnackbarWithText(getActivity(),
                    "This entry is temporarily locked");
              }

              @Override public void onLockedError() {
                lockErrorCallback.onLockedError();
              }
            });
      }

      @Override public void onSubmitError() {
        clearDisplay();
        AppUtil.guaranteeSingleDialogFragment(getActivity(), new ErrorDialog(), "unlock_error");
      }
    };

    setupTextInput(submitCallback);
    setupGoArrow(submitCallback);
    setupInputManager();
    clearDisplay();
  }

  public void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
    final String attempt = savedInstanceState.getString(CODE_DISPLAY, null);
    if (attempt == null) {
      Timber.d("Empty attempt");
      clearDisplay();
    } else {
      Timber.d("Set attempt %s", attempt);
      editText.setText(attempt);
    }
  }

  @Override public void onSaveInstanceState(Bundle outState) {
    final String attempt = getCurrentAttempt();
    outState.putString(CODE_DISPLAY, attempt);
    super.onSaveInstanceState(outState);
  }

  private void setupInputManager() {
    // Force the keyboard
    imm = (InputMethodManager) getActivity().getApplicationContext()
        .getSystemService(Context.INPUT_METHOD_SERVICE);
    imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
  }

  private void setupGoArrow(@NonNull LockSubmitCallback submitCallback) {
    binding.lockImageGo.setOnClickListener(view -> {
      presenter.submit(lockedPackageName, lockedActivityName, lockedCode,
          LockScreenActivity.getLockedUntilTime(getActivity()), getCurrentAttempt(),
          submitCallback);
      imm.toggleSoftInputFromWindow(getActivity().getWindow().getDecorView().getWindowToken(), 0,
          0);
    });

    // Force keyboard focus
    editText.requestFocus();

    editText.setOnFocusChangeListener((view, hasFocus) -> {
      if (hasFocus) {
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
      }
    });

    arrowGoTask = AsyncMapHelper.unsubscribe(arrowGoTask);
    arrowGoTask = AsyncDrawable.load(R.drawable.ic_arrow_forward_24dp)
        .tint(R.color.orangeA200)
        .into(binding.lockImageGo);
  }

  @SuppressWarnings("WeakerAccess") @CheckResult @NonNull String getCurrentAttempt() {
    return editText.getText().toString();
  }

  private void setupTextInput(@NonNull LockSubmitCallback submitCallback) {
    editText = binding.lockText.getEditText();
    if (editText == null) {
      throw new NullPointerException("Edit text is NULL");
    }

    editText.setOnEditorActionListener((textView, actionId, keyEvent) -> {
      if (keyEvent == null) {
        Timber.e("KeyEvent was not caused by keypress");
        return false;
      }

      if (keyEvent.getAction() == KeyEvent.ACTION_DOWN && actionId == EditorInfo.IME_NULL) {
        Timber.d("KeyEvent is Enter pressed");
        presenter.submit(lockedPackageName, lockedActivityName, lockedCode,
            LockScreenActivity.getLockedUntilTime(getActivity()), getCurrentAttempt(),
            submitCallback);
        return true;
      }

      Timber.d("Do not handle key event");
      return false;
    });
  }

  void clearDisplay() {
    if (editText != null) {
      editText.setText("");
    }
  }
}
