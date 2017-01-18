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

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import com.pyamsoft.padlock.PadLock;
import com.pyamsoft.padlock.R;
import com.pyamsoft.padlock.databinding.OnboardListDialogBinding;

public class OnboardingDialog extends DialogFragment implements Onboard {

  @NonNull public static final String TAG = "OnboardingDialog";
  @NonNull private static final String KEY_LAST_POSITION = "key_onboardin_list_dialog_position";
  private static final int PAGER_PAGE_COUNT = 3;
  private OnboardListDialogBinding binding;
  private ViewPager.OnPageChangeListener pageChangeListener;

  @Override public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setCancelable(false);
  }

  @Override public void onDestroy() {
    super.onDestroy();
    PadLock.getRefWatcher(this).watch(this);
  }

  @NonNull @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
    final Dialog dialog = super.onCreateDialog(savedInstanceState);
    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
    return dialog;
  }

  @Nullable @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    binding = OnboardListDialogBinding.inflate(inflater, container, false);
    return binding.getRoot();
  }

  @Override public void onDestroyView() {
    super.onDestroyView();
    binding.onboardListPager.removeOnPageChangeListener(pageChangeListener);
    binding.unbind();
  }

  @Override public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    final int position;
    if (savedInstanceState == null) {
      position = 0;
    } else {
      position = savedInstanceState.getInt(KEY_LAST_POSITION, 0);
    }
    setupToolbar(position);
    setupViewPager(position);
  }

  @Override public void onSaveInstanceState(Bundle outState) {
    outState.putInt(KEY_LAST_POSITION, binding.onboardListPager.getCurrentItem());
    super.onSaveInstanceState(outState);
  }

  @Override public void onResume() {
    super.onResume();
    // The dialog is super small for some reason. We have to set the size manually, in onResume
    final Window window = getDialog().getWindow();
    if (window != null) {
      window.setLayout(WindowManager.LayoutParams.MATCH_PARENT,
          WindowManager.LayoutParams.WRAP_CONTENT);
    }
  }

  private void setupToolbar(int position) {
    ViewCompat.setElevation(binding.onboardListToolbar, 0);
    binding.onboardListToolbar.setTitle("");
    binding.onboardListToolbar.inflateMenu(R.menu.onboarding_next_page_menu);
    binding.onboardListToolbar.setNavigationOnClickListener(v -> scrollToPreviousPage());
    binding.onboardListToolbar.setOnMenuItemClickListener(item -> {
      if (item.getItemId() == R.id.onboarding_next_page) {
        scrollToNextPage();
        return true;
      }

      return false;
    });

    onPageChangeToolbarResponse(position);
  }

  private void setupViewPager(int position) {
    ViewCompat.setElevation(binding.onboardListPager, 0);
    binding.onboardListPager.setAdapter(new OnboardListPagerAdapter(getChildFragmentManager()));
    binding.onboardListPager.setCurrentItem(position);

    pageChangeListener = new ViewPager.SimpleOnPageChangeListener() {
      @Override public void onPageSelected(int position) {
        super.onPageSelected(position);
        onPageChangeToolbarResponse(position);
      }
    };
    binding.onboardListPager.addOnPageChangeListener(pageChangeListener);
  }

  void onPageChangeToolbarResponse(int position) {
    if (position == 0) {
      binding.onboardListToolbar.setNavigationIcon(null);
    } else {
      binding.onboardListToolbar.setNavigationIcon(R.drawable.ic_arrow_back_24dp);
    }

    binding.onboardListToolbar.getMenu()
        .findItem(R.id.onboarding_next_page)
        .setVisible(position + 1 < PAGER_PAGE_COUNT);
  }

  @Override public void onboardingComplete() {
    // TODO Save onboarding
    dismiss();
  }

  @Override public void scrollToPreviousPage() {
    binding.onboardListPager.arrowScroll(View.FOCUS_LEFT);
  }

  @Override public void scrollToNextPage() {
    binding.onboardListPager.arrowScroll(View.FOCUS_RIGHT);
  }

  static class OnboardListPagerAdapter extends FragmentStatePagerAdapter {

    OnboardListPagerAdapter(FragmentManager fm) {
      super(fm);
    }

    @Override public Fragment getItem(int position) {
      return new OnboardCreatePinFragment();
    }

    @Override public int getCount() {
      return PAGER_PAGE_COUNT;
    }
  }
}
