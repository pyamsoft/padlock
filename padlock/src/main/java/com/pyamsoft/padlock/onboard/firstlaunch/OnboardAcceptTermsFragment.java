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
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.pyamsoft.padlock.Injector;
import com.pyamsoft.padlock.PadLock;
import com.pyamsoft.padlock.databinding.OnboardFirstlaunchAcceptTermsBinding;
import com.pyamsoft.padlock.onboard.OnboardChildFragment;
import javax.inject.Inject;
import timber.log.Timber;

public class OnboardAcceptTermsFragment extends OnboardChildFragment {

  @Inject OnboardAcceptTermsPresenter presenter;
  private OnboardFirstlaunchAcceptTermsBinding binding;

  @Override public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Injector.get().provideComponent().plusOnboardFirstLaunchComponent().inject(this);
  }

  @Nullable @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    binding = OnboardFirstlaunchAcceptTermsBinding.inflate(inflater, container, false);
    return binding.getRoot();
  }

  @Override public void onDestroyView() {
    super.onDestroyView();
    binding.unbind();
  }

  @Override public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    setupNextButton();
    setupCancelButton();
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

  private void setupCancelButton() {
    binding.onboardingCancel.setOnClickListener(v -> {
      Timber.w("Did not agree to terms");
      getActivity().finish();
    });
  }

  private void setupNextButton() {
    binding.onboardingAcceptTerms.setOnClickListener(v -> {
      Timber.i("Accepted Terms of use");
      presenter.acceptUsageTerms(this::completeOnboarding);
    });
  }
}
