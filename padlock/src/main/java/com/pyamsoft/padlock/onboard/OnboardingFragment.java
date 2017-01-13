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
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.pyamsoft.padlock.databinding.FragmentOnboardingBinding;
import timber.log.Timber;

public class OnboardingFragment extends Fragment {

  @NonNull public static final String TAG = "OnboardingFragment";
  static final int MAX_USABLE_PAGE_COUNT = 2;
  @NonNull private static final String PAGER_SAVED_POSITION = "pager_saved_position";
  private FragmentOnboardingBinding binding;
  private ViewPager.OnPageChangeListener pageChangeListener;

  @Nullable @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    binding = FragmentOnboardingBinding.inflate(inflater, container, false);
    return binding.getRoot();
  }

  @Override public void onDestroyView() {
    super.onDestroyView();
    binding.onboardingPager.removeOnPageChangeListener(pageChangeListener);
    pageChangeListener = null;
    binding.unbind();
  }

  @Override public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    setupToolbar();

    final int startPosition;
    if (savedInstanceState != null) {
      startPosition = savedInstanceState.getInt(PAGER_SAVED_POSITION, 0);
    } else {
      startPosition = 0;
    }

    Timber.d("Current pager position is %d, max is %d", startPosition, MAX_USABLE_PAGE_COUNT);
    setupViewPager(startPosition);
    setupNextButton(startPosition);
    setupCancelButton(startPosition);
  }

  /**
   * The onboarding toolbar is not an action bar, just give it a title and menu, 0 elevation
   */
  private void setupToolbar() {
    ViewCompat.setElevation(binding.onboardingToolbar, 0);
    binding.onboardingToolbar.setTitle("Welcome to PadLock");
  }

  private void setupViewPager(final int startPosition) {
    ViewCompat.setElevation(binding.onboardingPager, 0);
    binding.onboardingPager.setAdapter(new OnboardingPagerAdapter(getChildFragmentManager()));
    binding.onboardingPager.setCurrentItem(startPosition);
    pageChangeListener = new ViewPager.SimpleOnPageChangeListener() {

      @Override public void onPageSelected(int position) {
        super.onPageSelected(position);
        if (position == MAX_USABLE_PAGE_COUNT) {
          prepareAcceptTermsPage();
        }
      }
    };

    binding.onboardingPager.addOnPageChangeListener(pageChangeListener);
  }

  private void setupNextButton(final int position) {
    if (position == MAX_USABLE_PAGE_COUNT) {
      prepareAcceptTermsPage();
    } else {
      binding.onboardingNext.setOnClickListener(v -> scrollToNextPage());
    }
  }

  void scrollToNextPage() {
    Timber.d("Scroll to next page");
    binding.onboardingPager.arrowScroll(View.FOCUS_RIGHT);
  }

  private void setupCancelButton(final int startPosition) {
    binding.onboardingCancel.setOnClickListener(v -> {
      Timber.w("Terms rejected, Close");
      getActivity().finish();
    });

    final int visible;
    if (startPosition == MAX_USABLE_PAGE_COUNT) {
      visible = View.VISIBLE;
    } else {
      visible = View.GONE;
    }

    binding.onboardingCancel.setVisibility(visible);
  }

  void prepareAcceptTermsPage() {
    Timber.d("End reached, this button serves as accept");
    binding.onboardingNext.setText("Accept Terms");
    binding.onboardingNext.setOnClickListener(v -> {
      Timber.i("Terms accepted, show normal");
      // TODO
    });

    binding.onboardingCancel.setVisibility(View.VISIBLE);
  }

  @Override public void onSaveInstanceState(Bundle outState) {
    final int position = binding.onboardingPager.getCurrentItem();
    Timber.d("Save current pager position: %d", position);
    outState.putInt(PAGER_SAVED_POSITION, position);
    super.onSaveInstanceState(outState);
  }

  static class OnboardingPagerAdapter extends FragmentStatePagerAdapter {

    OnboardingPagerAdapter(FragmentManager fm) {
      super(fm);
    }

    @Override public Fragment getItem(int position) {
      if (position >= getCount()) {
        throw new IndexOutOfBoundsException("OOB position: " + position);
      }

      final Fragment fragment;
      switch (position) {
        case 0:
          fragment = new OnboardingGetStartedFragment();
          break;
        case 1:
          fragment = new OnboardingEnableServiceFragment();
          break;
        case 2:
          fragment = new OnboardingAcceptTermsFragment();
          break;
        default:
          throw new IllegalArgumentException("Invalid position: " + position);
      }
      return fragment;
    }

    /**
     * Count is MAX_USABLE_PAGE_COUNT + 1 because arrays
     */
    @Override public int getCount() {
      return MAX_USABLE_PAGE_COUNT + 1;
    }
  }
}
