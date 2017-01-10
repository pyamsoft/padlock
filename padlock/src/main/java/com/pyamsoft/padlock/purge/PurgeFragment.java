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

package com.pyamsoft.padlock.purge;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import com.mikepenz.fastadapter.commons.adapters.FastItemAdapter;
import com.pyamsoft.padlock.PadLock;
import com.pyamsoft.padlock.R;
import com.pyamsoft.padlock.databinding.FragmentPurgeBinding;
import com.pyamsoft.padlock.main.MainActivity;
import com.pyamsoft.pydroid.cache.PersistentCache;
import com.pyamsoft.pydroid.util.AppUtil;
import timber.log.Timber;

public class PurgeFragment extends Fragment implements PurgePresenter.View {

  @NonNull public static final String TAG = "PurgeFragment";
  @NonNull private static final String KEY_PRESENTER = TAG + "key_purge_presenter";
  @NonNull private final Handler handler = new Handler(Looper.getMainLooper());
  @SuppressWarnings("WeakerAccess") PurgePresenter presenter;
  @SuppressWarnings("WeakerAccess") FastItemAdapter<PurgeItem> fastItemAdapter;
  private FragmentPurgeBinding binding;
  @NonNull private final Runnable startRefreshRunnable =
      () -> binding.purgeSwipeRefresh.post(() -> {
        if (binding != null) {
          if (binding.purgeSwipeRefresh != null) {
            binding.purgeSwipeRefresh.setRefreshing(true);
          }
        }
      });
  @NonNull private final Runnable stopRefreshRunnable = () -> binding.purgeSwipeRefresh.post(() -> {
    if (binding != null) {
      if (binding.purgeSwipeRefresh != null) {
        binding.purgeSwipeRefresh.setRefreshing(false);
      }
    }
  });
  private DividerItemDecoration decoration;
  private boolean listIsRefreshed;

  @Override public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setHasOptionsMenu(true);

    presenter = PersistentCache.load(getActivity(), KEY_PRESENTER, new PurgePresenterLoader());
  }

  @Override public void onDestroy() {
    super.onDestroy();
    PadLock.getRefWatcher(this).watch(this);
  }

  @Nullable @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    listIsRefreshed = false;
    fastItemAdapter = new FastItemAdapter<>();
    binding = FragmentPurgeBinding.inflate(inflater, container, false);
    return binding.getRoot();
  }

  @Override public void onDestroyView() {
    super.onDestroyView();
    handler.removeCallbacksAndMessages(null);
    fastItemAdapter.withOnClickListener(null);
    binding.purgeList.removeItemDecoration(decoration);
    binding.purgeList.setOnClickListener(null);
    binding.purgeList.setLayoutManager(null);
    binding.purgeList.setAdapter(null);
    binding.unbind();
  }

  @Override public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    setupRecyclerView();
    setupSwipeRefresh();
  }

  private void setupSwipeRefresh() {
    binding.purgeSwipeRefresh.setColorSchemeResources(R.color.blue500, R.color.amber700,
        R.color.blue700, R.color.amber500);
    binding.purgeSwipeRefresh.setOnRefreshListener(() -> {
      Timber.d("onRefresh");
      refreshList();
    });
  }

  @Override public void onStart() {
    super.onStart();
    presenter.bindView(this);
    if (!listIsRefreshed) {
      if (!binding.purgeSwipeRefresh.isRefreshing()) {
        binding.purgeSwipeRefresh.post(() -> {
          if (binding != null) {
            if (binding.purgeSwipeRefresh != null) {
              binding.purgeSwipeRefresh.setRefreshing(true);
            }
          }
        });
      }
      presenter.retrieveStaleApplications();
    }
  }

  private void refreshList() {
    fastItemAdapter.clear();
    presenter.clearList();

    handler.removeCallbacksAndMessages(null);
    handler.post(startRefreshRunnable);

    presenter.retrieveStaleApplications();
  }

  @Override public void onStop() {
    super.onStop();
    presenter.unbindView();
  }

  @Override public void onResume() {
    super.onResume();
    MainActivity.getNavigationDrawerController(getActivity()).drawerNormalNavigation();
  }

  @Override public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    super.onCreateOptionsMenu(menu, inflater);
    inflater.inflate(R.menu.purge_old_menu, menu);
  }

  @Override public boolean onOptionsItemSelected(MenuItem item) {
    final boolean handled;
    switch (item.getItemId()) {
      case R.id.menu_purge_all:
        AppUtil.guaranteeSingleDialogFragment(getFragmentManager(), new PurgeAllDialog(),
            "purge_all");
        handled = true;
        break;
      default:
        handled = false;
    }

    return handled || super.onOptionsItemSelected(item);
  }

  private void setupRecyclerView() {
    fastItemAdapter.withSelectable(true);
    fastItemAdapter.withOnClickListener((v, adapter, item, position) -> {
      handleDeleteRequest(position, item.getModel());
      return true;
    });
    decoration = new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL);
    binding.purgeList.setLayoutManager(new LinearLayoutManager(getContext()));
    binding.purgeList.addItemDecoration(decoration);
    binding.purgeList.setAdapter(fastItemAdapter);
  }

  @SuppressWarnings("WeakerAccess") void handleDeleteRequest(int position,
      @NonNull String packageName) {
    Timber.d("Handle delete request for %s at %d", packageName, position);
    AppUtil.guaranteeSingleDialogFragment(getFragmentManager(),
        PurgeSingleItemDialog.newInstance(packageName), "purge_single");
  }

  @Override public void onStaleApplicationRetrieved(@NonNull String name) {
    // In case the configuration changes, we do the animation again
    if (!binding.purgeSwipeRefresh.isRefreshing()) {
      binding.purgeSwipeRefresh.post(() -> {
        if (binding != null) {
          if (binding.purgeSwipeRefresh != null) {
            binding.purgeSwipeRefresh.setRefreshing(true);
          }
        }
      });
    }

    fastItemAdapter.add(new PurgeItem(name));
  }

  @Override public void onRetrievalComplete() {
    listIsRefreshed = true;

    // TODO show empty view if empty list
    handler.removeCallbacksAndMessages(null);
    handler.post(stopRefreshRunnable);
  }

  @Override public void onDeleted(@NonNull String packageName) {
    final int itemCount = fastItemAdapter.getItemCount();
    if (itemCount == 0) {
      Timber.e("Adapter is EMPTY");
    } else {
      int found = -1;
      for (int i = 0; i < itemCount; ++i) {
        final PurgeItem item = fastItemAdapter.getAdapterItem(i);
        if (item.getModel().equals(packageName)) {
          found = i;
          break;
        }
      }

      if (found != -1) {
        Timber.d("Remove deleted item: %s", packageName);
        fastItemAdapter.remove(found);
      }
    }
  }

  void purge(@NonNull String packageName) {
    Timber.d("Purge stale: %s", packageName);
    presenter.deleteStale(packageName);
  }

  void purgeAll() {
    final int itemCount = fastItemAdapter.getItemCount();
    if (itemCount == 0) {
      Timber.e("Adapter is EMPTY");
    } else {
      for (final PurgeItem item : fastItemAdapter.getAdapterItems()) {
        purge(item.getModel());
      }
    }
  }
}
