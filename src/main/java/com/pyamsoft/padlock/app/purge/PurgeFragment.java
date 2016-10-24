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

import android.databinding.DataBindingUtil;
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
import com.mikepenz.fastadapter.adapters.FastItemAdapter;
import com.pyamsoft.padlock.R;
import com.pyamsoft.padlock.databinding.FragmentPurgeBinding;
import com.pyamsoft.pydroid.app.ListAdapterLoader;
import com.pyamsoft.pydroid.app.PersistLoader;
import com.pyamsoft.pydroid.app.fragment.ActionBarFragment;
import com.pyamsoft.pydroid.util.PersistentCache;
import timber.log.Timber;

public class PurgeFragment extends ActionBarFragment implements PurgePresenter.View {

  @NonNull public static final String TAG = "PurgeFragment";
  @NonNull private static final String KEY_PRESENTER = "key_purge_presenter";
  @NonNull private static final String KEY_LOAD_ADAPTER = "key_purge_adapter";
  PurgePresenter presenter;
  FastItemAdapter<PurgeItem> fastItemAdapter;
  boolean forceRefresh;
  private FragmentPurgeBinding binding;
  private long loadedKey;
  private long loadedAdapterKey;
  private DividerItemDecoration decoration;

  @Override public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setHasOptionsMenu(true);

    loadedKey = PersistentCache.get()
        .load(KEY_PRESENTER, savedInstanceState, new PersistLoader.Callback<PurgePresenter>() {
          @NonNull @Override public PersistLoader<PurgePresenter> createLoader() {
            forceRefresh = true;
            return new PurgePresenterLoader(getContext());
          }

          @Override public void onPersistentLoaded(@NonNull PurgePresenter persist) {
            presenter = persist;
          }
        });

    loadedAdapterKey = PersistentCache.get()
        .load(KEY_LOAD_ADAPTER, savedInstanceState,
            new PersistLoader.Callback<FastItemAdapter<PurgeItem>>() {
              @NonNull @Override public PersistLoader<FastItemAdapter<PurgeItem>> createLoader() {
                return new ListAdapterLoader<FastItemAdapter<PurgeItem>>(getContext()) {
                  @NonNull @Override public FastItemAdapter<PurgeItem> loadPersistent() {
                    return new FastItemAdapter<>();
                  }
                };
              }

              @Override
              public void onPersistentLoaded(@NonNull FastItemAdapter<PurgeItem> persist) {
                fastItemAdapter = persist;
              }
            });
  }

  @Override public void onDestroy() {
    super.onDestroy();
    if (!getActivity().isChangingConfigurations()) {
      PersistentCache.get().unload(loadedKey);
      PersistentCache.get().unload(loadedAdapterKey);
    }
  }

  @Nullable @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    binding = DataBindingUtil.inflate(inflater, R.layout.fragment_purge, container, false);
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
    }
  }

  private void clearList() {
    final int oldSize = fastItemAdapter.getAdapterItems().size() - 1;
    if (oldSize <= 0) {
      Timber.w("List is already empty");
      return;
    }

    for (int i = oldSize; i >= 0; --i) {
      fastItemAdapter.remove(i);
    }
  }

  private void refreshList() {
    forceRefresh = false;
    clearList();
    presenter.retrieveStaleApplications();
  }

  @Override public void onStop() {
    super.onStop();
    presenter.unbindView();
  }

  @Override public void onResume() {
    super.onResume();
    setActionBarUpEnabled(true);
  }

  @Override public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    PersistentCache.get().saveKey(outState, KEY_PRESENTER, loadedKey);
    PersistentCache.get().saveKey(outState, KEY_LOAD_ADAPTER, loadedAdapterKey);
  }

  @Override public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    super.onCreateOptionsMenu(menu, inflater);
    inflater.inflate(R.menu.purge_old_menu, menu);
  }

  @Override public boolean onOptionsItemSelected(MenuItem item) {
    return super.onOptionsItemSelected(item);
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
  }

  @Override public void onStaleApplicationRetrieved(@NonNull String name) {
    fastItemAdapter.add(new PurgeItem(name));
  }

  @Override public void onRetrievalComplete() {
    forceRefresh = false;
  }
}
