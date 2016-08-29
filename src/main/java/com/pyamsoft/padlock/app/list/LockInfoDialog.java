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
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
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
import com.pyamsoft.padlock.app.db.DBPresenter;
import com.pyamsoft.padlock.model.ActivityEntry;
import com.pyamsoft.padlock.model.AppEntry;
import com.pyamsoft.pydroid.base.app.ListAdapterLoader;
import com.pyamsoft.pydroid.tool.AsyncDrawable;
import com.pyamsoft.pydroid.tool.AsyncDrawableMap;
import com.pyamsoft.pydroid.tool.DividerItemDecoration;
import com.pyamsoft.pydroid.util.AppUtil;
import rx.Subscription;
import timber.log.Timber;

public class LockInfoDialog extends DialogFragment
    implements LockInfoPresenter.LockInfoView, DBPresenter.DBView {

  @NonNull private static final String ARG_APP_ENTRY = "app_entry";
  private static final int KEY_PRESENTER = 0;
  private static final int KEY_ADAPTER_PRESENTER = 1;
  private static final int KEY_DB_PRESENTER = 2;
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
  @SuppressWarnings("WeakerAccess") DBPresenter dbPresenter;
  @SuppressWarnings("WeakerAccess") LockInfoAdapter fastItemAdapter;
  @SuppressWarnings("WeakerAccess") boolean firstRefresh;
  @SuppressWarnings("WeakerAccess") AppEntry appEntry;
  private Unbinder unbinder;

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
  }

  @SuppressLint("InflateParams") @NonNull @Override
  public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
    getLoaderManager().initLoader(KEY_PRESENTER, null,
        new LoaderManager.LoaderCallbacks<LockInfoPresenter>() {
          @Override public Loader<LockInfoPresenter> onCreateLoader(int id, Bundle args) {
            firstRefresh = true;
            return new LockInfoPresenterLoader(getContext());
          }

          @Override
          public void onLoadFinished(Loader<LockInfoPresenter> loader, LockInfoPresenter data) {
            presenter = data;
          }

          @Override public void onLoaderReset(Loader<LockInfoPresenter> loader) {
            presenter = null;
          }
        });

    getLoaderManager().initLoader(KEY_ADAPTER_PRESENTER, null,
        new LoaderManager.LoaderCallbacks<LockInfoAdapter>() {
          @Override public Loader<LockInfoAdapter> onCreateLoader(int id, Bundle args) {
            return new ListAdapterLoader<LockInfoAdapter>(getContext()) {
              @NonNull @Override protected LockInfoAdapter loadAdapter() {
                return new LockInfoAdapter();
              }
            };
          }

          @Override
          public void onLoadFinished(Loader<LockInfoAdapter> loader, LockInfoAdapter data) {
            fastItemAdapter = data;
          }

          @Override public void onLoaderReset(Loader<LockInfoAdapter> loader) {
            fastItemAdapter = null;
          }
        });

    getLoaderManager().initLoader(KEY_DB_PRESENTER, null,
        new LoaderManager.LoaderCallbacks<DBPresenter>() {
          @Override public Loader<DBPresenter> onCreateLoader(int id, Bundle args) {
            firstRefresh = true;
            return new DBPresenterLoader(getContext());
          }

          @Override public void onLoadFinished(Loader<DBPresenter> loader, DBPresenter data) {
            dbPresenter = data;
          }

          @Override public void onLoaderReset(Loader<DBPresenter> loader) {
            dbPresenter = null;
          }
        });

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

  @Override public void onResume() {
    super.onResume();
    presenter.bindView(this);
    dbPresenter.bindView(this);

    recyclerView.setAdapter(fastItemAdapter);
    presenter.loadApplicationIcon(appEntry.packageName());
    presenter.setToggleAllState(appEntry.packageName());
    if (firstRefresh) {
      refreshList();
    }
  }

  @Override public void onPause() {
    super.onPause();
    presenter.unbindView();
    dbPresenter.unbindView();
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

  @Override public void onDBCreateEvent(int position) {
    if (position >= 0) {
      fastItemAdapter.onDBCreateEvent(position);
    } else {
      refreshList();
    }
  }

  @Override public void onDBDeleteEvent(int position) {
    if (position >= 0) {
      fastItemAdapter.onDBDeleteEvent(position);
    } else {
      refreshList();
    }
  }

  @Override public void onDBError() {
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

      dbPresenter.attemptDBAllModification(isChecked, appEntry.packageName(), null,
          appEntry.system());
    });
  }

  @Override public void enableToggleAll() {
    safeChangeToggleAllState(true);
  }

  @Override public void disableToggleAll() {
    safeChangeToggleAllState(false);
  }
}
