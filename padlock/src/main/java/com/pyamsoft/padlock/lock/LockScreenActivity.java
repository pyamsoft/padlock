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

package com.pyamsoft.padlock.lock;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import com.pyamsoft.padlock.Injector;
import com.pyamsoft.padlock.R;
import com.pyamsoft.padlock.databinding.ActivityLockBinding;
import com.pyamsoft.padlock.iconloader.AppIconLoaderPresenter;
import com.pyamsoft.padlock.list.ErrorDialog;
import com.pyamsoft.padlock.lock.common.LockTypePresenter;
import com.pyamsoft.pydroid.ui.app.activity.ActivityBase;
import com.pyamsoft.pydroid.util.AppUtil;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import timber.log.Timber;

public class LockScreenActivity extends ActivityBase {

  @NonNull public static final String ENTRY_PACKAGE_NAME = "entry_packagename";
  @NonNull public static final String ENTRY_ACTIVITY_NAME = "entry_activityname";
  @NonNull public static final String ENTRY_REAL_NAME = "real_name";
  @NonNull public static final String ENTRY_LOCK_CODE = "lock_code";
  @NonNull public static final String ENTRY_IS_SYSTEM = "is_system";
  @NonNull public static final String ENTRY_LOCK_UNTIL_TIME = "lock_until_time";
  @NonNull private static final String FORGOT_PASSWORD_TAG = "forgot_password";

  /**
   * KLUDGE This is a map that holds references to Activities
   *
   * That's bad mk.
   *
   * More of a proof of concept, only publishable if it works just, really really well and
   * is LOCKED DOWN TO NOT LEAK
   */
  @NonNull private static final Map<LockScreenEntry, LockScreenActivity> LOCK_SCREEN_MAP;

  static {
    LOCK_SCREEN_MAP = new HashMap<>();
  }

  @NonNull private final Intent home;
  @SuppressWarnings("WeakerAccess") @Inject LockScreenPresenter presenter;
  @SuppressWarnings("WeakerAccess") @Inject AppIconLoaderPresenter appIconLoaderPresenter;
  @SuppressWarnings("WeakerAccess") String lockedActivityName;
  @SuppressWarnings("WeakerAccess") String lockedPackageName;
  @SuppressWarnings("WeakerAccess") long lockUntilTime;
  ActivityLockBinding binding;
  MenuItem menuIgnoreNone;
  MenuItem menuIgnoreOne;
  MenuItem menuIgnoreFive;
  MenuItem menuIgnoreTen;
  MenuItem menuIgnoreFifteen;
  MenuItem menuIgnoreTwenty;
  MenuItem menuIgnoreThirty;
  MenuItem menuIgnoreFourtyFive;
  MenuItem menuIgnoreSixty;
  long[] ignoreTimes;
  @NonNull private final LockScreenPresenter.IgnoreTimeCallback ignoreCallback = time -> {
    if (time == ignoreTimes[0]) {
      menuIgnoreNone.setChecked(true);
    } else if (time == ignoreTimes[1]) {
      menuIgnoreOne.setChecked(true);
    } else if (time == ignoreTimes[2]) {
      menuIgnoreFive.setChecked(true);
    } else if (time == ignoreTimes[3]) {
      menuIgnoreTen.setChecked(true);
    } else if (time == ignoreTimes[4]) {
      menuIgnoreFifteen.setChecked(true);
    } else if (time == ignoreTimes[5]) {
      menuIgnoreTwenty.setChecked(true);
    } else if (time == ignoreTimes[6]) {
      menuIgnoreThirty.setChecked(true);
    } else if (time == ignoreTimes[7]) {
      menuIgnoreFourtyFive.setChecked(true);
    } else if (time == ignoreTimes[8]) {
      menuIgnoreSixty.setChecked(true);
    } else {
      Timber.e("No valid ignore time, initialize to None");
      menuIgnoreNone.setChecked(true);
    }
  };
  MenuItem menuExclude;
  String lockedRealName;
  boolean lockedSystem;
  private long ignorePeriod = -1;
  private boolean excludeEntry;

  public LockScreenActivity() {
    home = new Intent(Intent.ACTION_MAIN);
    home.addCategory(Intent.CATEGORY_HOME);
    home.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
  }

  private static void addToLockedMap(@NonNull String packageName, @NonNull String className,
      @NonNull LockScreenActivity instance) {
    final LockScreenEntry entry = LockScreenEntry.create(packageName, className);
    Timber.i("Add instance to map for activity: %s %s", packageName, className);
    if (LOCK_SCREEN_MAP.put(entry, instance) != null) {
      Timber.e("Instance for %s %s existed. I hope you called finish before this", packageName,
          className);
    }
  }

  private static void removeFromLockedMap(@NonNull String packageName, @NonNull String className) {
    final LockScreenEntry entry = LockScreenEntry.create(packageName, className);
    if (LOCK_SCREEN_MAP.remove(entry) != null) {
      Timber.i("Remove instance from map for activity: %s %s", packageName, className);
    }
  }

