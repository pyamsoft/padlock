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

package com.pyamsoft.padlock.app.main;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import com.pyamsoft.padlock.app.list.LockListFragment;
import com.pyamsoft.padlock.app.settings.SettingsScreen;
import com.pyamsoft.pydroid.tool.PageAwareAdapter;
import com.pyamsoft.pydroid.tool.PageAwareView;

final class MainPagerAdapter extends FragmentStatePagerAdapter implements PageAwareAdapter {

  public static final int LIST = 0;
  public static final int SETTINGS = 1;
  private static final int NUMBER_ITEMS = 2;
  private LockListFragment lockListFragment;
  private SettingsScreen settingsFragment;

  public MainPagerAdapter(FragmentManager fm) {
    super(fm);
  }

  public void setLockListFragment(final LockListFragment fragment) {
    this.lockListFragment = fragment;
  }

  public void setSettingsFragment(final SettingsScreen fragment) {
    this.settingsFragment = fragment;
  }

  @Override public Fragment getItem(int position) {
    Fragment fragment;
    switch (position) {
      case LIST:
        fragment = lockListFragment;
        break;
      case SETTINGS:
        fragment = settingsFragment;
        break;
      default:
        throw new IllegalStateException("Invalid position for Adapter: " + position);
    }

    if (fragment == null) {
      throw new IllegalStateException("Fragment must be set before Adapter is used");
    }

    return fragment;
  }

  @Override public int getCount() {
    return NUMBER_ITEMS;
  }

  @Override public void onPageSelected(ViewPager viewPager, int position) {
    final Object item = instantiateItem(viewPager, position);
    if (item != null && item instanceof PageAwareView) {
      final PageAwareView pageAware = (PageAwareView) item;
      pageAware.onPageSelected();
    }
  }

  @Override public void onPageUnselected(ViewPager viewPager, int position) {
    final Object item = instantiateItem(viewPager, position);
    if (item != null && item instanceof PageAwareView) {
      final PageAwareView pageAware = (PageAwareView) item;
      pageAware.onPageUnselected();
    }
  }
}
