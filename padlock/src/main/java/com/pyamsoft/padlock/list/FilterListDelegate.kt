/*
 *     Copyright (C) 2017 Peter Kenji Yamanaka
 *
 *     This program is free software; you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation; either version 2 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License along
 *     with this program; if not, write to the Free Software Foundation, Inc.,
 *     51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.pyamsoft.padlock.list

import android.support.annotation.CheckResult
import android.support.v7.widget.SearchView
import android.view.Menu
import android.view.MenuItem
import com.mikepenz.fastadapter.adapters.ModelAdapter
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
