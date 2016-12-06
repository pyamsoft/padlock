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

import android.databinding.DataBindingUtil;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.RadioButton;
import com.mikepenz.fastadapter.items.AbstractItem;
import com.mikepenz.fastadapter.utils.ViewHolderFactory;
import com.pyamsoft.padlock.R;
import com.pyamsoft.padlock.databinding.AdapterItemLockinfoBinding;
import com.pyamsoft.padlock.model.ActivityEntry;
import java.util.List;

class LockInfoItem extends AbstractItem<LockInfoItem, LockInfoItem.ViewHolder> {

  @NonNull private static final ViewHolderFactory<? extends ViewHolder> FACTORY = new ItemFactory();
  @NonNull private final String packageName;
  @NonNull private final ActivityEntry entry;

  LockInfoItem(@NonNull String packageName, @NonNull ActivityEntry entry) {
    this.packageName = packageName;
    this.entry = entry;
  }

  @NonNull @CheckResult ActivityEntry getEntry() {
    return entry;
  }

  @NonNull @CheckResult String getPackageName() {
    return packageName;
  }

  @Override public int getType() {
    return R.id.adapter_lock_info;
  }

  @Override public int getLayoutRes() {
    return R.layout.adapter_item_lockinfo;
  }

  @Override public void bindView(ViewHolder holder, List<Object> payloads) {
    super.bindView(holder, payloads);

    // Remove any old binds
    final RadioButton lockedButton;
    switch (entry.lockState()) {
      case DEFAULT:
        lockedButton = holder.binding.lockInfoRadioDefault;
        break;
      case WHITELISTED:
        lockedButton = holder.binding.lockInfoRadioWhite;
        break;
      case LOCKED:
        lockedButton = holder.binding.lockInfoRadioBlack;
        break;
      default:
        throw new IllegalStateException("Illegal enum state");
    }

    holder.binding.lockInfoRadioBlack.setOnCheckedChangeListener(null);
    holder.binding.lockInfoRadioWhite.setOnCheckedChangeListener(null);
    holder.binding.lockInfoRadioDefault.setOnCheckedChangeListener(null);
    lockedButton.setChecked(true);

    final String entryName = entry.name();
    final String activityName;
    if (entryName.indexOf('.') == packageName.length() && entryName.startsWith(packageName)) {
      activityName = entryName.replace(packageName, "");
    } else {
      activityName = entryName;
    }
    holder.binding.lockInfoActivity.setText(activityName);
  }

  @Override public void unbindView(ViewHolder holder) {
    super.unbindView(holder);
    holder.binding.lockInfoActivity.setText(null);
    holder.binding.lockInfoRadioBlack.setOnCheckedChangeListener(null);
    holder.binding.lockInfoRadioWhite.setOnCheckedChangeListener(null);
    holder.binding.lockInfoRadioDefault.setOnCheckedChangeListener(null);
  }

  @Override public ViewHolderFactory<? extends ViewHolder> getFactory() {
    return FACTORY;
  }

  @SuppressWarnings("WeakerAccess") protected static class ItemFactory
      implements ViewHolderFactory<ViewHolder> {
    @Override public ViewHolder create(View v) {
      return new ViewHolder(v);
    }
  }

  protected static final class ViewHolder extends RecyclerView.ViewHolder {

    @NonNull final AdapterItemLockinfoBinding binding;

    public ViewHolder(View itemView) {
      super(itemView);
      binding = DataBindingUtil.bind(itemView);
    }
  }
}
