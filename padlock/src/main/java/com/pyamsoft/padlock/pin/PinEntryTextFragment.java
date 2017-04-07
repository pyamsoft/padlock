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
import com.pyamsoft.padlock.Injector;
import com.pyamsoft.padlock.PadLock;
import com.pyamsoft.padlock.R;
import com.pyamsoft.padlock.databinding.FragmentPinEntryTextBinding;
import com.pyamsoft.pydroid.bus.EventBus;
import com.pyamsoft.pydroid.ui.loader.DrawableHelper;
import com.pyamsoft.pydroid.ui.loader.DrawableLoader;
import javax.inject.Inject;
import timber.log.Timber;

public class PinEntryTextFragment extends PinEntryBaseFragment {

  @NonNull static final String TAG = "PinEntryTextFragment";
  @NonNull private static final String CODE_DISPLAY = "CODE_DISPLAY";
  @NonNull private static final String CODE_REENTRY_DISPLAY = "CODE_REENTRY_DISPLAY";
  @NonNull private static final String HINT_DISPLAY = "HINT_DISPLAY";
  @SuppressWarnings("WeakerAccess") @Inject PinEntryPresenter presenter;
  EditText pinEntryText;
  EditText pinHintText;
  @SuppressWarnings("WeakerAccess") InputMethodManager imm;
  FragmentPinEntryTextBinding binding;
  private EditText pinReentryText;
  @NonNull final PinEntryPresenter.SubmitCallback submitCallback =
      new PinEntryPresenter.SubmitCallback() {

        @Override public void onSubmitSuccess(boolean creating) {
          clearDisplay();
          if (creating) {
            EventBus.get().publish(CreatePinEvent.create(true));
          } else {
            EventBus.get().publish(ClearPinEvent.create(true));
          }
          dismissParent();
        }

        @Override public void onSubmitFailure(boolean creating) {
          clearDisplay();
          if (creating) {
            EventBus.get().publish(CreatePinEvent.create(false));
          } else {
            EventBus.get().publish(ClearPinEvent.create(false));
          }
          dismissParent();
        }

        @Override public void onSubmitError() {
          clearDisplay();
          dismissParent();
        }
      };
  @NonNull private DrawableLoader.Loaded goTask = DrawableLoader.empty();

  @Override public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Injector.get().provideComponent().plusPinEntryComponent().inject(this);
  }

  @Nullable @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    binding = FragmentPinEntryTextBinding.inflate(inflater, container, false);
    return binding.getRoot();
  }

  @Override public void onDestroyView() {
    super.onDestroyView();
    goTask = DrawableHelper.unload(goTask);
    imm.toggleSoftInputFromWindow(getActivity().getWindow().getDecorView().getWindowToken(), 0, 0);
    binding.unbind();
  }

  @Override public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    // Resolve TextInputLayout edit texts
    pinEntryText = binding.pinEntryCode.getEditText();
    if (pinEntryText == null) {
      throw new NullPointerException("No item entry edit text");
    }

    pinReentryText = binding.pinReentryCode.getEditText();
    if (pinReentryText == null) {
      throw new NullPointerException("No item re-entry edit text");
    }

    pinHintText = binding.pinHint.getEditText();
    if (pinHintText == null) {
      throw new NullPointerException("No item hint edit text");
    }

    // Force the keyboard
    imm = (InputMethodManager) getContext().getApplicationContext()
        .getSystemService(Context.INPUT_METHOD_SERVICE);
    imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);

    clearDisplay();
    setupGoArrow(submitCallback);

    if (savedInstanceState != null) {
      onRestoreInstanceState(savedInstanceState);
    }
  }

  @Override public void onStart() {
    super.onStart();
    presenter.checkMasterPinPresent(new PinEntryPresenter.MasterPinStatusCallback() {

      private void setupSubmissionView(@NonNull EditText view,
          @NonNull PinEntryPresenter.SubmitCallback submitCallback) {
        view.setOnEditorActionListener((textView, actionId, keyEvent) -> {
          if (keyEvent == null) {
            Timber.e("KeyEvent was not caused by keypress");
            return false;
          }

          if (keyEvent.getAction() == KeyEvent.ACTION_DOWN && actionId == EditorInfo.IME_NULL) {
            Timber.d("KeyEvent is Enter pressed");
            presenter.submit(getCurrentAttempt(), getCurrentReentry(), getCurrentHint(),
                submitCallback);
            return true;
          }

          Timber.d("Do not handle key event");
          return false;
        });
      }

      @Override public void onMasterPinMissing() {
        Timber.d("No active master, show extra views");
        binding.pinReentryCode.setVisibility(View.VISIBLE);
        binding.pinHint.setVisibility(View.VISIBLE);
        setupSubmissionView(pinHintText, submitCallback);
      }

      @Override public void onMasterPinPresent() {
        Timber.d("Active master, hide extra views");
        binding.pinReentryCode.setVisibility(View.GONE);
        binding.pinHint.setVisibility(View.GONE);
        setupSubmissionView(pinEntryText, submitCallback);
      }
    });
  }

  @Override public void onStop() {
    super.onStop();
    presenter.stop();
  }

  @Override public void onDestroy() {
    super.onDestroy();
    presenter.destroy();
    PadLock.getRefWatcher(this).watch(this);
  }

  private void setupGoArrow(@NonNull PinEntryPresenter.SubmitCallback submitCallback) {
    binding.pinImageGo.setOnClickListener(view -> {
      presenter.submit(getCurrentAttempt(), getCurrentReentry(), getCurrentHint(), submitCallback);
      imm.toggleSoftInputFromWindow(getActivity().getWindow().getDecorView().getWindowToken(), 0,
          0);
    });

    // Force keyboard focus
    pinEntryText.requestFocus();

    goTask = DrawableHelper.unload(goTask);
    goTask = DrawableLoader.load(R.drawable.ic_arrow_forward_24dp)
        .tint(R.color.orangeA200)
        .into(binding.pinImageGo);
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
  void clearDisplay() {
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
}

