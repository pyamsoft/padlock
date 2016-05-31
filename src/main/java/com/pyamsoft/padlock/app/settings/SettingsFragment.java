package com.pyamsoft.padlock.app.settings;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.pyamsoft.padlock.PadLock;
import com.pyamsoft.padlock.R;
import com.pyamsoft.padlock.dagger.settings.DaggerSettingsComponent;
import com.pyamsoft.pydroid.support.RatingDialog;
import com.pyamsoft.pydroid.util.AppUtil;
import javax.inject.Inject;
import timber.log.Timber;

public final class SettingsFragment extends PreferenceFragmentCompat
    implements SettingsPresenter.SettingsView {

  @Inject SettingsPresenter presenter;

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    DaggerSettingsComponent.builder()
        .padLockComponent(PadLock.padLockComponent(this))
        .build()
        .inject(this);
    presenter.onCreateView(this);
    return super.onCreateView(inflater, container, savedInstanceState);
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
    presenter.onDestroyView();
    super.onDestroyView();
  }

  @Override public void onCreatePreferences(@Nullable Bundle bundle, @Nullable String s) {
    addPreferencesFromResource(R.xml.preferences);

    final Preference clearDb = findPreference(getString(R.string.clear_db_key));
    clearDb.setOnPreferenceClickListener(preference -> {
      Timber.d("Clear DB onClick");
      presenter.confirmDatabaseClear();
      return true;
    });

    final Preference resetAll = findPreference(getString(R.string.clear_all_key));
    resetAll.setOnPreferenceClickListener(preference -> {
      Timber.d("Reset settings onClick");
      presenter.confirmSettingsClear();
      return true;
    });

    final Preference upgradeInfo = findPreference(getString(R.string.upgrade_info_key));
    upgradeInfo.setOnPreferenceClickListener(preference -> {
      final FragmentActivity activity = getActivity();
      if (activity instanceof RatingDialog.ChangeLogProvider) {
        final RatingDialog.ChangeLogProvider provider = (RatingDialog.ChangeLogProvider) activity;
        RatingDialog.showRatingDialog(activity, provider, true);
      } else {
        throw new ClassCastException("Activity is not a change log provider");
      }
      return true;
    });
  }

  @Override public void onConfirmAttempt(int code) {
    AppUtil.guaranteeSingleDialogFragment(getFragmentManager(),
        ConfirmationDialog.newInstance(code), "confirm_dialog");
  }
}
