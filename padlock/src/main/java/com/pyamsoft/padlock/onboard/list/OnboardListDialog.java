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
import com.pyamsoft.padlock.Injector;
import com.pyamsoft.padlock.PadLock;
import com.pyamsoft.padlock.R;
import com.pyamsoft.padlock.databinding.OnboardListDialogBinding;
import com.pyamsoft.padlock.list.LockListFragment;
import com.pyamsoft.padlock.main.MainFragment;
import com.pyamsoft.padlock.onboard.Onboard;
import com.pyamsoft.pydroid.ui.loader.ImageLoader;
import com.pyamsoft.pydroid.ui.loader.LoaderMap;
import com.pyamsoft.pydroid.ui.loader.loaded.Loaded;
import javax.inject.Inject;

public class OnboardListDialog extends DialogFragment implements Onboard {

  @NonNull public static final String TAG = "OnboardListDialog";
  @NonNull private static final String KEY_LAST_POSITION = "key_onboard_list_dialog_position";
  private static final int PAGER_PAGE_COUNT = 3;
  @NonNull private final LoaderMap mapper = new LoaderMap();
  @Inject OnboardListPresenter presenter;
  private OnboardListDialogBinding binding;
  private ViewPager.OnPageChangeListener pageChangeListener;

  @Override public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setCancelable(false);
    Injector.get().provideComponent().plusOnboardListComponent().inject(this);
  }

  @Override public void onDestroy() {
    super.onDestroy();
    presenter.destroy();
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
    mapper.clear();
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
    setupButtons(position);
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

  @Override public void onStop() {
    super.onStop();
    presenter.stop();
  }

  private void setupButtons(int position) {
    ViewCompat.setElevation(binding.onboardListButtonbar, 0);
    binding.onboardListBack.setOnClickListener(v -> scrollToPreviousPage());
    binding.onboardListNext.setOnClickListener(v -> scrollToNextPage());
    binding.onboardListConfirm.setOnClickListener(v -> completeOnboarding());

    final Loaded backTask =
        ImageLoader.fromResource(R.drawable.ic_arrow_back_24dp).into(binding.onboardListBack);
    mapper.put("back", backTask);

    final Loaded nextTask =
        ImageLoader.fromResource(R.drawable.ic_arrow_forward_24dp).into(binding.onboardListNext);
    mapper.put("next", nextTask);

    final Loaded confirmTask =
        ImageLoader.fromResource(R.drawable.ic_check_24dp).into(binding.onboardListConfirm);
    mapper.put("confirm", confirmTask);

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
      binding.onboardListBack.setVisibility(View.GONE);
      binding.onboardListConfirm.setVisibility(View.GONE);
    } else {
      binding.onboardListBack.setVisibility(View.VISIBLE);
      if (position + 1 < PAGER_PAGE_COUNT) {
        binding.onboardListConfirm.setVisibility(View.GONE);
        binding.onboardListConfirm.hide();
        binding.onboardListNext.setVisibility(View.VISIBLE);
      } else {
        binding.onboardListNext.setVisibility(View.GONE);
        binding.onboardListConfirm.setVisibility(View.VISIBLE);
        binding.onboardListConfirm.show();
      }
    }
  }

  @Override public void completeOnboarding() {
    presenter.finishOnboarding();
    dismiss();

    // Talk to list fragment
    final FragmentManager fragmentManager = getFragmentManager();
    final Fragment fragment = fragmentManager.findFragmentByTag(MainFragment.TAG);
    if (fragment instanceof MainFragment) {
      Fragment lockList =
          fragment.getChildFragmentManager().findFragmentByTag(LockListFragment.TAG);
      if (lockList instanceof LockListFragment) {
        ((LockListFragment) lockList).onCompletedOnboarding();
      }
    }
  }

  @Override public void scrollToPreviousPage() {
    binding.onboardListPager.arrowScroll(View.FOCUS_LEFT);
  }

  @Override public void scrollToNextPage() {
    binding.onboardListPager.arrowScroll(View.FOCUS_RIGHT);
  }

  private static class OnboardListPagerAdapter extends FragmentStatePagerAdapter {

    OnboardListPagerAdapter(FragmentManager fm) {
      super(fm);
    }

    @Override public Fragment getItem(int position) {
      final Fragment fragment;
      switch (position) {
        case 0:
          fragment = new OnboardCreatePinFragment();
          break;
        case 1:
          fragment = new OnboardLockPackageFragment();
          break;
        case 2:
          fragment = new OnboardShowInfoFragment();
          break;
        default:
          throw new IllegalArgumentException("Invalid position: " + position);
      }
      return fragment;
    }

    @Override public int getCount() {
      return PAGER_PAGE_COUNT;
    }
  }
}
