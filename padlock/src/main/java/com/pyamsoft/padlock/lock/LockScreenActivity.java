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

package com.pyamsoft.padlock.lock;

import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
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
import com.pyamsoft.padlock.base.db.PadLockEntry;
import com.pyamsoft.padlock.databinding.ActivityLockBinding;
import com.pyamsoft.padlock.loader.AppIconLoader;
import com.pyamsoft.padlock.lock.common.LockTypePresenter;
import com.pyamsoft.pydroid.ui.app.activity.ActivityBase;
import com.pyamsoft.pydroid.ui.loader.ImageLoader;
import com.pyamsoft.pydroid.ui.loader.LoaderHelper;
import com.pyamsoft.pydroid.ui.loader.loaded.Loaded;
import com.pyamsoft.pydroid.util.DialogUtil;
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

  @NonNull private final Intent home;
  @SuppressWarnings("WeakerAccess") @Inject LockScreenPresenter presenter;
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
  @NonNull private Loaded appIcon = LoaderHelper.empty();

  public LockScreenActivity() {
    home = new Intent(Intent.ACTION_MAIN);
    home.addCategory(Intent.CATEGORY_HOME);
    home.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
  }

  /**
   * Starts a LockScreenActivity instance
   */
  public static void start(@NonNull Context context, @NonNull PadLockEntry entry,
      @NonNull String realName) {
    Intent intent = new Intent(context.getApplicationContext(), LockScreenActivity.class);
    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
        | Intent.FLAG_ACTIVITY_MULTIPLE_TASK
        | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
    intent.putExtra(LockScreenActivity.ENTRY_PACKAGE_NAME, entry.packageName());
    intent.putExtra(LockScreenActivity.ENTRY_ACTIVITY_NAME, entry.activityName());
    intent.putExtra(LockScreenActivity.ENTRY_LOCK_CODE, entry.lockCode());
    intent.putExtra(LockScreenActivity.ENTRY_IS_SYSTEM, entry.systemApplication());
    intent.putExtra(LockScreenActivity.ENTRY_REAL_NAME, realName);
    intent.putExtra(LockScreenActivity.ENTRY_LOCK_UNTIL_TIME, entry.lockUntilTime());

    if (entry.whitelist()) {
      throw new RuntimeException("Cannot launch LockScreen for whitelisted applications");
    }

    context.getApplicationContext().startActivity(intent);
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

    appIcon = LoaderHelper.unload(appIcon);
    appIcon = ImageLoader.fromLoader(AppIconLoader.forPackageName(lockedPackageName))
        .into(binding.lockImage);

    presenter.closeOldAndAwaitSignal(lockedPackageName, lockedActivityName, () -> {
      Timber.w("Close event received for this LockScreen: %s", LockScreenActivity.this);
      finish();
    });

    supportInvalidateOptionsMenu();
  }

  @Override protected void onStop() {
    super.onStop();
    presenter.stop();
    appIcon = LoaderHelper.unload(appIcon);
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
    presenter.destroy();

    Timber.d("Clear currently locked");
    binding.unbind();
  }

  @Override public void finish() {
    super.finish();
    overridePendingTransition(0, 0);
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
        DialogUtil.guaranteeSingleDialogFragment(this, new ForgotPasswordDialog(),
            FORGOT_PASSWORD_TAG);
        break;
      case R.id.menu_exclude:
        item.setChecked(!item.isChecked());
        break;
      case R.id.menu_lockscreen_info:
        DialogUtil.guaranteeSingleDialogFragment(this,
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