  @CheckResult @Nullable
  public static LockScreenActivity hasLockedMapEntry(@NonNull String packageName,
      @NonNull String className) {
    final LockScreenEntry entry = LockScreenEntry.create(packageName, className);
    return LOCK_SCREEN_MAP.get(entry);
  }

  @CallSuper @Override public void onCreate(final @Nullable Bundle savedInstanceState) {
    setTheme(R.style.Theme_PadLock_Light_Lock);
    overridePendingTransition(0, 0);
    super.onCreate(savedInstanceState);

    binding = DataBindingUtil.setContentView(this, R.layout.activity_lock);
    PreferenceManager.setDefaultValues(getApplicationContext(), R.xml.preferences, false);

    Injector.get().provideComponent().plusLockScreenComponent().inject(this);

    populateIgnoreTimes();
    getValuesFromBundle();
    setupActionBar();

    final String lockedCode = getIntent().getExtras().getString(ENTRY_LOCK_CODE);
    presenter.initializeLockScreenType(new LockTypePresenter.LockScreenTypeCallback() {

      private void pushFragment(@NonNull Fragment pushFragment, @NonNull String tag) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment fragment = fragmentManager.findFragmentByTag(LockScreenTextFragment.TAG);
        if (fragment == null) {
          fragmentManager.beginTransaction()
              .replace(R.id.lock_screen_container, pushFragment, tag)
              .commitNow();
        }
      }

      @Override public void onTypeText() {
        // Push text as child fragment
        pushFragment(
            LockScreenTextFragment.newInstance(lockedPackageName, lockedActivityName, lockedCode,
                lockedRealName, lockedSystem), LockScreenTextFragment.TAG);
      }

      @Override public void onTypePattern() {
        pushFragment(
            LockScreenPatternFragment.newInstance(lockedPackageName, lockedActivityName, lockedCode,
                lockedRealName, lockedSystem), LockScreenPatternFragment.TAG);
      }
    });
  }

  private void setupActionBar() {
    setSupportActionBar(binding.toolbar);
    ViewCompat.setElevation(binding.toolbar, 0);
  }

  private void populateIgnoreTimes() {
    final String[] stringIgnoreTimes =
        getApplicationContext().getResources().getStringArray(R.array.ignore_time_entries);
    ignoreTimes = new long[stringIgnoreTimes.length];
    for (int i = 0; i < stringIgnoreTimes.length; ++i) {
      ignoreTimes[i] = Long.parseLong(stringIgnoreTimes[i]);
    }
  }

  private void getValuesFromBundle() {
    final Bundle bundle = getIntent().getExtras();
    lockedPackageName = bundle.getString(ENTRY_PACKAGE_NAME);
    lockedActivityName = bundle.getString(ENTRY_ACTIVITY_NAME);
    lockedRealName = bundle.getString(ENTRY_REAL_NAME);
    lockUntilTime = bundle.getLong(ENTRY_LOCK_UNTIL_TIME, 0);
    lockedSystem = bundle.getBoolean(ENTRY_IS_SYSTEM, false);

    if (lockedPackageName == null || lockedActivityName == null || lockedRealName == null) {
      throw new NullPointerException("Missing required lock attributes");
    }

    // Reload options
    supportInvalidateOptionsMenu();
  }

  @Override protected void onStart() {
    super.onStart();
    Timber.d("onStart");
    presenter.loadDisplayNameFromPackage(lockedPackageName, name -> {
      Timber.d("Set toolbar name %s", name);
      binding.toolbar.setTitle(name);
      final ActionBar bar = getSupportActionBar();
      if (bar != null) {
        Timber.d("Set actionbar name %s", name);
        bar.setTitle(name);
      }
    });
    appIconLoaderPresenter.loadApplicationIcon(lockedPackageName,
        new AppIconLoaderPresenter.LoadCallback() {
          @Override public void onApplicationIconLoadedSuccess(@NonNull Drawable icon) {
            binding.lockImage.setImageDrawable(icon);
          }

          @Override public void onApplicationIconLoadedError() {
            AppUtil.guaranteeSingleDialogFragment(LockScreenActivity.this, new ErrorDialog(),
                "error");
          }
        });
    supportInvalidateOptionsMenu();

    // Add the lock map
    addToLockedMap(lockedPackageName, lockedActivityName, this);
  }

  @Override protected void onStop() {
    super.onStop();
    presenter.stop();
    appIconLoaderPresenter.stop();
    removeFromLockedMap(lockedPackageName, lockedActivityName);
  }

  @Override protected void onPause() {
    super.onPause();
    if (isFinishing() || isChangingConfigurations()) {
      Timber.d(
          "Even though a leak is reported, this should dismiss the window, and clear the leak");
      binding.toolbar.getMenu().close();
      binding.toolbar.dismissPopupMenus();
    }
  }

  @Override public void onBackPressed() {
    Timber.d("onBackPressed");
    getApplicationContext().startActivity(home);
  }

  @CallSuper @Override protected void onDestroy() {
    super.onDestroy();
    overridePendingTransition(0, 0);
    presenter.destroy();
    appIconLoaderPresenter.stop();

    Timber.d("Clear currently locked");
    binding.unbind();
  }

  @Override public void finish() {
    super.finish();
    Timber.d("Finish called, either from Us or from Outside");
  }

  @Override protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
    Timber.d("onRestoreInstanceState");
    ignorePeriod = savedInstanceState.getLong("IGNORE", -1);
    excludeEntry = savedInstanceState.getBoolean("EXCLUDE", false);
    Fragment lockScreenText =
        getSupportFragmentManager().findFragmentByTag(LockScreenTextFragment.TAG);
    if (lockScreenText instanceof LockScreenTextFragment) {
      ((LockScreenTextFragment) lockScreenText).onRestoreInstanceState(savedInstanceState);
    }
    super.onRestoreInstanceState(savedInstanceState);
  }

  @Override protected void onSaveInstanceState(@NonNull Bundle outState) {
    final long ignoreTime = getIgnoreTimeFromSelectedIndex();
    outState.putLong("IGNORE", ignoreTime);

    boolean exclude;
    try {
      exclude = menuExclude.isChecked();
    } catch (NullPointerException e) {
      exclude = false;
    }
    outState.putBoolean("EXCLUDE", exclude);
    super.onSaveInstanceState(outState);
  }

  @Override public boolean onCreateOptionsMenu(@NonNull Menu menu) {
    Timber.d("onCreateOptionsMenu");
    getMenuInflater().inflate(R.menu.lockscreen_menu, menu);
    menuIgnoreNone = menu.findItem(R.id.menu_ignore_none);
    menuIgnoreOne = menu.findItem(R.id.menu_ignore_one);
    menuIgnoreFive = menu.findItem(R.id.menu_ignore_five);
    menuIgnoreTen = menu.findItem(R.id.menu_ignore_ten);
    menuIgnoreFifteen = menu.findItem(R.id.menu_ignore_fifteen);
    menuIgnoreTwenty = menu.findItem(R.id.menu_ignore_twenty);
    menuIgnoreThirty = menu.findItem(R.id.menu_ignore_thirty);
    menuIgnoreFourtyFive = menu.findItem(R.id.menu_ignore_fourtyfive);
    menuIgnoreSixty = menu.findItem(R.id.menu_ignore_sixty);
    menuExclude = menu.findItem(R.id.menu_exclude);
    return true;
  }

  @Override public boolean onPrepareOptionsMenu(@NonNull Menu menu) {
    if (presenter != null) {
      if (ignorePeriod == -1) {
        Timber.d("No previous selection, load ignore time from preference");
        presenter.createWithDefaultIgnoreTime(ignoreCallback);
      } else {
        ignoreCallback.onInitializeWithIgnoreTime(ignorePeriod);
      }
    }

    menuExclude.setChecked(excludeEntry);
    return super.onPrepareOptionsMenu(menu);
  }

  @Override public boolean onOptionsItemSelected(@NonNull MenuItem item) {
    Timber.d("onOptionsItemSelected");
    final int itemId = item.getItemId();
    switch (itemId) {
      case R.id.menu_lockscreen_forgot:
        AppUtil.onlyLoadOnceDialogFragment(this, new ForgotPasswordDialog(), FORGOT_PASSWORD_TAG);
        break;
      case R.id.menu_exclude:
        item.setChecked(!item.isChecked());
        break;
      case R.id.menu_lockscreen_info:
        AppUtil.onlyLoadOnceDialogFragment(this,
            LockedStatDialog.newInstance(binding.toolbar.getTitle().toString(), lockedPackageName,
                lockedActivityName, lockedRealName, lockedSystem, binding.lockImage.getDrawable()),
            "info_dialog");
        break;
      default:
        item.setChecked(true);
    }
    return true;
  }

  @CheckResult long getIgnoreTimeFromSelectedIndex() {
    int index;
    try {
      if (menuIgnoreNone.isChecked()) {
        index = 0;
      } else if (menuIgnoreOne.isChecked()) {
        index = 1;
      } else if (menuIgnoreFive.isChecked()) {
        index = 2;
      } else if (menuIgnoreTen.isChecked()) {
        index = 3;
      } else if (menuIgnoreFifteen.isChecked()) {
        index = 4;
      } else if (menuIgnoreTwenty.isChecked()) {
        index = 5;
      } else if (menuIgnoreThirty.isChecked()) {
        index = 6;
      } else if (menuIgnoreFourtyFive.isChecked()) {
        index = 7;
      } else if (menuIgnoreSixty.isChecked()) {
        index = 8;
      } else {
        index = 0;
      }
    } catch (NullPointerException e) {
      Timber.w("NULL menu item, default to 0");
      index = 0;
    }

    return ignoreTimes[index];
  }
}
