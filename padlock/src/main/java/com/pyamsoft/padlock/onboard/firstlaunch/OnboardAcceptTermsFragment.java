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

package com.pyamsoft.padlock.onboard.firstlaunch;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.pyamsoft.padlock.databinding.OnboardFirstlaunchAcceptTermsBinding;
import com.pyamsoft.padlock.main.MainActivity;
import com.pyamsoft.pydroid.cache.PersistentCache;
import timber.log.Timber;

public class OnboardAcceptTermsFragment extends OnboardChildFragment
    implements OnboardAcceptTermsPresenter.View {

  @NonNull private static final String TAG = "OnboardAcceptTermsFragment";
  @NonNull private static final String KEY_PRESENTER = TAG + "key_presenter";
  OnboardAcceptTermsPresenter presenter;
  private OnboardFirstlaunchAcceptTermsBinding binding;

  @Override public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    presenter =
        PersistentCache.load(getActivity(), KEY_PRESENTER, new OnboardAcceptTermsPresenterLoader());
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

  @Override public void onStart() {
    super.onStart();
    presenter.bindView(this);
  }

  @Override public void onStop() {
    super.onStop();
    presenter.unbindView();
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
      presenter.acceptUsageTerms();
    });
  }

  @Override public void onUsageTermsAccepted() {
    final Activity activity = getActivity();
    if (activity instanceof MainActivity) {
      ((MainActivity) activity).onOnboardingCompleted();
    } else {
      throw new IllegalStateException("Activity cannot handle onboarding result");
    }
  }
}
