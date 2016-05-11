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

package com.pyamsoft.padlock.app.settings;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.Spannable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import com.pyamsoft.padlock.PadLock;
import com.pyamsoft.padlock.R;
import com.pyamsoft.padlock.app.main.PageAwareFragment;
import com.pyamsoft.padlock.dagger.db.DBModule;
import com.pyamsoft.padlock.dagger.lockscreen.LockScreenModule;
import com.pyamsoft.padlock.dagger.settings.DaggerSettingsComponent;
import com.pyamsoft.padlock.dagger.settings.SettingsModule;
import com.pyamsoft.pydroid.util.AppUtil;
import com.pyamsoft.pydroid.util.StringUtil;
import javax.inject.Inject;

public final class SettingsFragment extends PageAwareFragment
    implements SettingsPresenter.SettingsView {

  @Inject SettingsPresenter presenter;

  @BindView(R.id.settings_ignoretime_radio_none) RadioButton ignoreNone;
  @BindView(R.id.settings_ignoretime_radio_five) RadioButton ignoreFive;
  @BindView(R.id.settings_ignoretime_radio_ten) RadioButton ignoreTen;
  @BindView(R.id.settings_ignoretime_radio_thirty) RadioButton ignoreThirty;
  @BindView(R.id.settings_timeout_radio_none) RadioButton timeoutNone;
  @BindView(R.id.settings_timeout_radio_one) RadioButton timeoutOne;
  @BindView(R.id.settings_timeout_radio_five) RadioButton timeoutFive;
  @BindView(R.id.settings_timeout_radio_ten) RadioButton timeoutTen;
  @BindView(R.id.settings_clear_database) Button clearDatabase;
  @BindView(R.id.settings_clear_all) Button clearAll;
  @BindView(R.id.settings_ignoretime_title) TextView ignoreTitle;
  @BindView(R.id.settings_timeout_title) TextView timeoutTitle;

  private Unbinder unbinder;

  @Override public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    DaggerSettingsComponent.builder()
        .lockScreenModule(new LockScreenModule())
        .settingsModule(new SettingsModule())
        .dBModule(new DBModule())
        .padLockComponent(PadLock.padLockComponent(this))
        .build()
        .inject(this);
  }

  @Nullable @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    final View view = inflater.inflate(R.layout.fragment_settings, container, false);
    unbinder = ButterKnife.bind(this, view);
    presenter.onCreateView(this);
    return view;
  }

  @Override public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    setupIgnoreTitle();
    setupTimeoutTitle();

    presenter.setIgnorePeriodFromPreference();
    presenter.setTimeoutPeriodFromPreference();

    setOnCheckListenerIgnoreNone();
    setOnCheckListenerIgnoreFive();
    setOnCheckListenerIgnoreTen();
    setOnCheckListenerIgnoreThirty();

    setOnCheckListenerTimeoutNone();
    setOnCheckListenerTimeoutOne();
    setOnCheckListenerTimeoutFive();
    setOnCheckListenerTimeoutTen();

    clearDatabase.setOnClickListener(
        view1 -> AppUtil.guaranteeSingleDialogFragment(getFragmentManager(),
            ConfirmationDialog.newInstance(0), "confirm_dialog"));

    clearAll.setOnClickListener(view1 -> AppUtil.guaranteeSingleDialogFragment(getFragmentManager(),
        ConfirmationDialog.newInstance(1), "confirm_dialog"));
  }

  private void setupIgnoreTitle() {
    final String title = "Ignore Time Period";
    final String description =
        "Once an app is unlocked, PadLock will ignore it for the following amount of time if it is opened again later.";
    final Spannable spannable = StringUtil.createBuilder(title, "\n", description);
    final int textSizeLarge =
        StringUtil.getTextSizeFromAppearance(getContext(), android.R.attr.textAppearanceLarge);
    final int textColorLarge =
        StringUtil.getTextColorFromAppearance(getContext(), android.R.attr.textAppearanceLarge);
    final int textSizeSmall =
        StringUtil.getTextSizeFromAppearance(getContext(), android.R.attr.textAppearanceSmall);
    final int textColorSmall =
        StringUtil.getTextColorFromAppearance(getContext(), android.R.attr.textAppearanceSmall);

    StringUtil.colorSpan(spannable, 0, title.length(), textColorLarge);
    StringUtil.sizeSpan(spannable, 0, title.length(), textSizeLarge);

    StringUtil.colorSpan(spannable, title.length(), spannable.length(), textColorSmall);
    StringUtil.sizeSpan(spannable, title.length(), spannable.length(), textSizeSmall);

    ignoreTitle.setText(spannable);
  }

  private void setupTimeoutTitle() {
    final String title = "Timeout Time Period";
    final String description =
        "After too many incorrect attempts, the application will be locked for the amount of time";
    final Spannable spannable = StringUtil.createBuilder(title, "\n", description);
    final int textSizeLarge =
        StringUtil.getTextSizeFromAppearance(getContext(), android.R.attr.textAppearanceLarge);
    final int textColorLarge =
        StringUtil.getTextColorFromAppearance(getContext(), android.R.attr.textAppearanceLarge);
    final int textSizeSmall =
        StringUtil.getTextSizeFromAppearance(getContext(), android.R.attr.textAppearanceSmall);
    final int textColorSmall =
        StringUtil.getTextColorFromAppearance(getContext(), android.R.attr.textAppearanceSmall);

    StringUtil.colorSpan(spannable, 0, title.length(), textColorLarge);
    StringUtil.sizeSpan(spannable, 0, title.length(), textSizeLarge);

    StringUtil.colorSpan(spannable, title.length(), spannable.length(), textColorSmall);
    StringUtil.sizeSpan(spannable, title.length(), spannable.length(), textSizeSmall);

    timeoutTitle.setText(spannable);
  }

  @Override public void onResume() {
    super.onResume();

    presenter.onResume();
  }

  @Override public void onPause() {
    super.onPause();

    presenter.onPause();
  }

  @Override public void onDestroyView() {
    super.onDestroyView();

    presenter.onDestroyView();
    if (unbinder != null) {
      unbinder.unbind();
    }
  }

  @Override public void setIgnorePeriodNone() {
    ignoreNone.setOnCheckedChangeListener(null);
    ignoreNone.setChecked(true);
    setOnCheckListenerIgnoreNone();
  }

  @Override public void setIgnorePeriodFive() {
    ignoreFive.setOnCheckedChangeListener(null);
    ignoreFive.setChecked(true);
    setOnCheckListenerIgnoreFive();
  }

  @Override public void setIgnorePeriodTen() {
    ignoreTen.setOnCheckedChangeListener(null);
    ignoreTen.setChecked(true);
    setOnCheckListenerIgnoreTen();
  }

  @Override public void setIgnorePeriodThirty() {
    ignoreThirty.setOnCheckedChangeListener(null);
    ignoreThirty.setChecked(true);
    setOnCheckListenerIgnoreThirty();
  }

  @Override public void setTimeoutPeriodNone() {
    timeoutNone.setOnCheckedChangeListener(null);
    timeoutNone.setChecked(true);
    setOnCheckListenerTimeoutNone();
  }

  @Override public void setTimeoutPeriodOne() {
    timeoutOne.setOnCheckedChangeListener(null);
    timeoutOne.setChecked(true);
    setOnCheckListenerTimeoutOne();
  }

  @Override public void setTimeoutPeriodFive() {
    timeoutFive.setOnCheckedChangeListener(null);
    timeoutFive.setChecked(true);
    setOnCheckListenerTimeoutFive();
  }

  @Override public void setTimeoutPeriodTen() {
    timeoutTen.setOnCheckedChangeListener(null);
    timeoutTen.setChecked(true);
    setOnCheckListenerTimeoutTen();
  }

  private void setOnCheckListenerIgnoreNone() {
    ignoreNone.setOnCheckedChangeListener((compoundButton, b) -> {
      if (b) {
        presenter.setIgnorePeriodNone();
      }
    });
  }

  private void setOnCheckListenerIgnoreFive() {
    ignoreFive.setOnCheckedChangeListener((compoundButton, b) -> {
      if (b) {
        presenter.setIgnorePeriodFive();
      }
    });
  }

  private void setOnCheckListenerIgnoreTen() {
    ignoreTen.setOnCheckedChangeListener((compoundButton, b) -> {
      if (b) {
        presenter.setIgnorePeriodTen();
      }
    });
  }

  private void setOnCheckListenerIgnoreThirty() {
    ignoreThirty.setOnCheckedChangeListener((compoundButton, b) -> {
      if (b) {
        presenter.setIgnorePeriodThirty();
      }
    });
  }

  private void setOnCheckListenerTimeoutNone() {
    timeoutNone.setOnCheckedChangeListener((compoundButton, b) -> {
      if (b) {
        presenter.setTimeoutPeriodNone();
      }
    });
  }

  private void setOnCheckListenerTimeoutOne() {
    timeoutOne.setOnCheckedChangeListener((compoundButton, b) -> {
      if (b) {
        presenter.setTimeoutPeriodOne();
      }
    });
  }

  private void setOnCheckListenerTimeoutFive() {
    timeoutFive.setOnCheckedChangeListener((compoundButton, b) -> {
      if (b) {
        presenter.setTimeoutPeriodFive();
      }
    });
  }

  private void setOnCheckListenerTimeoutTen() {
    timeoutTen.setOnCheckedChangeListener((compoundButton, b) -> {
      if (b) {
        presenter.setTimeoutPeriodTen();
      }
    });
  }
}
