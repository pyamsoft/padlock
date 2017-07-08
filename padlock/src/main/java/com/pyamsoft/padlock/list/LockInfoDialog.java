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

package com.pyamsoft.padlock.list;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;
import com.mikepenz.fastadapter.commons.adapters.FastItemAdapter;
import com.pyamsoft.padlock.Injector;
import com.pyamsoft.padlock.PadLock;
import com.pyamsoft.padlock.R;
import com.pyamsoft.padlock.databinding.DialogLockInfoBinding;
import com.pyamsoft.padlock.loader.AppIconLoader;
import com.pyamsoft.padlock.model.ActivityEntry;
import com.pyamsoft.padlock.model.AppEntry;
import com.pyamsoft.pydroid.loader.ImageLoader;
import com.pyamsoft.pydroid.loader.LoaderHelper;
import com.pyamsoft.pydroid.loader.loaded.Loaded;
import com.pyamsoft.pydroid.ui.util.DialogUtil;
import com.pyamsoft.pydroid.util.AppUtil;
import javax.inject.Inject;
import timber.log.Timber;

public class LockInfoDialog extends DialogFragment {

  @NonNull public static final String TAG = "LockInfoDialog";
  @NonNull private static final String ARG_APP_PACKAGE_NAME = "app_packagename";
  @NonNull private static final String ARG_APP_NAME = "app_name";
  @NonNull private static final String ARG_APP_SYSTEM = "app_system";
  @SuppressWarnings("WeakerAccess") @Inject LockInfoPresenter presenter;
  @SuppressWarnings("WeakerAccess") FastItemAdapter<LockInfoItem> fastItemAdapter;
  @SuppressWarnings("WeakerAccess") DialogLockInfoBinding binding;
  String appPackageName;
  boolean appIsSystem;
  boolean listDoneRefreshing;
  private String appName;
  private DividerItemDecoration dividerDecoration;
  private FilterListDelegate filterListDelegate;
  @NonNull private Loaded appIcon = LoaderHelper.empty();

  @CheckResult @NonNull public static LockInfoDialog newInstance(@NonNull AppEntry appEntry) {
    final LockInfoDialog fragment = new LockInfoDialog();
    final Bundle args = new Bundle();

    args.putString(ARG_APP_PACKAGE_NAME, appEntry.packageName());
    args.putString(ARG_APP_NAME, appEntry.name());
    args.putBoolean(ARG_APP_SYSTEM, appEntry.system());

    fragment.setArguments(args);
    return fragment;
  }

