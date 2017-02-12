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

package com.pyamsoft.padlock.onboard.list;

import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.support.annotation.CheckResult;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.pyamsoft.padlock.PadLock;
import com.pyamsoft.padlock.databinding.OnboardListPageContentBinding;
import com.pyamsoft.padlock.onboard.OnboardChildFragment;
import com.pyamsoft.pydroid.helper.AsyncMapHelper;
import com.pyamsoft.pydroid.tool.AsyncDrawable;
import com.pyamsoft.pydroid.tool.AsyncMap;

abstract class OnboardContentFragment extends OnboardChildFragment {

  private OnboardListPageContentBinding binding;
  private AsyncMap.Entry imageTask;

  @CallSuper @Override public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
  }

  @CallSuper @Override public void onDestroy() {
    super.onDestroy();
    PadLock.getRefWatcher(this).watch(this);
  }

  @CallSuper @Nullable @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    binding = OnboardListPageContentBinding.inflate(inflater, container, false);
    return binding.getRoot();
  }

  @CallSuper @Override public void onDestroyView() {
    super.onDestroyView();
    AsyncMapHelper.unsubscribe(imageTask);
    binding.unbind();
  }

  @CallSuper @Override public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    AsyncMapHelper.unsubscribe(imageTask);
    imageTask = AsyncDrawable.load(getOnboardImage()).into(binding.onboardListContentImage);

    binding.onboardListContentText.setText(getOnboardText());
  }

  @CheckResult @StringRes protected abstract int getOnboardText();

  @CheckResult @DrawableRes protected abstract int getOnboardImage();
}
