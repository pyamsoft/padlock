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

import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import com.mikepenz.fastadapter.commons.adapters.FastItemAdapter;
import com.pyamsoft.padlock.R;
import com.pyamsoft.pydroid.app.fragment.ActionBarFragment;
import timber.log.Timber;

abstract class FilterListFragment extends ActionBarFragment {

  @Nullable SearchView searchView;
  @Nullable private MenuItem searchItem;

  @CallSuper @Override public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setHasOptionsMenu(true);
  }

  @CallSuper @Override
  public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
    super.onCreateOptionsMenu(menu, inflater);
    inflater.inflate(R.menu.search_menu, menu);
  }

  @CallSuper @Override public void onPrepareOptionsMenu(@NonNull Menu menu) {
    super.onPrepareOptionsMenu(menu);
    setupSearchItem(menu);
  }

  @CallSuper
  @Override public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    getListAdapter().withFilterPredicate((item, query) -> {
      final String queryString = String.valueOf(query).toLowerCase().trim();
      return item.filterAgainst(queryString);
    });
  }

  private void setupSearchItem(@NonNull Menu menu) {
    searchItem = menu.findItem(R.id.menu_search);
    if (searchItem != null) {
      searchView = (SearchView) searchItem.getActionView();
      setSearchViewOnQueryTextListener();
    }
  }

  private void setSearchViewOnQueryTextListener() {
    if (searchView != null) {
      Timber.d("Set Search View listeners");
      searchView.setOnQueryTextListener(getOnQueryTextListener());
      searchView.setOnCloseListener(() -> {
        getListAdapter().filter(null);
        return true;
      });
    }
  }

  @CheckResult @NonNull abstract FastItemAdapter<? extends FilterableItem> getListAdapter();

  @CheckResult @NonNull private SearchView.OnQueryTextListener getOnQueryTextListener() {
    return new SearchView.OnQueryTextListener() {

      @Override public boolean onQueryTextChange(String newText) {
        getListAdapter().filter(newText);
        return true;
      }

      @Override public boolean onQueryTextSubmit(String query) {
        getListAdapter().filter(query);
        if (searchView != null) {
          searchView.clearFocus();
        }
        return true;
      }
    };
  }

  @CallSuper @Override public void onDestroyView() {
    if (searchItem != null) {
      searchItem.collapseActionView();
    }
    searchItem = null;

    if (searchView != null) {
      searchView.setOnQueryTextListener(null);
      searchView.setOnCloseListener(null);
    }
    searchView = null;
    super.onDestroyView();
  }
}
