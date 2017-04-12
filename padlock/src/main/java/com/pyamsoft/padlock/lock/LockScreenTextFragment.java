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
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import com.pyamsoft.padlock.R;
import com.pyamsoft.padlock.databinding.FragmentLockScreenTextBinding;
import com.pyamsoft.padlock.list.ErrorDialog;
import com.pyamsoft.padlock.service.PadLockService;
import com.pyamsoft.pydroid.ui.loader.DrawableHelper;
import com.pyamsoft.pydroid.ui.loader.DrawableLoader;
import com.pyamsoft.pydroid.util.DialogUtil;
import java.util.Locale;
import timber.log.Timber;

import static com.pyamsoft.padlock.lock.LockScreenActivity.ENTRY_LOCK_UNTIL_TIME;

public class LockScreenTextFragment extends LockScreenBaseFragment {

  @NonNull static final String TAG = "LockScreenTextFragment";
  @NonNull private static final String CODE_DISPLAY = "CODE_DISPLAY";
  @SuppressWarnings("WeakerAccess") InputMethodManager imm;
  FragmentLockScreenTextBinding binding;
  private EditText editText;
  @NonNull private DrawableLoader.Loaded arrowGoTask = DrawableLoader.empty();

  @CheckResult @NonNull
  public static LockScreenTextFragment newInstance(@NonNull String lockedPackageName,
      @NonNull String lockedActivityName, @Nullable String lockedCode,
      @NonNull String lockedRealName, boolean lockedSystem) {
    LockScreenTextFragment fragment = new LockScreenTextFragment();
    fragment.setArguments(
        buildBundle(lockedPackageName, lockedActivityName, lockedCode, lockedRealName,
            lockedSystem));
    return fragment;
  }

  @Nullable @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    binding = FragmentLockScreenTextBinding.inflate(inflater, container, false);
    return binding.getRoot();
  }

  @Override public void onDestroyView() {
    super.onDestroyView();
    arrowGoTask = DrawableHelper.unload(arrowGoTask);
    imm.toggleSoftInputFromWindow(getActivity().getWindow().getDecorView().getWindowToken(), 0, 0);
    binding.unbind();
  }

  @Override public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    LockSubmitCallback submitCallback = new LockSubmitCallback() {

      @NonNull final LockScreenPresenter.LockErrorCallback lockErrorCallback = () -> {
        Timber.e("LOCK ERROR");
        DialogUtil.guaranteeSingleDialogFragment(getActivity(), new ErrorDialog(), "lock_error");
      };

      @Override public void onSubmitSuccess() {
        Timber.d("Unlocked!");
        clearDisplay();

        presenter.postUnlock(getLockedPackageName(), getLockedActivityName(), getLockedRealName(),
            getLockedCode(), isLockedSystem(), isExcluded(), getSelectedIgnoreTime(),
            new LockScreenEntryPresenter.PostUnlockCallback() {
              @Override public void onPostUnlock() {
                Timber.d("POST Unlock Finished! 1");
                PadLockService.passLockScreen(getLockedPackageName(), getLockedActivityName());
                getActivity().finish();
              }

              @Override public void onLockedError() {
                lockErrorCallback.onLockedError();
              }
            });
      }

      @Override public void onSubmitFailure() {
        Timber.e("Failed to unlock");
        clearDisplay();
        showSnackbarWithText("Error: Invalid PIN");
        binding.lockDisplayHint.setVisibility(View.VISIBLE);

        // Once fail count is tripped once, continue to update it every time following until time elapses
        presenter.lockEntry(getLockedPackageName(), getLockedActivityName(),
            new LockScreenEntryPresenter.LockCallback() {
              @Override public void onLocked(long lockUntilTime) {
                setLockedUntilTime(lockUntilTime);
                getActivity().getIntent().removeExtra(ENTRY_LOCK_UNTIL_TIME);
                getActivity().getIntent().putExtra(ENTRY_LOCK_UNTIL_TIME, lockUntilTime);
                showSnackbarWithText("This entry is temporarily locked");
              }

              @Override public void onLockedError() {
                lockErrorCallback.onLockedError();
              }
            });
      }

      @Override public void onSubmitError() {
        clearDisplay();
        DialogUtil.guaranteeSingleDialogFragment(getActivity(), new ErrorDialog(), "unlock_error");
      }
    };

    setupTextInput(submitCallback);
    setupGoArrow(submitCallback);
    setupInputManager();
    clearDisplay();

    // Hide hint to begin with
    binding.lockDisplayHint.setVisibility(View.GONE);
  }

  @Override public void onStart() {
    super.onStart();
    presenter.displayLockedHint(hint -> {
      Timber.d("Settings hint");
      binding.lockDisplayHint.setText(
          String.format(Locale.getDefault(), "Hint: %s", hint.isEmpty() ? "NO HINT" : hint));
    });
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
      presenter.submit(getLockedPackageName(), getLockedActivityName(), getLockedCode(),
          getLockedUntilTime(), getCurrentAttempt(), submitCallback);
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

    arrowGoTask = DrawableHelper.unload(arrowGoTask);
    arrowGoTask = DrawableLoader.load(R.drawable.ic_arrow_forward_24dp)
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
        presenter.submit(getLockedPackageName(), getLockedActivityName(), getLockedCode(),
            getLockedUntilTime(), getCurrentAttempt(), submitCallback);
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
