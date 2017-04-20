/*
 * Copyright 2017 Peter Kenji Yamanaka
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

package com.pyamsoft.padlock.onboard.firstlaunch;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.pyamsoft.padlock.Injector;
import com.pyamsoft.padlock.R;
import com.pyamsoft.padlock.databinding.OnboardFirstlaunchEnableServiceBinding;
import com.pyamsoft.padlock.onboard.OnboardChildFragment;
import com.pyamsoft.padlock.service.PadLockService;
import com.pyamsoft.padlock.uicommon.AccessibilityRequestDelegate;

public class OnboardEnableServiceFragment extends OnboardChildFragment {

  @NonNull final AccessibilityRequestDelegate delegate = new AccessibilityRequestDelegate();
  private OnboardFirstlaunchEnableServiceBinding binding;

  @Nullable @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    binding = OnboardFirstlaunchEnableServiceBinding.inflate(inflater, container, false);
    return binding.getRoot();
  }

  @Override public void onDestroyView() {
    super.onDestroyView();
    binding.unbind();
  }

  @Override public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Injector.get().provideComponent().plusOnboardFirstLaunchComponent().inject(this);
  }

  @Override public void onResume() {
    super.onResume();
    if (PadLockService.isRunning()) {
      onServiceEnabled();
    } else {
      onServiceDisabled();
    }
  }

  private void onServiceEnabled() {
    binding.onboardingNext.setText(R.string.continue_onboard);
    binding.onboardingNext.setOnClickListener(v -> scrollToNextPage());
    binding.serviceEnabledText.setText(R.string.service_enabled);
  }

  private void onServiceDisabled() {
    binding.onboardingNext.setText(R.string.enable_service);
    binding.onboardingNext.setOnClickListener(
        v -> delegate.launchAccessibilityIntent(getActivity()));
    binding.serviceEnabledText.setText(R.string.please_enable_service);
  }
}
