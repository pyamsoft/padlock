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

package com.pyamsoft.padlock.app.list;

import android.databinding.DataBindingUtil;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.getkeepsafe.taptargetview.TapTarget;
import com.getkeepsafe.taptargetview.TapTargetView;
import com.pyamsoft.padlock.R;
import com.pyamsoft.padlock.app.main.MainActivity;
import com.pyamsoft.padlock.databinding.FragmentLockinfoBinding;
import com.pyamsoft.padlock.model.ActivityEntry;
import com.pyamsoft.padlock.model.AppEntry;
import com.pyamsoft.padlock.model.LockState;
import com.pyamsoft.pydroid.app.ListAdapterLoader;
import com.pyamsoft.pydroid.app.PersistLoader;
import com.pyamsoft.pydroid.app.fragment.ActionBarFragment;
import com.pyamsoft.pydroid.util.AppUtil;
import com.pyamsoft.pydroid.util.PersistentCache;
import timber.log.Timber;

public class LockInfoFragment extends ActionBarFragment implements LockInfoPresenter.LockInfoView {

  @NonNull public static final String TAG = "LockInfoDialog";
  @NonNull private static final String ARG_APP_PACKAGE_NAME = "app_packagename";
  @NonNull private static final String ARG_APP_NAME = "app_name";
  @NonNull private static final String ARG_APP_SYSTEM = "app_system";
  @NonNull private static final String KEY_LOAD_ADAPTER = "key_load_adapter";
  @NonNull private static final String KEY_PRESENTER = "key_presenter";

  @SuppressWarnings("WeakerAccess") LockInfoPresenter presenter;
  @SuppressWarnings("WeakerAccess") LockInfoAdapter fastItemAdapter;
  @SuppressWarnings("WeakerAccess") boolean firstRefresh;
  private FragmentLockinfoBinding binding;
  private long loadedPresenterKey;
  private long loadedAdapterKey;

  private String appPackageName;
  private String appName;
  private boolean appIsSystem;

  @Nullable private TapTargetView toggleAllTapTarget;
  @Nullable private TapTargetView defaultLockTapTarget;
  @Nullable private TapTargetView whiteLockTapTarget;
  @Nullable private TapTargetView blackLockTapTarget;

  public static LockInfoFragment newInstance(final @NonNull AppEntry appEntry) {
    final LockInfoFragment fragment = new LockInfoFragment();
    final Bundle args = new Bundle();

    args.putString(ARG_APP_PACKAGE_NAME, appEntry.packageName());
    args.putString(ARG_APP_NAME, appEntry.name());
    args.putBoolean(ARG_APP_SYSTEM, appEntry.system());

    fragment.setArguments(args);
    return fragment;
  }

  @Override public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    appPackageName = getArguments().getString(ARG_APP_PACKAGE_NAME, null);
    appName = getArguments().getString(ARG_APP_NAME, null);
    appIsSystem = getArguments().getBoolean(ARG_APP_SYSTEM, false);

    if (appPackageName == null || appName == null) {
      throw new NullPointerException("App information is NULL");
    }

    loadedPresenterKey = PersistentCache.get()
        .load(KEY_PRESENTER, savedInstanceState, new PersistLoader.Callback<LockInfoPresenter>() {
          @NonNull @Override public PersistLoader<LockInfoPresenter> createLoader() {
            firstRefresh = true;
            return new LockInfoPresenterLoader();
          }

          @Override public void onPersistentLoaded(@NonNull LockInfoPresenter persist) {
            presenter = persist;
          }
        });

