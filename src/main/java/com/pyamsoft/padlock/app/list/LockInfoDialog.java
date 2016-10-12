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

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.databinding.DataBindingUtil;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import com.pyamsoft.padlock.R;
import com.pyamsoft.padlock.databinding.DialogLockinfoBinding;
import com.pyamsoft.padlock.model.ActivityEntry;
import com.pyamsoft.padlock.model.AppEntry;
import com.pyamsoft.padlock.model.LockState;
import com.pyamsoft.pydroid.app.ListAdapterLoader;
import com.pyamsoft.pydroid.app.PersistLoader;
import com.pyamsoft.pydroid.tool.AsyncDrawable;
import com.pyamsoft.pydroid.tool.AsyncMap;
import com.pyamsoft.pydroid.util.AppUtil;
import com.pyamsoft.pydroid.util.PersistentCache;
import com.pyamsoft.pydroid.widget.DividerItemDecoration;
import com.pyamsoft.pydroidrx.RXLoader;
import timber.log.Timber;

public class LockInfoDialog extends DialogFragment implements LockInfoPresenter.LockInfoView {

  @NonNull private static final String ARG_APP_PACKAGE_NAME = "app_packagename";
  @NonNull private static final String ARG_APP_NAME = "app_name";
  @NonNull private static final String ARG_APP_SYSTEM = "app_system";
  @NonNull private static final String KEY_LOAD_ADAPTER = "key_load_adapter";
  @NonNull private static final String KEY_PRESENTER = "key_presenter";
  @NonNull private final AsyncDrawable.Mapper taskMap = new AsyncDrawable.Mapper();

  @SuppressWarnings("WeakerAccess") LockInfoPresenter presenter;
  @SuppressWarnings("WeakerAccess") LockInfoAdapter fastItemAdapter;
  @SuppressWarnings("WeakerAccess") boolean firstRefresh;
  private DialogLockinfoBinding binding;
  private long loadedPresenterKey;
  private long loadedAdapterKey;

  private String appPackageName;
  private String appName;
  private boolean appIsSystem;

  public static LockInfoDialog newInstance(final @NonNull AppEntry appEntry) {
    final LockInfoDialog fragment = new LockInfoDialog();
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
            return new LockInfoPresenterLoader(getContext());
          }

          @Override public void onPersistentLoaded(@NonNull LockInfoPresenter persist) {
            presenter = persist;
          }
        });

    loadedAdapterKey = PersistentCache.get()
        .load(KEY_LOAD_ADAPTER, savedInstanceState, new PersistLoader.Callback<LockInfoAdapter>() {
          @NonNull @Override public PersistLoader<LockInfoAdapter> createLoader() {
            return new ListAdapterLoader<LockInfoAdapter>(getContext()) {
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

  @SuppressLint("InflateParams") @NonNull @Override
  public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
    binding =
        DataBindingUtil.inflate(LayoutInflater.from(getActivity()), R.layout.dialog_lockinfo, null,
            false);
    initializeForEntry();
    return new AlertDialog.Builder(getActivity()).setView(binding.getRoot()).create();
  }

  @Override public void onDestroyView() {
    super.onDestroyView();

    // KLUDGE If dialog is killed with back button, it may not have finished adding all of the individual
    // KLUDGE entries from the Toggle All

    binding.lockInfoRecycler.setOnClickListener(null);
    binding.lockInfoRecycler.setLayoutManager(null);
    binding.lockInfoRecycler.setAdapter(null);

    taskMap.clear();
    binding.unbind();
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
    }
  }

  @Override public void onStop() {
    super.onStop();
    presenter.unbindView();
  }

  @Override public void onSaveInstanceState(Bundle outState) {
    PersistentCache.get().saveKey(outState, KEY_PRESENTER, loadedPresenterKey);
    PersistentCache.get().saveKey(outState, KEY_LOAD_ADAPTER, loadedAdapterKey);
    super.onSaveInstanceState(outState);
  }

  private void initializeForEntry() {
    ViewCompat.setElevation(binding.lockInfoFauxbar, AppUtil.convertToDP(getContext(), 4));
    binding.lockInfoClose.setOnClickListener(view -> {
      // Only close if list is displayed
      if (binding.lockInfoRecycler.isClickable()) {
        dismiss();
      }
    });

    final AsyncMap.Entry task = AsyncDrawable.with(getContext())
        .load(R.drawable.ic_close_24dp, new RXLoader())
        .into(binding.lockInfoClose);
    taskMap.put("close", task);

    binding.lockInfoTitle.setText(appName);
    binding.lockInfoPackageName.setText(appPackageName);
    binding.lockInfoSystem.setText((appIsSystem ? "YES" : "NO"));

    // Recycler setup
    final LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
    final RecyclerView.ItemDecoration dividerDecoration =
        new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL_LIST);

    binding.lockInfoRecycler.setLayoutManager(layoutManager);
    binding.lockInfoRecycler.addItemDecoration(dividerDecoration);
  }

  @Override public void refreshList() {
    final int oldSize = fastItemAdapter.getItemCount() - 1;
    for (int i = oldSize; i >= 0; --i) {
      fastItemAdapter.remove(i);
    }

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
  }

  @Override public void onListPopulated() {
    Timber.d("Refresh finished");
    binding.lockInfoRecycler.setClickable(true);
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
      final int oldSize = fastItemAdapter.getItemCount() - 1;
      for (int i = oldSize; i >= 0; --i) {
        fastItemAdapter.remove(i);
      }

      presenter.modifyDatabaseGroup(isChecked, appPackageName, null, appIsSystem);
    });
  }

  @Override public void enableToggleAll() {
    safeChangeToggleAllState(true);
  }

  @Override public void disableToggleAll() {
    safeChangeToggleAllState(false);
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