  @NonNull @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
    final Dialog dialog = super.onCreateDialog(savedInstanceState);
    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
    return dialog;
  }

  @Override public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    listDoneRefreshing = false;

    appPackageName = getArguments().getString(ARG_APP_PACKAGE_NAME, null);
    appName = getArguments().getString(ARG_APP_NAME, null);
    appIsSystem = getArguments().getBoolean(ARG_APP_SYSTEM, false);

    if (appPackageName == null || appName == null) {
      throw new NullPointerException("App information is NULL");
    }

    Injector.get().provideComponent().inject(this);
  }

  @Nullable @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    filterListDelegate = new FilterListDelegate();
    fastItemAdapter = new FastItemAdapter<>();
    binding = DialogLockInfoBinding.inflate(inflater, container, false);
    return binding.getRoot();
  }

  @Override public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    setupToolbar();
    binding.lockInfoPackageName.setText(appPackageName);
    binding.lockInfoSystem.setText((appIsSystem ? "YES" : "NO"));
    filterListDelegate.onViewCreated(fastItemAdapter);
    setupSwipeRefresh();
    setupRecyclerView();
  }

  private void setupToolbar() {
    ViewCompat.setElevation(binding.lockInfoToolbar, AppUtil.convertToDP(getContext(), 4));
    binding.lockInfoToolbar.setTitle(appName);
    binding.lockInfoToolbar.setNavigationOnClickListener(v -> dismiss());
    binding.lockInfoToolbar.inflateMenu(R.menu.search_menu);
    filterListDelegate.onPrepareOptionsMenu(binding.lockInfoToolbar.getMenu(), fastItemAdapter);
  }

  private void setupSwipeRefresh() {
    binding.lockInfoSwipeRefresh.setColorSchemeResources(R.color.blue500, R.color.amber700,
        R.color.blue700, R.color.amber500);
    binding.lockInfoSwipeRefresh.setOnRefreshListener(() -> {
      Timber.d("onRefresh");
      refreshList(true);
    });
  }

  private void setupRecyclerView() {
    dividerDecoration = new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL);

    fastItemAdapter.getItemFilter().withFilterPredicate((item, query) -> {
      final String queryString = String.valueOf(query).toLowerCase().trim();
      return item.filterAgainst(queryString);
    });

    LinearLayoutManager manager = new LinearLayoutManager(getContext());
    manager.setItemPrefetchEnabled(true);
    manager.setInitialPrefetchItemCount(3);
    binding.lockInfoRecycler.setLayoutManager(manager);
    binding.lockInfoRecycler.setClipToPadding(false);
    binding.lockInfoRecycler.setHasFixedSize(false);
    binding.lockInfoRecycler.addItemDecoration(dividerDecoration);
    binding.lockInfoRecycler.setAdapter(fastItemAdapter);
  }

  @Override public void onDestroyView() {
    super.onDestroyView();
    filterListDelegate.onDestroyView();

    binding.lockInfoRecycler.removeItemDecoration(dividerDecoration);
    binding.lockInfoRecycler.setOnClickListener(null);
    binding.lockInfoRecycler.setLayoutManager(null);
    binding.lockInfoRecycler.setAdapter(null);
    binding.unbind();
  }

  @Override public void onDestroy() {
    super.onDestroy();
    presenter.destroy();
    PadLock.getRefWatcher(this).watch(this);
  }

  @Override public void onStart() {
    super.onStart();

    appIcon = LoaderHelper.unload(appIcon);
    appIcon = ImageLoader.fromLoader(AppIconLoader.forPackageName(appPackageName))
        .into(binding.lockInfoIcon);

    if (!listDoneRefreshing) {
      refreshList(false);
    }
  }

  @Override public void onStop() {
    super.onStop();
    presenter.stop();
    appIcon = LoaderHelper.unload(appIcon);
  }

  @Override public void onResume() {
    super.onResume();
    // The dialog is super small for some reason. We have to set the size manually, in onResume
    final Window window = getDialog().getWindow();
    if (window != null) {
      window.setLayout(WindowManager.LayoutParams.MATCH_PARENT,
          WindowManager.LayoutParams.WRAP_CONTENT);
    }
  }

  void refreshList(boolean force) {
    presenter.populateList(appPackageName, force, new LockInfoPresenter.PopulateListCallback() {

      private void setRefreshing(boolean refresh) {
        // In case the configuration changes, we do the animation again
        binding.lockInfoSwipeRefresh.post(() -> {
          if (binding != null) {
            if (binding.lockInfoSwipeRefresh != null) {
              binding.lockInfoSwipeRefresh.setRefreshing(refresh);
            }
          }
        });
      }

      @Override public void onListPopulateBegin() {
        listDoneRefreshing = false;
        setRefreshing(true);
        fastItemAdapter.clear();
      }

      @Override public void onEntryAddedToList(@NonNull ActivityEntry entry) {
        fastItemAdapter.add(new LockInfoItem(appPackageName, appIsSystem, entry));
      }

      @Override public void onListPopulated() {
        listDoneRefreshing = true;
        setRefreshing(false);

        if (fastItemAdapter.getAdapterItemCount() > 0) {
          Timber.d("Refresh finished");
          presenter.showOnBoarding(new LockInfoPresenter.OnBoardingCallback() {
            @Override public void onShowOnboarding() {
              Timber.d("Show onboarding");
              // TODO
            }

            @Override public void onOnboardingComplete() {
              // TODO
            }
          });
        } else {
          Toast.makeText(getContext(), "Error while loading list. Please try again.",
              Toast.LENGTH_SHORT).show();
        }
      }

      @Override public void onListPopulateError() {
        DialogUtil.guaranteeSingleDialogFragment(getActivity(), new ErrorDialog(), "error");
      }
    });
  }
}
