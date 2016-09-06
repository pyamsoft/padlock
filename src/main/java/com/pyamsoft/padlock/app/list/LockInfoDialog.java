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
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import com.pyamsoft.padlock.R;
import com.pyamsoft.padlock.model.ActivityEntry;
import com.pyamsoft.padlock.model.AppEntry;
import com.pyamsoft.padlock.model.LockState;
import com.pyamsoft.pydroid.app.widget.DividerItemDecoration;
import com.pyamsoft.pydroid.base.ListAdapterLoader;
import com.pyamsoft.pydroid.base.PersistLoader;
import com.pyamsoft.pydroid.tool.AsyncDrawable;
import com.pyamsoft.pydroid.tool.AsyncDrawableMap;
import com.pyamsoft.pydroid.util.AppUtil;
import com.pyamsoft.pydroid.util.PersistentCache;
import rx.Subscription;
import timber.log.Timber;

public class LockInfoDialog extends DialogFragment implements LockInfoPresenter.LockInfoView {

  @NonNull private static final String ARG_APP_ENTRY = "app_entry";
  @NonNull private static final String KEY_LOAD_ADAPTER = "key_load_adapter";
  @NonNull private static final String KEY_PRESENTER = "key_presenter";
  @NonNull private final AsyncDrawableMap taskMap = new AsyncDrawableMap();
  @BindView(R.id.lock_info_fauxbar) LinearLayout toolbar;
  @BindView(R.id.lock_info_close) ImageView close;
  @BindView(R.id.lock_info_title) TextView name;
  @BindView(R.id.lock_info_icon) ImageView icon;
  @BindView(R.id.lock_info_package_name) TextView packageName;
  @BindView(R.id.lock_info_system) TextView system;
  @BindView(R.id.lock_info_recycler) RecyclerView recyclerView;
  @BindView(R.id.lock_info_toggleall) SwitchCompat toggleAll;

  @SuppressWarnings("WeakerAccess") LockInfoPresenter presenter;
  @SuppressWarnings("WeakerAccess") LockInfoAdapter fastItemAdapter;
  @SuppressWarnings("WeakerAccess") boolean firstRefresh;
  @SuppressWarnings("WeakerAccess") AppEntry appEntry;
  private Unbinder unbinder;
  private long loadedPresenterKey;
  private long loadedAdapterKey;

  public static LockInfoDialog newInstance(final @NonNull AppEntry appEntry) {
    final LockInfoDialog fragment = new LockInfoDialog();
    final Bundle args = new Bundle();

    args.putParcelable(ARG_APP_ENTRY, appEntry);

    fragment.setArguments(args);
    return fragment;
  }

  @Override public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    appEntry = getArguments().getParcelable(ARG_APP_ENTRY);

    loadedPresenterKey =
        PersistentCache.load(KEY_PRESENTER, savedInstanceState, new PersistLoader.Callback<LockInfoPresenter>() {
          @NonNull @Override public PersistLoader<LockInfoPresenter> createLoader() {
            firstRefresh = true;
            return new LockInfoPresenterLoader(getContext());
          }

          @Override public void onPersistentLoaded(@NonNull LockInfoPresenter persist) {
            presenter = persist;
          }
        });

    loadedAdapterKey = PersistentCache.load(KEY_LOAD_ADAPTER, savedInstanceState,
        new PersistLoader.Callback<LockInfoAdapter>() {
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
    final View rootView =
        LayoutInflater.from(getActivity()).inflate(R.layout.dialog_lockinfo, null, false);
    unbinder = ButterKnife.bind(this, rootView);
    initializeForEntry();
    return new AlertDialog.Builder(getActivity()).setView(rootView).create();
  }

  @Override public void onDestroyView() {
    super.onDestroyView();

    // KLUDGE If dialog is killed with back button, it may not have finished adding all of the individual
    // KLUDGE entries from the Toggle All

    recyclerView.setOnClickListener(null);
    recyclerView.setLayoutManager(null);
    recyclerView.setAdapter(null);

    taskMap.clear();
    unbinder.unbind();
  }

  @Override public void onDestroy() {
    super.onDestroy();
    if (!getActivity().isChangingConfigurations()) {
      PersistentCache.unload(loadedPresenterKey);
      PersistentCache.unload(loadedAdapterKey);
    }
  }

  @Override public void onStart() {
    super.onStart();
    presenter.bindView(this);

    recyclerView.setAdapter(fastItemAdapter);
    presenter.loadApplicationIcon(appEntry.packageName());
    presenter.setToggleAllState(appEntry.packageName());
    if (firstRefresh) {
      refreshList();
    }
  }

  @Override public void onStop() {
    super.onStop();
    presenter.unbindView();
  }

  @Override public void onSaveInstanceState(Bundle outState) {
    PersistentCache.saveKey(outState, KEY_PRESENTER, loadedPresenterKey);
    PersistentCache.saveKey(outState, KEY_LOAD_ADAPTER, loadedAdapterKey);
    super.onSaveInstanceState(outState);
  }

  private void initializeForEntry() {
    ViewCompat.setElevation(toolbar, AppUtil.convertToDP(getContext(), 4));
    close.setOnClickListener(view -> {
      // Only close if list is displayed
      if (recyclerView.isClickable()) {
        dismiss();
      }
    });

    final Subscription task =
        AsyncDrawable.with(getContext()).load(R.drawable.ic_close_24dp).into(close);
    taskMap.put("close", task);

    name.setText(appEntry.name());
    packageName.setText(appEntry.packageName());
    system.setText((appEntry.system() ? "YES" : "NO"));

    // Recycler setup
    final LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
    final RecyclerView.ItemDecoration dividerDecoration =
        new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL_LIST);

    recyclerView.setLayoutManager(layoutManager);
    recyclerView.addItemDecoration(dividerDecoration);
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
    recyclerView.setClickable(false);
    presenter.populateList(appEntry.packageName());
  }

  @Override public void onEntryAddedToList(@NonNull ActivityEntry entry) {
    Timber.d("Add entry: %s", entry);
    fastItemAdapter.add(new LockInfoItem(appEntry.packageName(), entry));
  }

  @Override public void onListPopulated() {
    Timber.d("Refresh finished");
    recyclerView.setClickable(true);
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
    icon.setImageDrawable(drawable);
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
    toggleAll.setOnCheckedChangeListener(null);
    toggleAll.setChecked(enabled);
    toggleAll.setOnCheckedChangeListener((compoundButton, isChecked) -> {
      recyclerView.setClickable(false);

      // Clear All
      final int oldSize = fastItemAdapter.getItemCount() - 1;
      for (int i = oldSize; i >= 0; --i) {
        fastItemAdapter.remove(i);
      }

      presenter.modifyDatabaseGroup(isChecked, appEntry.packageName(), null, appEntry.system());
    });
  }

  @Override public void enableToggleAll() {
    safeChangeToggleAllState(true);
  }

  @Override public void disableToggleAll() {
    safeChangeToggleAllState(false);
  }

  @Override public void processDatabaseModifyEvent(int position, @NonNull String activityName,
      @NonNull LockState lockState) {
    Timber.d("Received a database modify event request for %s %s at %d", appEntry.packageName(),
        activityName, position);
    final boolean whitelist = lockState.equals(LockState.WHITELISTED);
    final boolean forceLock = lockState.equals(LockState.LOCKED);
    presenter.modifyDatabaseEntry(position, appEntry.packageName(), activityName, null,
        appEntry.system(), whitelist, forceLock);
  }
}
