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

package com.pyamsoft.padlock.app.accessibility;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import butterknife.BindView;
import com.pyamsoft.padlock.R;
import com.pyamsoft.pydroid.base.fragment.ActionBarFragment;

public class AccessibilityFragment extends ActionBarFragment {

  @NonNull public static final String TAG = "AccessibilityFragment";
  @NonNull private final Intent accessibilityServiceIntent =
      new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
  @BindView(R.id.main_service_button) Button serviceButton;

  @Nullable @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_accessibility_ask, container, false);
  }

  @Override public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    setupAccessibilityButton();
  }

  @Override public void onResume() {
    super.onResume();
    setActionBarUpEnabled(false);
  }

  private void setupAccessibilityButton() {
    serviceButton.setOnClickListener(view -> startActivity(accessibilityServiceIntent));
  }
}
