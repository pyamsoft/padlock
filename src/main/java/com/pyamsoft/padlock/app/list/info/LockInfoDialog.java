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

package com.pyamsoft.padlock.app.list.info;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import com.pyamsoft.padlock.PadLock;
import com.pyamsoft.padlock.R;
import com.pyamsoft.padlock.app.db.DBPresenter;
import com.pyamsoft.padlock.app.list.AdapterPresenter;
import com.pyamsoft.padlock.app.list.LockListLayoutManager;
import com.pyamsoft.padlock.dagger.list.info.DaggerLockInfoComponent;
import com.pyamsoft.padlock.dagger.list.info.LockInfoModule;
import com.pyamsoft.padlock.model.ActivityEntry;
import com.pyamsoft.padlock.model.AppEntry;
import com.pyamsoft.pydroid.base.Presenter;
import com.pyamsoft.pydroid.base.RetainedDialogFragment;
import com.pyamsoft.pydroid.tool.DataHolderFragment;
import com.pyamsoft.pydroid.tool.DividerItemDecoration;
import javax.inject.Inject;
import timber.log.Timber;

public class LockInfoDialog extends RetainedDialogFragment
    implements LockInfoPresenter.LockInfoView {

  @NonNull private static final String ARG_APP_ENTRY = "app_entry";
  private static final int KEY_PRESENTER = 0;
  private static final int KEY_ADAPTER_PRESENTER = 1;
  private static final int KEY_DB_PRESENTER = 2;
  @BindView(R.id.lock_info_close) ImageView close;
  @BindView(R.id.lock_info_title) TextView name;
  @BindView(R.id.lock_info_icon) ImageView icon;
  @BindView(R.id.lock_info_package_name) TextView packageName;
  @BindView(R.id.lock_info_system) TextView system;
  @BindView(R.id.lock_info_swiperefresh) SwipeRefreshLayout swipeRefreshLayout;
  @BindView(R.id.lock_info_recycler) RecyclerView recyclerView;
  @Inject LockInfoPresenter presenter;
  @Inject DBPresenter dbPresenter;
  @Inject AdapterPresenter<ActivityEntry> adapterPresenter;
  private DataHolderFragment<Presenter> presenterDataHolder;
  private LockInfoAdapter adapter;
  private AppEntry appEntry;
  private boolean firstRefresh;
  private LockListLayoutManager layoutManager;
  @NonNull private final Runnable stopRefreshRunnable = new Runnable() {
    @Override public void run() {
      swipeRefreshLayout.setRefreshing(false);
      layoutManager.setVerticalScrollEnabled(true);
    }
  };
  @NonNull private final Runnable startRefreshRunnable = new Runnable() {
    @Override public void run() {
      swipeRefreshLayout.setRefreshing(true);
      layoutManager.setVerticalScrollEnabled(false);
    }
  };
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
    Timber.d("onCreate");

    appEntry = getArguments().getParcelable(ARG_APP_ENTRY);
    presenterDataHolder =
        DataHolderFragment.getInstance(getFragmentManager(), "lock_info_presenters");

    final LockInfoPresenter lockInfoPresenter =
        (LockInfoPresenter) presenterDataHolder.pop(KEY_PRESENTER);
    @SuppressWarnings("unchecked") final AdapterPresenter<ActivityEntry>
        activityEntryAdapterPresenter =
        (AdapterPresenter<ActivityEntry>) presenterDataHolder.pop(KEY_ADAPTER_PRESENTER);
    final DBPresenter lockDBPresenter = (DBPresenter) presenterDataHolder.pop(KEY_DB_PRESENTER);
    if (lockInfoPresenter == null
        || activityEntryAdapterPresenter == null
        || lockDBPresenter == null) {
      Timber.d("Create new presenters");
      firstRefresh = true;
      DaggerLockInfoComponent.builder()
          .padLockComponent(PadLock.padLockComponent(this))
          .lockInfoModule(new LockInfoModule())
          .build()
          .inject(this);
    } else {
      Timber.d("Load cached presenters");
      firstRefresh = false;
      presenter = lockInfoPresenter;
      dbPresenter = lockDBPresenter;
      adapterPresenter = activityEntryAdapterPresenter;
    }

    adapter = new LockInfoAdapter(appEntry, adapterPresenter, dbPresenter);
  }

  @Nullable @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    adapter.onCreate();
    presenter.onCreateView(this);
    return super.onCreateView(inflater, container, savedInstanceState);
  }

  @SuppressLint("InflateParams") @NonNull @Override
  public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
    final View rootView =
        LayoutInflater.from(getActivity()).inflate(R.layout.dialog_lockinfo, null, false);
    unbinder = ButterKnife.bind(this, rootView);

    initializeForEntry();

    if (firstRefresh) {
      firstRefresh = false;
      refreshList();
    }

    return new AlertDialog.Builder(getActivity()).setView(rootView).create();
  }

  @Override public void onDestroyView() {
    super.onDestroyView();

    recyclerView.setOnClickListener(null);
    recyclerView.setLayoutManager(null);
    recyclerView.setAdapter(null);

    adapter.onDestroy();

    if (!getActivity().isChangingConfigurations()) {
      presenter.onDestroyView();
    }

    swipeRefreshLayout.setOnRefreshListener(null);
    swipeRefreshLayout.removeCallbacks(startRefreshRunnable);
    swipeRefreshLayout.removeCallbacks(stopRefreshRunnable);

    if (unbinder != null) {
      unbinder.unbind();
    }
  }

  private void initializeForEntry() {
    close.setOnClickListener(view -> dismiss());
    name.setText(appEntry.name());
    icon.setImageBitmap(appEntry.icon());
    packageName.setText(appEntry.packageName());
    system.setText((appEntry.system() ? "YES" : "NO"));

    // Recycler setup
    layoutManager = new LockListLayoutManager(getActivity());
    final RecyclerView.ItemDecoration dividerDecoration =
        new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL_LIST);
    recyclerView.setLayoutManager(layoutManager);
    recyclerView.addItemDecoration(dividerDecoration);
    recyclerView.setAdapter(adapter);

    swipeRefreshLayout.setColorSchemeResources(R.color.blue500, R.color.amber700, R.color.blue700,
        R.color.amber500);
    swipeRefreshLayout.setOnRefreshListener(() -> {
      Timber.d("onRefresh");
      refreshList();
    });
  }

  @Override public void refreshList() {
    final int oldSize = adapter.getItemCount() - 1;
    for (int i = oldSize; i >= 0; --i) {
      adapter.removeItem();
    }
    onListCleared();
    repopulateList();
  }

  private void repopulateList() {
    Timber.d("Repopulate list");
    presenter.populateList(appEntry.packageName(), appEntry.activities());
  }

  @Override public void onEntryAddedToList(@NonNull ActivityEntry entry) {
    Timber.d("Add entry: %s", entry);

    // In case the configuration changes, we do the animation again
    if (!swipeRefreshLayout.isRefreshing()) {
      swipeRefreshLayout.post(() -> swipeRefreshLayout.setRefreshing(true));
    }

    adapter.addItem(entry);
  }

  @Override public void onListPopulated() {
    Timber.d("Refresh finished");
    swipeRefreshLayout.post(stopRefreshRunnable);
  }

  @Override public void onListPopulateError() {
    Timber.e("onListPopulateError");
    onListPopulated();
  }

  @Override public void onListCleared() {
    Timber.d("onListCleared");
    swipeRefreshLayout.post(startRefreshRunnable);
  }

  @Override public void onSaveInstanceState(@NonNull Bundle outState) {
    super.onSaveInstanceState(outState);
    if (getActivity().isChangingConfigurations()) {
      presenterDataHolder.put(KEY_PRESENTER, presenter);
      presenterDataHolder.put(KEY_ADAPTER_PRESENTER, adapterPresenter);
      presenterDataHolder.put(KEY_DB_PRESENTER, dbPresenter);
    } else {
      presenterDataHolder.clear();
    }
  }
}
