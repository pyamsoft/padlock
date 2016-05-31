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

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.v7.widget.Toolbar;
import android.text.Spannable;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import com.pyamsoft.padlock.BuildConfig;
import com.pyamsoft.padlock.PadLock;
import com.pyamsoft.padlock.R;
import com.pyamsoft.padlock.app.list.LockListFragment;
import com.pyamsoft.padlock.app.service.PadLockService;
import com.pyamsoft.padlock.dagger.main.DaggerMainComponent;
import com.pyamsoft.padlock.dagger.main.MainModule;
import com.pyamsoft.pydroid.base.DonationActivityBase;
import com.pyamsoft.pydroid.support.RatingDialog;
import com.pyamsoft.pydroid.tool.AsyncVectorDrawableTask;
import com.pyamsoft.pydroid.tool.DataHolderFragment;
import com.pyamsoft.pydroid.util.AppUtil;
import com.pyamsoft.pydroid.util.StringUtil;
import javax.inject.Inject;
import timber.log.Timber;

public class MainActivity extends DonationActivityBase
    implements MainPresenter.MainView, RatingDialog.ChangeLogProvider {

  @NonNull private static final String USAGE_TERMS_TAG = "usage_terms";
  private static final int VECTOR_TASK_SIZE = 2;

  @NonNull private final AsyncVectorDrawableTask[] tasks;
  @Nullable private Unbinder unbinder;
  private boolean firstCreate;
  @BindView(R.id.main_view) CoordinatorLayout mainView;
  @BindView(R.id.main_enable_service) LinearLayout enableService;
  @BindView(R.id.main_service_button) Button serviceButton;
  @BindView(R.id.toolbar) Toolbar toolbar;
  @Inject MainPresenter presenter;

  public MainActivity() {
    tasks = new AsyncVectorDrawableTask[VECTOR_TASK_SIZE];
  }

  @Override public void onCreate(final @Nullable Bundle savedInstanceState) {
    setTheme(R.style.Theme_PadLock_Light);
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    PreferenceManager.setDefaultValues(getApplicationContext(), R.xml.preferences, false);
    unbinder = ButterKnife.bind(this);

    firstCreate = (savedInstanceState == null);

    DaggerMainComponent.builder()
        .padLockComponent(PadLock.padLockComponent(this))
        .mainModule(new MainModule())
        .build()
        .inject(this);

    presenter.onCreateView(this);

    setAppBarState();
    setupAccessibilityButton();
  }

  private void setupAccessibilityButton() {
    serviceButton.setOnClickListener(view -> {
      final Intent accessibilityServiceIntent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
      startActivity(accessibilityServiceIntent);
    });
  }

  private void showAccessibilityPrompt() {
    supportInvalidateOptionsMenu();
    enableService.setVisibility(View.VISIBLE);
  }

  private void showLockList() {
    supportInvalidateOptionsMenu();
    enableService.setVisibility(View.GONE);
    if (firstCreate) {
      getSupportFragmentManager().beginTransaction()
          .replace(R.id.main_view_container, new LockListFragment())
          .commit();
    }
  }

  private void cancelAsyncVectorTask(int position) {
    if (tasks[position] != null) {
      if (!tasks[position].isCancelled()) {
        tasks[position].cancel(true);
      }
      tasks[position] = null;
    }
  }

  @Override protected void onDestroy() {
    super.onDestroy();

    for (int i = 0; i < VECTOR_TASK_SIZE; ++i) {
      cancelAsyncVectorTask(i);
    }

    if (!isChangingConfigurations()) {
      presenter.onDestroyView();
    }

    if (unbinder != null) {
      unbinder.unbind();
    }
  }

  private void setAppBarState() {
    setSupportActionBar(toolbar);
    toolbar.setTitle(getString(R.string.app_name));
    setActionBarUpEnabled(false);
  }

  @Override protected boolean shouldConfirmBackPress() {
    return true;
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

  @NonNull @Override public String getPlayStoreAppPackage() {
    return getPackageName();
  }

  @Override protected void onResume() {
    super.onResume();
    animateActionBarToolbar(toolbar);

    presenter.onResume();
  }

  @Override public void onDidNotAgreeToTerms() {
    finish();
  }

  @Override protected void onPause() {
    super.onPause();

    presenter.onPause();
  }

  @Override protected void onPostResume() {
    super.onPostResume();
    RatingDialog.showRatingDialog(this, this);

    if (PadLockService.isRunning()) {
      showLockList();
      presenter.showTermsDialog();
    } else {
      showAccessibilityPrompt();
    }
  }

  @Override protected void onSaveInstanceState(@NonNull Bundle outState) {
    if (isFinishing()) {
      DataHolderFragment.removeAll(this);
    }

    super.onSaveInstanceState(outState);
  }

  @Override public void showUsageTermsDialog() {
    AppUtil.guaranteeSingleDialogFragment(getSupportFragmentManager(), new AgreeTermsDialog(),
        USAGE_TERMS_TAG);
  }

  @NonNull @Override public Spannable getChangeLogText() {
    // The changelog text
    final String title = "What's New in Version " + BuildConfig.VERSION_NAME;
    final String line1 =
        "BUGFIX: Any entries that were managed via the Information dialog were not properly created or deleted in the database. The recent patch should fix this issue";
    final String line2 =
        "BUGFIX: Entries will be locked even if the package does not change, and the starting Activity was not locked. Basically, things should 'work better' now.";

    // Turn it into a spannable
    final Spannable spannable = StringUtil.createBuilder(title, "\n\n", line1, "\n\n", line2);

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
    end += 2 + line1.length() + 2 + line2.length();

    StringUtil.sizeSpan(spannable, start, end, smallSize);
    StringUtil.colorSpan(spannable, start, end, smallColor);

    return spannable;
  }

  @Override public int getChangeLogIcon() {
    return R.mipmap.ic_launcher;
  }

  @NonNull @Override public String getChangeLogPackageName() {
    return getPackageName();
  }

  @Override public int getChangeLogVersion() {
    return BuildConfig.VERSION_CODE;
  }

  @Override public boolean onCreateOptionsMenu(@NonNull Menu menu) {
    Timber.d("onCreateOptionsMenu");
    return super.onCreateOptionsMenu(menu);
  }
}

