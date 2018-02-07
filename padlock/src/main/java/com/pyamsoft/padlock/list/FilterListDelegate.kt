/*
 * Copyright (C) 2018 Peter Kenji Yamanaka
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pyamsoft.padlock.list

import android.support.annotation.CheckResult
import android.support.v7.widget.SearchView
import android.view.Menu
import android.view.MenuItem
import com.mikepenz.fastadapter.adapters.ModelAdapter
import com.pyamsoft.padlock.R
import timber.log.Timber

internal class FilterListDelegate {

  private var searchView: SearchView? = null
  private var searchItem: MenuItem? = null

  /**
   * Prepare the menu item for search
   *
   * Must have an already inflater menu item to work
   */
  fun onPrepareOptionsMenu(
      menu: Menu,
      listAdapter: ModelAdapter<*, out FilterableItem<*, *>>
  ) {
    searchItem = menu.findItem(R.id.menu_search)
    val obj = searchItem
    if (obj != null) {
      searchView = obj.actionView as SearchView
      setSearchViewOnQueryTextListener(listAdapter)
    }
  }

  /**
   * Sets the list adapter to use a filter predicate
   */
  fun <T : FilterableItem<*, *>> onViewCreated(listAdapter: ModelAdapter<*, T>) {
    listAdapter.itemFilter.withFilterPredicate { item, query ->
      val queryString = query.toString()
          .toLowerCase()
          .trim { it <= ' ' }
      return@withFilterPredicate item.filterAgainst(queryString)
    }
  }

  fun setEnabled(enabled: Boolean) {
    searchItem?.isEnabled = enabled
    if (!enabled) {
      searchItem?.collapseActionView()
    }
  }

  fun onDestroyView() {
    searchItem?.collapseActionView()
    searchItem = null

    searchView?.setOnQueryTextListener(null)
    searchView?.setOnCloseListener(null)
    searchView = null
  }

  private fun setSearchViewOnQueryTextListener(
      listAdapter: ModelAdapter<*, out FilterableItem<*, *>>
  ) {
    Timber.d("Set Search View listeners")
    searchView?.setOnQueryTextListener(getOnQueryTextListener(listAdapter))
    searchView?.setOnCloseListener {
      listAdapter.filter(null)
      return@setOnCloseListener true
    }
  }

  @CheckResult
  private fun getOnQueryTextListener(
      listAdapter: ModelAdapter<*, out FilterableItem<*, *>>
  ): SearchView.OnQueryTextListener {
    return object : SearchView.OnQueryTextListener {

      override fun onQueryTextChange(newText: String): Boolean {
        listAdapter.filter(newText)
        return true
      }

      override fun onQueryTextSubmit(query: String): Boolean {
        listAdapter.filter(query)
        searchView?.clearFocus()
        return true
      }
    }
  }
}
