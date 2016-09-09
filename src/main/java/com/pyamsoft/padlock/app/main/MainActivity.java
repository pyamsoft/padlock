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

import android.os.Bundle;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.ActionMenuView;
import android.support.v7.widget.Toolbar;
import android.text.Spannable;
import android.view.MenuItem;
import android.view.View;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import com.pyamsoft.padlock.BuildConfig;
import com.pyamsoft.padlock.R;
import com.pyamsoft.padlock.app.list.LockListFragment;
import com.pyamsoft.padlock.app.settings.SettingsFragment;
import com.pyamsoft.pydroid.app.activity.DonationActivity;
import com.pyamsoft.pydroid.base.PersistLoader;
import com.pyamsoft.pydroid.lib.AboutLibrariesFragment;
import com.pyamsoft.pydroid.support.RatingDialog;
import com.pyamsoft.pydroid.util.AnimUtil;
import com.pyamsoft.pydroid.util.AppUtil;
import com.pyamsoft.pydroid.util.PersistentCache;
import com.pyamsoft.pydroid.util.StringUtil;
import timber.log.Timber;

public class MainActivity extends DonationActivity
    implements MainPresenter.MainView, RatingDialog.ChangeLogProvider {

  @NonNull private static final String KEY_PRESENTER = "key_main_presenter";
  @BindView(R.id.toolbar) Toolbar toolbar;
  @SuppressWarnings("WeakerAccess") MainPresenter presenter;
  private Unbinder unbinder;
  private long loaderKey;

  @Override public void onCreate(final @Nullable Bundle savedInstanceState) {
    setTheme(R.style.Theme_PadLock_Light);
    super.onCreate(savedInstanceState);
    unbinder = ButterKnife.bind(this);
    PreferenceManager.setDefaultValues(getApplicationContext(), R.xml.preferences, false);

    loaderKey = PersistentCache.get()
        .load(KEY_PRESENTER, savedInstanceState, new PersistLoader.Callback<MainPresenter>() {
          @NonNull @Override public PersistLoader<MainPresenter> createLoader() {
            return new MainPresenterLoader(getApplicationContext());
          }

          @Override public void onPersistentLoaded(@NonNull MainPresenter persist) {
            presenter = persist;
          }
        });

    setAppBarState();
  }

  @Override protected int bindActivityToView() {
    setContentView(R.layout.activity_main);
    return R.id.ad_view;
  }

  @NonNull @Override protected String provideAdViewUnitId() {
    return getString(R.string.banner_ad_id);
  }

  @Override protected void onSaveInstanceState(Bundle outState) {
    PersistentCache.get().saveKey(outState, KEY_PRESENTER, loaderKey);
    super.onSaveInstanceState(outState);
  }

  @Override protected void onStart() {
    super.onStart();
    presenter.bindView(this);
    showLockList(false);
  }

  @Override protected void onStop() {
    super.onStop();
    presenter.unbindView();
  }

  @NonNull @Override public String provideApplicationName() {
    return "PadLock";
  }

  @Override public int getCurrentApplicationVersion() {
    return BuildConfig.VERSION_CODE;
  }

  private void setAppBarState() {
    setSupportActionBar(toolbar);
    toolbar.setTitle(getString(R.string.app_name));
  }

  private void showLockList(boolean forceRefresh) {
    supportInvalidateOptionsMenu();
    final FragmentManager fragmentManager = getSupportFragmentManager();
    if ((fragmentManager.findFragmentByTag(LockListFragment.TAG) == null
        && fragmentManager.findFragmentByTag(SettingsFragment.TAG) == null
        && fragmentManager.findFragmentByTag(AboutLibrariesFragment.TAG) == null) || forceRefresh) {
      fragmentManager.beginTransaction()
          .replace(R.id.main_view_container, LockListFragment.newInstance(forceRefresh),
              LockListFragment.TAG)
          .commitNow();
    }
  }

  @CheckResult @NonNull public View getSettingsMenuItemView() {
    final View amv = toolbar.getChildAt(1);
    if (amv != null && amv instanceof ActionMenuView) {
      final ActionMenuView actions = (ActionMenuView) amv;
      // Settings gear is the second item
      return actions.getChildAt(1);
    } else {
      throw new RuntimeException("Could not locate view for Settings menu item");
    }
  }

  @Override protected void onDestroy() {
    super.onDestroy();
    if (!isChangingConfigurations()) {
      PersistentCache.get().unload(loaderKey);
    }
    unbinder.unbind();
  }

  @Override protected boolean shouldConfirmBackPress() {
    return true;
  }

  @Override public void onBackPressed() {
    final FragmentManager fragmentManager = getSupportFragmentManager();
    final int backStackCount = fragmentManager.getBackStackEntryCount();
    if (backStackCount > 0) {
      fragmentManager.popBackStack();
    } else {
      super.onBackPressed();
    }
  }

  @Override public boolean onOptionsItemSelected(final @NonNull MenuItem item) {
    final int itemId = item.getItemId();
    boolean handled;
    switch (itemId) {
      case android.R.id.home:
        onBackPressed();
        handled = true;
        break;
      default:
        handled = false;
    }
    return handled || super.onOptionsItemSelected(item);
  }

  @Override public void onDidNotAgreeToTerms() {
    Timber.e("Did not agree to terms");
    finish();
  }

  @Override protected void onPostResume() {
    super.onPostResume();
    AnimUtil.animateActionBarToolbar(toolbar);
    RatingDialog.showRatingDialog(this, this);
    presenter.showTermsDialog();
  }

  @Override public void showUsageTermsDialog() {
    AppUtil.guaranteeSingleDialogFragment(getSupportFragmentManager(), new AgreeTermsDialog(),
        AgreeTermsDialog.TAG);
  }

  @NonNull @Override public Spannable getChangeLogText() {
    // The changelog text
    final String title = "What's New in Version " + BuildConfig.VERSION_NAME;
    final String line1 =
        "BUGFIX: Fixed an issue where whitelisting an Entry would cause the Lock Screen to fail.";

    // Turn it into a spannable
    final Spannable spannable = StringUtil.createLineBreakBuilder(title, line1);

    int start = 0;
    int end = title.length();
    final int largeSize =
        StringUtil.getTextSizeFromAppearance(this, android.R.attr.textAppearanceLarge);
    final int largeColor =
        StringUtil.getTextColorFromAppearance(this, android.R.attr.textAppearanceLarge);
    final int smallSize =
        StringUtil.getTextSizeFromAppearance(this, android.R.attr.textAppearanceSmall);
    final int smallColor =
        StringUtil.getTextColorFromAppearance(this, android.R.attr.textAppearanceSmall);

    StringUtil.boldSpan(spannable, start, end);
    StringUtil.sizeSpan(spannable, start, end, largeSize);
    StringUtil.colorSpan(spannable, start, end, largeColor);

    start += end + 2;
    end += 2 + line1.length();

    StringUtil.sizeSpan(spannable, start, end, smallSize);
    StringUtil.colorSpan(spannable, start, end, smallColor);

    return spannable;
  }

  @Override public int getChangeLogIcon() {
    return R.mipmap.ic_launcher;
  }

  @Override public void forceRefresh() {
    Timber.d("Force lock list refresh");
    final FragmentManager fragmentManager = getSupportFragmentManager();
    fragmentManager.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
    showLockList(true);
  }
}

