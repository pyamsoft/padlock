package com.pyamsoft.padlock.app.settings;

import android.os.Bundle;
import android.support.v7.preference.PreferenceFragmentCompat;
import com.pyamsoft.padlock.R;

public final class SettingsScreen extends PreferenceFragmentCompat {
  @Override public void onCreatePreferences(Bundle bundle, String s) {
    addPreferencesFromResource(R.xml.preferences);
  }
}
