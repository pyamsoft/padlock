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

package com.pyamsoft.padlock.onboard;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.pyamsoft.padlock.databinding.OnboardEnableServiceBinding;
import com.pyamsoft.padlock.service.PadLockService;
import com.pyamsoft.pydroid.cache.PersistentCache;

public class OnboardEnableServiceFragment extends OnboardChildFragment
    implements OnboardEnableServicePresenter.View {

  @NonNull private static final String TAG = "OnboardingEnableServiceFragment";
  @NonNull private static final String KEY_PRESENTER = TAG + "key_presenter";
  OnboardEnableServicePresenter presenter;
  private OnboardEnableServiceBinding binding;

  @Nullable @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    binding = OnboardEnableServiceBinding.inflate(inflater, container, false);
    return binding.getRoot();
  }

  @Override public void onDestroyView() {
    super.onDestroyView();
    binding.unbind();
  }

  @Override public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    presenter = PersistentCache.load(getActivity(), KEY_PRESENTER,
        new OnboardEnableServicePresenterLoader());
  }

  @Override public void onStart() {
    super.onStart();
    presenter.bindView(this);
  }

  @Override public void onStop() {
    super.onStop();
    presenter.unbindView();
  }

  @Override public void onResume() {
    super.onResume();
    presenter.checkIfServiceIsRunning(PadLockService.isRunning());
  }

  @Override public void onServiceEnabled() {
    setNextButtonState(true);
  }

  @Override public void onServiceDisabled() {
    setNextButtonState(false);
  }

  private void setNextButtonState(boolean enabled) {
    final int buttonColor = enabled ? android.R.color.white : R.color.grey500;
    binding.onboardingNext.setTextColor(ContextCompat.getColor(getContext(), buttonColor));
    binding.onboardingNext.setOnClickListener(enabled ? v -> scrollToNextPage() : null);
  }
}
