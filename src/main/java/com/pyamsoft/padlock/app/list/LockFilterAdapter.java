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

import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.widget.Filter;
import com.mikepenz.fastadapter.adapters.FastItemAdapter;
import com.mikepenz.fastadapter.items.AbstractItem;
import java.util.ArrayList;
import java.util.List;
import timber.log.Timber;

abstract class LockFilterAdapter<T extends AbstractItem> extends FastItemAdapter<T> {

  @NonNull private final List<T> allItemList = new ArrayList<>();

  @SuppressWarnings("WeakerAccess") @NonNull @CheckResult List<T> getAllItemList() {
    return allItemList;
  }

  @Override public FastItemAdapter<T> add(T item) {
    allItemList.add(item);
    return super.add(item);
  }

  @Override public FastItemAdapter<T> remove(int position) {
    allItemList.remove(position);
    return super.remove(position);
  }

  @CheckResult @NonNull Filter getFilter() {
    return new Filter() {
      @Override protected FilterResults performFiltering(CharSequence query) {
        final List<T> listResult = new ArrayList<>();
        if (query == null) {
          listResult.addAll(getAllItemList());
        } else {
          query = query.toString().toLowerCase().trim();
          for (T item : getAllItemList()) {
            if (filterQuery(getFilterName(item), query)) {
              Timber.d("Add %s to filter", item);
              listResult.add(item);
            }
          }
        }

        // Return the filter result
        final FilterResults result = new FilterResults();
        result.values = listResult;
        result.count = listResult.size();
        return result;
      }

      @Override
      protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
        final List<T> listFiltered = getAdapterItems();
        listFiltered.clear();
        if (filterResults == null) {
          listFiltered.addAll(getAllItemList());
        } else {
          //noinspection unchecked
          listFiltered.addAll((List<T>) filterResults.values);
        }

        Timber.d("Nofiy changes");
        notifyAdapterDataSetChanged();
      }
    };
  }

  @CheckResult boolean filterQuery(@NonNull String name, @NonNull CharSequence query) {
    name = name.toLowerCase();
    Timber.d("Compare lock list: '%s' %s", query, name);
    return name.startsWith(String.valueOf(query));
  }

  @CheckResult @NonNull abstract String getFilterName(@NonNull T item);
}
