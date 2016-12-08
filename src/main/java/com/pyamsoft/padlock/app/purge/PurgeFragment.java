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

package com.pyamsoft.padlock.app.purge;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import com.mikepenz.fastadapter.commons.adapters.FastItemAdapter;
import com.pyamsoft.padlock.R;
import com.pyamsoft.padlock.app.main.MainActivity;
import com.pyamsoft.padlock.databinding.FragmentPurgeBinding;
import com.pyamsoft.pydroid.app.PersistLoader;
import com.pyamsoft.pydroid.app.fragment.ActionBarFragment;
import com.pyamsoft.pydroid.util.AppUtil;
import com.pyamsoft.pydroid.util.PersistentCache;
import timber.log.Timber;

public class PurgeFragment extends ActionBarFragment implements PurgePresenter.View {

  @NonNull public static final String TAG = "PurgeFragment";
  @NonNull private static final String FORCE_REFRESH = "key_force_refresh";
  @NonNull private static final String KEY_PRESENTER = "key_purge_presenter";
  PurgePresenter presenter;
  FastItemAdapter<PurgeItem> fastItemAdapter;
  boolean forceRefresh;
  private FragmentPurgeBinding binding;
  private long loadedKey;
  private DividerItemDecoration decoration;

  @Override public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setHasOptionsMenu(true);

    if (savedInstanceState != null) {
      Timber.i("Restore forceRefresh state from savedInstanceState");
      forceRefresh = savedInstanceState.getBoolean(FORCE_REFRESH, true);
    }

    loadedKey = PersistentCache.get()
        .load(KEY_PRESENTER, savedInstanceState, new PersistLoader.Callback<PurgePresenter>() {
          @NonNull @Override public PersistLoader<PurgePresenter> createLoader() {
            forceRefresh = true;
            return new PurgePresenterLoader();
          }

          @Override public void onPersistentLoaded(@NonNull PurgePresenter persist) {
            presenter = persist;
          }
        });

    fastItemAdapter = new FastItemAdapter<>();
  }

  @Override public void onDestroy() {
    super.onDestroy();
    if (!getActivity().isChangingConfigurations()) {
      PersistentCache.get().unload(loadedKey);
    }
  }

  @Nullable @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    binding = FragmentPurgeBinding.inflate(inflater, container, false);
    return binding.getRoot();
  }

  @Override public void onDestroyView() {
    super.onDestroyView();
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
  }

  @Override public void onStart() {
    super.onStart();
    presenter.bindView(this);
    if (forceRefresh) {
      Timber.d("Do initial refresh");
      refreshList();
    } else {
      Timber.d("We are already refreshed");
      presenter.retrieveStaleApplications();
    }
  }

  private void refreshList() {
    fastItemAdapter.clear();
    presenter.clearList();
    forceRefresh = true;

    presenter.retrieveStaleApplications();
  }

  @Override public void onStop() {
    super.onStop();
    presenter.unbindView();
  }

  @Override public void onResume() {
    super.onResume();
    setActionBarUpEnabled(true);
    MainActivity.getNavigationDrawerController(getActivity()).drawerNormalNavigation();
  }

  @Override public void onSaveInstanceState(Bundle outState) {
    PersistentCache.get().saveKey(outState, KEY_PRESENTER, loadedKey);
    outState.putBoolean(FORCE_REFRESH, forceRefresh);
    super.onSaveInstanceState(outState);
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
      handleDeleteRequest(position, item.getPackageName());
      return true;
    });
    decoration = new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL);
    binding.purgeList.setLayoutManager(new LinearLayoutManager(getContext()));
    binding.purgeList.addItemDecoration(decoration);
    binding.purgeList.setAdapter(fastItemAdapter);
  }

  void handleDeleteRequest(int position, @NonNull String packageName) {
    Timber.d("Handle delete request for %s at %d", packageName, position);
    AppUtil.guaranteeSingleDialogFragment(getFragmentManager(),
        PurgeSingleItemDialog.newInstance(packageName), "purge_single");
  }

  @Override public void onStaleApplicationRetrieved(@NonNull String name) {
    fastItemAdapter.add(new PurgeItem(name));
  }

  @Override public void onRetrievalComplete() {
    forceRefresh = false;

    // TODO show empty view if empty list
  }

  @Override public void onDeleted(@NonNull String packageName) {
    final int itemCount = fastItemAdapter.getItemCount();
    if (itemCount == 0) {
      Timber.e("Adapter is EMPTY");
    } else {
      int found = -1;
      for (int i = 0; i < itemCount; ++i) {
        final PurgeItem item = fastItemAdapter.getAdapterItem(i);
        if (item.getPackageName().equals(packageName)) {
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
        purge(item.getPackageName());
      }
    }
  }
}
