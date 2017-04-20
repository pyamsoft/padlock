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

import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.view.MenuItem;
import com.mikepenz.fastadapter.commons.adapters.FastItemAdapter;
import com.pyamsoft.padlock.R;
import timber.log.Timber;

class FilterListDelegate {

  @SuppressWarnings("WeakerAccess") @Nullable SearchView searchView;
  @Nullable private MenuItem searchItem;

  /**
   * Prepare the menu item for search
   *
   * Must have an already inflater menu item to work
   */
  void onPrepareOptionsMenu(@NonNull Menu menu,
      @NonNull FastItemAdapter<? extends FilterableItem> listAdapter) {
    searchItem = menu.findItem(R.id.menu_search);
    if (searchItem != null) {
      searchView = (SearchView) searchItem.getActionView();
      setSearchViewOnQueryTextListener(listAdapter);
    }
  }

  /**
   * Sets the list adapter to use a filter predicate
   */
  void onViewCreated(@NonNull FastItemAdapter<? extends FilterableItem> listAdapter) {
    listAdapter.withFilterPredicate((item, query) -> {
      final String queryString = String.valueOf(query).toLowerCase().trim();
      return item.filterAgainst(queryString);
    });
  }

  void onDestroyView() {
    if (searchItem != null) {
      searchItem.collapseActionView();
    }
    searchItem = null;

    if (searchView != null) {
      searchView.setOnQueryTextListener(null);
      searchView.setOnCloseListener(null);
    }
    searchView = null;
  }

  private void setSearchViewOnQueryTextListener(
      @NonNull FastItemAdapter<? extends FilterableItem> listAdapter) {
    if (searchView != null) {
      Timber.d("Set Search View listeners");
      searchView.setOnQueryTextListener(getOnQueryTextListener(listAdapter));
      searchView.setOnCloseListener(() -> {
        listAdapter.filter(null);
        return true;
      });
    }
  }

  @CheckResult @NonNull private SearchView.OnQueryTextListener getOnQueryTextListener(
      @NonNull FastItemAdapter<? extends FilterableItem> listAdapter) {
    return new SearchView.OnQueryTextListener() {

      @Override public boolean onQueryTextChange(String newText) {
        listAdapter.filter(newText);
        return true;
      }

      @Override public boolean onQueryTextSubmit(String query) {
        listAdapter.filter(query);
        if (searchView != null) {
          searchView.clearFocus();
        }
        return true;
      }
    };
  }
}
