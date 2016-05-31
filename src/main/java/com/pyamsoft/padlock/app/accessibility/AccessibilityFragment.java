package com.pyamsoft.padlock.app.accessibility;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import com.pyamsoft.padlock.R;

public class AccessibilityFragment extends Fragment {

  @NonNull private final Intent accessibilityServiceIntent =
      new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
  @BindView(R.id.main_service_button) Button serviceButton;
  @Nullable private Unbinder unbinder;

  @Nullable @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    final View view = inflater.inflate(R.layout.fragment_accessibility_ask, container, false);
    unbinder = ButterKnife.bind(this, view);
    return view;
  }

  @Override public void onDestroyView() {
    super.onDestroyView();
    if (unbinder != null) {
      unbinder.unbind();
    }
  }

  @Override public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    setupAccessibilityButton();
  }

  private void setupAccessibilityButton() {
    serviceButton.setOnClickListener(view -> {
      startActivity(accessibilityServiceIntent);
    });
  }
}
