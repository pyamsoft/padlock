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

import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;

abstract class OnboardChildFragment extends Fragment implements Onboard {

  @Override public void scrollToNextPage() {
    getOnboard().scrollToNextPage();
  }

  @Override public void scrollToPreviousPage() {
    getOnboard().scrollToPreviousPage();
  }

  @Override public void onboardingComplete() {
    getOnboard().onboardingComplete();
  }

  @CheckResult @NonNull private Onboard getOnboard() {
    final Fragment parent = getParentFragment();
    if (parent instanceof Onboard) {
      return (Onboard) parent;
    } else {
      throw new IllegalStateException("Parent Fragment is not instance of Onboard");
    }
  }
}
