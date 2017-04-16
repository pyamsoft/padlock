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

package com.pyamsoft.padlock.settings;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.view.View;
import android.widget.Toast;
import com.pyamsoft.padlock.Injector;
import com.pyamsoft.padlock.PadLock;
import com.pyamsoft.padlock.R;
import com.pyamsoft.padlock.main.MainActivity;
import com.pyamsoft.padlock.pin.PinEntryDialog;
import com.pyamsoft.padlock.service.PadLockService;
import com.pyamsoft.pydroid.ui.about.AboutLibrariesFragment;
import com.pyamsoft.pydroid.ui.app.fragment.ActionBarSettingsPreferenceFragment;
import com.pyamsoft.pydroid.ui.helper.ProgressOverlay;
import com.pyamsoft.pydroid.ui.helper.ProgressOverlayHelper;
import com.pyamsoft.pydroid.ui.util.ActionBarUtil;
import com.pyamsoft.pydroid.util.DialogUtil;
import javax.inject.Inject;
import timber.log.Timber;

public class SettingsFragment extends ActionBarSettingsPreferenceFragment {

  @NonNull public static final String TAG = "SettingsPreferenceFragment";
  @SuppressWarnings("WeakerAccess") @Inject SettingsPreferencePresenter presenter;
  @NonNull ProgressOverlay overlay = ProgressOverlay.empty();

  @NonNull @Override protected AboutLibrariesFragment.BackStackState isLastOnBackStack() {
    return AboutLibrariesFragment.BackStackState.LAST;
  }

  @Override public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    Injector.get().provideComponent().plusSettingsPreferenceComponent().inject(this);
  }

  @CheckResult @NonNull SettingsPreferencePresenter getPresenter() {
    if (presenter == null) {
      throw new NullPointerException("Presenter is NULL");
    }

    return presenter;
  }

  @Override public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    final Preference clearDb = findPreference(getString(R.string.clear_db_key));
    clearDb.setOnPreferenceClickListener(preference -> {
      Timber.d("Clear DB onClick");
      DialogUtil.guaranteeSingleDialogFragment(getActivity(),
          ConfirmationDialog.newInstance(ConfirmEvent.Type.DATABASE), "confirm_dialog");
      return true;
    });

    final Preference installListener = findPreference(getString(R.string.install_listener_key));
    installListener.setOnPreferenceClickListener(preference -> {
      presenter.setApplicationInstallReceiverState();
      return true;
    });

    final ListPreference lockType =
        (ListPreference) findPreference(getString(R.string.lock_screen_type_key));
    lockType.setOnPreferenceChangeListener((preference, newValue) -> {
      presenter.checkLockType(new SettingsPreferencePresenter.LockTypeCallback() {
        @Override public void onBegin() {
          overlay = ProgressOverlayHelper.dispose(overlay);
          overlay = ProgressOverlay.builder().build(getActivity());
        }

        @Override public void onLockTypeChangeAccepted() {
          if (newValue instanceof String) {
            String value = (String) newValue;
            Timber.d("Change accepted, set value: %s", value);
            lockType.setValue(value);
          }
        }

        @Override public void onLockTypeChangePrevented() {
          Toast.makeText(getContext(),
              "Must clear Master Password before changing Lock Screen Type", Toast.LENGTH_SHORT)
              .show();
          DialogUtil.guaranteeSingleDialogFragment(getActivity(),
              PinEntryDialog.newInstance(getContext().getPackageName(),
                  getActivity().getClass().getName()), PinEntryDialog.TAG);
        }

        @Override public void onLockTypeChangeError(@NonNull Throwable throwable) {
          Toast.makeText(getContext(), "Error: " + throwable.getMessage(), Toast.LENGTH_SHORT)
              .show();
        }

        @Override public void onEnd() {
          overlay = ProgressOverlayHelper.dispose(overlay);
        }
      });

      Timber.d("Always return false here, the callback will decide if we can set value properly");
      return false;
    });
  }

  @Override protected int getPreferenceXmlResId() {
    return R.xml.preferences;
  }

  @Override protected int getRootViewContainer() {
    return R.id.main_view_container;
  }

  @NonNull @Override protected String getApplicationName() {
    return getString(R.string.app_name);
  }

  @Override protected void onClearAllClicked() {
    DialogUtil.guaranteeSingleDialogFragment(getActivity(),
        ConfirmationDialog.newInstance(ConfirmEvent.Type.ALL), "confirm_dialog");
  }

  @Override protected void onLicenseItemClicked() {
    ActionBarUtil.setActionBarUpEnabled(getActivity(), true);
    Activity activity = getActivity();
    if (activity instanceof MainActivity) {
      ((MainActivity) activity).hideBottomBar();
    }
    super.onLicenseItemClicked();
  }

  @Override public void onStart() {
    super.onStart();
    presenter.registerOnBus(new SettingsPreferencePresenter.ClearCallback() {
      @Override public void onClearAll() {
        Timber.d("Everything is cleared, kill self");
        try {
          PadLockService.finish();
        } catch (NullPointerException e) {
          Timber.e(e, "Expected NPE when Service is NULL");
        }
        final ActivityManager activityManager =
            (ActivityManager) getActivity().getApplicationContext()
                .getSystemService(Context.ACTIVITY_SERVICE);
        activityManager.clearApplicationUserData();
      }

      @Override public void onClearDatabase() {
        Toast.makeText(getContext(), "Locked application database has been cleared",
            Toast.LENGTH_SHORT).show();
      }
    });
  }

  @Override public void onStop() {
    super.onStop();
    presenter.stop();
  }

  @Override public void onResume() {
    super.onResume();
    setActionBarUpEnabled(false);

    Activity activity = getActivity();
    if (activity instanceof MainActivity) {
      ((MainActivity) activity).showBottomBar();
    }
  }

  @Override public void onDestroy() {
    super.onDestroy();
    presenter.destroy();
    PadLock.getRefWatcher(this).watch(this);
  }
}