    loadedAdapterKey = PersistentCache.get()
        .load(KEY_LOAD_ADAPTER, savedInstanceState, new PersistLoader.Callback<LockInfoAdapter>() {
          @NonNull @Override public PersistLoader<LockInfoAdapter> createLoader() {
            return new ListAdapterLoader<LockInfoAdapter>() {
              @NonNull @Override public LockInfoAdapter loadPersistent() {
                return new LockInfoAdapter();
              }
            };
          }

          @Override public void onPersistentLoaded(@NonNull LockInfoAdapter persist) {
            fastItemAdapter = persist;
          }
        });
  }

  @Nullable @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    binding =
        DataBindingUtil.inflate(LayoutInflater.from(getActivity()), R.layout.fragment_lockinfo,
            null, false);
    return binding.getRoot();
  }

  @Override public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    setActionBarTitle(appName);

    binding.lockInfoPackageName.setText(appPackageName);
    binding.lockInfoSystem.setText((appIsSystem ? "YES" : "NO"));

    // Recycler setup
    final LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
    final RecyclerView.ItemDecoration dividerDecoration =
        new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL);

    binding.lockInfoRecycler.setLayoutManager(layoutManager);
    binding.lockInfoRecycler.addItemDecoration(dividerDecoration);
  }

  @Override public void onDestroyView() {
    super.onDestroyView();
    dismissOnboarding();
    setActionBarTitle(R.string.app_name);
    setActionBarUpEnabled(false);

    clearListListeners();
    binding.lockInfoRecycler.setOnClickListener(null);
    binding.lockInfoRecycler.setLayoutManager(null);
    binding.lockInfoRecycler.setAdapter(null);
    binding.unbind();
  }

  private void dismissOnboarding() {
    dismissOnboarding(toggleAllTapTarget);
    toggleAllTapTarget = null;

    dismissOnboarding(defaultLockTapTarget);
    defaultLockTapTarget = null;

    dismissOnboarding(whiteLockTapTarget);
    whiteLockTapTarget = null;

    dismissOnboarding(blackLockTapTarget);
    blackLockTapTarget = null;
  }

  private void dismissOnboarding(@Nullable TapTargetView tapTarget) {
    if (tapTarget == null) {
      Timber.d("NULL Target");
      return;
    }

    if (tapTarget.isVisible()) {
      tapTarget.dismiss(false);
    }
  }

  private void clearListListeners() {
    final int oldSize = fastItemAdapter.getAdapterItems().size() - 1;
    if (oldSize <= 0) {
      Timber.w("List is already empty");
      return;
    }

    for (int i = oldSize; i >= 0; --i) {
      final LockInfoItem item = fastItemAdapter.getAdapterItem(i);
      if (item != null) {
        item.cleanup();
      }
    }
  }

  @Override public void onDestroy() {
    super.onDestroy();
    if (!getActivity().isChangingConfigurations()) {
      PersistentCache.get().unload(loadedPresenterKey);
      PersistentCache.get().unload(loadedAdapterKey);
    }
  }

  @Override public void onStart() {
    super.onStart();
    presenter.bindView(this);

    binding.lockInfoRecycler.setAdapter(fastItemAdapter);
    presenter.loadApplicationIcon(appPackageName);
    presenter.setToggleAllState(appPackageName);
    if (firstRefresh) {
      refreshList();
    } else {
      Timber.d("We are already refreshed, just refresh the request listeners");
      applyUpdatedRequestListeners();
      presenter.showOnBoarding();
    }
  }

  private void applyUpdatedRequestListeners() {
    for (final LockInfoItem item : fastItemAdapter.getAdapterItems()) {
      item.setListener((position, name, currentState, newState) -> {
        Timber.d("Process lock state selection: [%d] %s from %s to %s", position, name,
            currentState, newState);
        processDatabaseModifyEvent(position, name, currentState, newState);
      });
    }
  }

  @Override public void onStop() {
    super.onStop();
    presenter.unbindView();
  }

  @Override public void onResume() {
    super.onResume();
    MainActivity.getNavigationDrawerController(getActivity()).drawerShowUpNavigation();
    setActionBarUpEnabled(true);
  }

  @Override public void onSaveInstanceState(Bundle outState) {
    PersistentCache.get().saveKey(outState, KEY_PRESENTER, loadedPresenterKey);
    PersistentCache.get().saveKey(outState, KEY_LOAD_ADAPTER, loadedAdapterKey);
    super.onSaveInstanceState(outState);
  }

  void clearList() {
    final int oldSize = fastItemAdapter.getAdapterItems().size() - 1;
    if (oldSize <= 0) {
      Timber.w("List is already empty");
      return;
    }

    for (int i = oldSize; i >= 0; --i) {
      final LockInfoItem item = fastItemAdapter.getItem(i);
      if (item != null) {
        item.cleanup();
      }
      fastItemAdapter.remove(i);
    }
  }

  @Override public void refreshList() {
    clearList();
    onListCleared();
    repopulateList();
  }

  private void repopulateList() {
    Timber.d("Repopulate list");
    binding.lockInfoRecycler.setClickable(false);
    presenter.populateList(appPackageName);
  }

  @Override public void onEntryAddedToList(@NonNull ActivityEntry entry) {
    Timber.d("Add entry: %s", entry);
    fastItemAdapter.add(
        new LockInfoItem(appPackageName, entry, (position, name, currentState, newState) -> {
          Timber.d("Process lock state selection: [%d] %s from %s to %s", position, name,
              currentState, newState);
          processDatabaseModifyEvent(position, name, currentState, newState);
        }));
    fastItemAdapter.notifyAdapterItemInserted(fastItemAdapter.getItemCount() - 1);
  }

  @Override public void onListPopulated() {
    Timber.d("Refresh finished");
    fastItemAdapter.notifyAdapterDataSetChanged();
    binding.lockInfoRecycler.setClickable(true);
    presenter.showOnBoarding();
  }

  @Override public void onListPopulateError() {
    Timber.e("onListPopulateError");
    onListPopulated();
    AppUtil.guaranteeSingleDialogFragment(getFragmentManager(), new ErrorDialog(), "error");
  }

  @Override public void onListCleared() {
    Timber.d("onListCleared");
  }

  @Override public void onApplicationIconLoadedError() {
    // TODO handle
  }

  @Override public void onApplicationIconLoadedSuccess(@NonNull Drawable drawable) {
    binding.lockInfoIcon.setImageDrawable(drawable);
  }

  @Override public void onDatabaseEntryCreated(int position) {
    if (position == LockInfoPresenter.GROUP_POSITION) {
      refreshList();
    } else {
      fastItemAdapter.onDatabaseEntryCreated(position);
    }
  }

  @Override public void onDatabaseEntryDeleted(int position) {
    if (position == LockInfoPresenter.GROUP_POSITION) {
      refreshList();
    } else {
      fastItemAdapter.onDatabaseEntryDeleted(position);
    }
  }

  @Override public void onDatabaseEntryWhitelisted(int position) {
    if (position == LockInfoPresenter.GROUP_POSITION) {
      refreshList();
    } else {
      fastItemAdapter.onDatabaseEntryWhitelisted(position);
    }
  }

  @Override public void onDatabaseEntryError(int position) {
    AppUtil.guaranteeSingleDialogFragment(getFragmentManager(), new ErrorDialog(), "error");
  }

  private void safeChangeToggleAllState(boolean enabled) {
    binding.lockInfoToggleall.setOnCheckedChangeListener(null);
    binding.lockInfoToggleall.setChecked(enabled);
    binding.lockInfoToggleall.setOnCheckedChangeListener((compoundButton, isChecked) -> {
      binding.lockInfoRecycler.setClickable(false);

      // Clear All
      clearList();

      presenter.modifyDatabaseGroup(isChecked, appPackageName, null, appIsSystem);
    });
  }

  @Override public void enableToggleAll() {
    safeChangeToggleAllState(true);
  }

  @Override public void disableToggleAll() {
    safeChangeToggleAllState(false);
  }

  @Override public void showOnBoarding() {
    Timber.d("Show onboarding");
    if (toggleAllTapTarget == null) {
      createToggleTarget();
    }
  }

  private void createToggleTarget() {
    final LockInfoItem.ViewHolder holder =
        (LockInfoItem.ViewHolder) binding.lockInfoRecycler.findViewHolderForAdapterPosition(0);
    final TapTarget toggleTarget =
        TapTarget.forView(binding.lockInfoToggleall, getString(R.string.onboard_title_info_toggle),
            getString(R.string.onboard_desc_info_toggle)).cancelable(false).tintTarget(false);
    toggleAllTapTarget =
        TapTargetView.showFor(getActivity(), toggleTarget, new TapTargetView.Listener() {

          @Override public void onTargetClick(TapTargetView view) {
            super.onTargetClick(view);
            Timber.d("Toggle all target clicked");
            if (holder != null) {
              final View radioDefault = holder.binding.lockInfoRadioDefault;
              createDefaultLockTarget(holder, radioDefault);
            } else {
              Timber.w("Cannot find Default lock target");
              endOnboarding();
            }
          }
        });
  }

  void createDefaultLockTarget(@NonNull LockInfoItem.ViewHolder holder,
      @NonNull View radioDefault) {
    final TapTarget lockDefaultTarget =
        TapTarget.forView(radioDefault, getString(R.string.onboard_title_info_lock_default),
            getString(R.string.onboard_desc_info_lock_default)).tintTarget(false).cancelable(false);
    defaultLockTapTarget =
        TapTargetView.showFor(getActivity(), lockDefaultTarget, new TapTargetView.Listener() {
          @Override public void onTargetClick(TapTargetView view) {
            super.onTargetClick(view);

            Timber.d("Default lock target clicked");
            final View radioWhite = holder.binding.lockInfoRadioWhite;
            createWhiteLockTarget(holder, radioWhite);
          }
        });
  }

  void createWhiteLockTarget(@NonNull LockInfoItem.ViewHolder holder, @NonNull View radioWhite) {
    final TapTarget lockWhiteTarget =
        TapTarget.forView(radioWhite, getString(R.string.onboard_title_info_lock_white),
            getString(R.string.onboard_desc_info_lock_white)).tintTarget(false).cancelable(false);
    whiteLockTapTarget =
        TapTargetView.showFor(getActivity(), lockWhiteTarget, new TapTargetView.Listener() {

          @Override public void onTargetClick(TapTargetView view) {
            super.onTargetClick(view);
            Timber.d("White lock target clicked");
            final View radioBlack = holder.binding.lockInfoRadioBlack;
            createBlackLockTarget(holder, radioBlack);
          }
        });
  }

  void createBlackLockTarget(@NonNull LockInfoItem.ViewHolder holder, @NonNull View radioBlack) {
    final TapTarget lockBlackTarget =
        TapTarget.forView(radioBlack, getString(R.string.onboard_title_info_lock_black),
            getString(R.string.onboard_desc_info_lock_black)).tintTarget(false).cancelable(false);
    blackLockTapTarget =
        TapTargetView.showFor(getActivity(), lockBlackTarget, new TapTargetView.Listener() {
          @Override public void onTargetClick(TapTargetView view) {
            super.onTargetClick(view);
            Timber.d("Black lock target clicked");
            endOnboarding();
          }
        });
  }

  void endOnboarding() {
    if (presenter != null) {
      Timber.d("End onboarding");
      presenter.setOnBoard();
    }
  }

  void processDatabaseModifyEvent(int position, @NonNull String activityName,
      @NonNull LockState previousLockState, @NonNull LockState newLockState) {
    Timber.d("Received a database modify event request for %s %s at %d [%s]", appPackageName,
        activityName, position, newLockState.name());
    final boolean whitelist = newLockState.equals(LockState.WHITELISTED);
    final boolean forceLock = newLockState.equals(LockState.LOCKED);
    final boolean wasDefault = previousLockState.equals(LockState.DEFAULT);
    presenter.modifyDatabaseEntry(wasDefault, position, appPackageName, activityName, null,
        appIsSystem, whitelist, forceLock);
  }
}
