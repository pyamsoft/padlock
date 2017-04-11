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

import android.databinding.DataBindingUtil;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import com.mikepenz.fastadapter.items.GenericAbstractItem;
import com.pyamsoft.padlock.R;
import com.pyamsoft.padlock.databinding.AdapterItemPurgeBinding;
import java.util.List;

class PurgeItem extends GenericAbstractItem<String, PurgeItem, PurgeItem.ViewHolder> {

  PurgeItem(@NonNull String packageName) {
    super(packageName);
  }

  @Override public int getType() {
    return R.id.adapter_purge;
  }

  @Override public int getLayoutRes() {
    return R.layout.adapter_item_purge;
  }

  @Override public ViewHolder getViewHolder(View view) {
    return new ViewHolder(view);
  }

  @Override public void unbindView(ViewHolder holder) {
    super.unbindView(holder);
    holder.binding.itemPurgeName.setText(null);
  }

  @Override public void bindView(ViewHolder holder, List<Object> payloads) {
    super.bindView(holder, payloads);
    holder.binding.itemPurgeName.setText(getModel());
  }

  static class ViewHolder extends RecyclerView.ViewHolder {

    @NonNull final AdapterItemPurgeBinding binding;

    ViewHolder(View itemView) {
      super(itemView);
      binding = DataBindingUtil.bind(itemView);
    }
  }
}
