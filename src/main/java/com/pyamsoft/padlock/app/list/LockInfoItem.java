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
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import com.mikepenz.fastadapter.items.AbstractItem;
import com.mikepenz.fastadapter.utils.ViewHolderFactory;
import com.pyamsoft.padlock.R;
import com.pyamsoft.padlock.databinding.AdapterItemLockinfoBinding;
import com.pyamsoft.padlock.model.ActivityEntry;
import com.pyamsoft.padlock.model.LockState;
import java.util.List;

class LockInfoItem extends AbstractItem<LockInfoItem, LockInfoItem.ViewHolder> {

  @NonNull private static final ViewHolderFactory<? extends ViewHolder> FACTORY = new ItemFactory();
  @NonNull private final String packageName;
  @NonNull private final ActivityEntry entry;
  @SuppressWarnings("WeakerAccess") @Nullable OnLockStateChangeListener listener;

  LockInfoItem(@NonNull String packageName, @NonNull ActivityEntry entry,
      @NonNull OnLockStateChangeListener listener) {
    this.packageName = packageName;
    this.entry = entry;
    this.listener = listener;
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

  @NonNull @CheckResult OnLockStateChangeListener getListener() {
    if (listener == null) {
      throw new NullPointerException("Lister is NULL");
    }
    return listener;
  }

  void setListener(@Nullable OnLockStateChangeListener listener) {
    this.listener = listener;
  }

  void cleanup() {
    listener = null;
  }

  @Override public void unbindView(ViewHolder holder) {
    super.unbindView(holder);
    holder.binding.lockInfoActivity.setText(null);
    holder.binding.lockInfoActivity.setOnClickListener(null);
    holder.binding.lockInfoRadioBlack.setOnCheckedChangeListener(null);
    holder.binding.lockInfoRadioWhite.setOnCheckedChangeListener(null);
    holder.binding.lockInfoRadioDefault.setOnCheckedChangeListener(null);
  }

  @Override public void bindView(ViewHolder holder, List payloads) {
    super.bindView(holder, payloads);

    switch (entry.lockState()) {
      case DEFAULT:
        holder.binding.lockInfoRadioDefault.setChecked(true);
        break;
      case WHITELISTED:
        holder.binding.lockInfoRadioWhite.setChecked(true);
        break;
      case LOCKED:
        holder.binding.lockInfoRadioBlack.setChecked(true);
        break;
      default:
        throw new IllegalStateException("Illegal enum state");
    }

    final String entryName = entry.name();
    final String activityName;
    if (entryName.startsWith(packageName)) {
      activityName = entryName.replace(packageName, "");
    } else {
      activityName = entryName;
    }
    holder.binding.lockInfoActivity.setText(activityName);

    holder.binding.lockInfoRadioDefault.setOnCheckedChangeListener((compoundButton, b) -> {
      if (b) {
        if (listener != null) {
          listener.onLockStateChange(holder.getAdapterPosition(), entryName, getEntry().lockState(),
              LockState.DEFAULT);
        }
      }
    });

    holder.binding.lockInfoRadioWhite.setOnCheckedChangeListener((compoundButton, b) -> {
      if (b) {
        if (listener != null) {
          listener.onLockStateChange(holder.getAdapterPosition(), entryName, getEntry().lockState(),
              LockState.WHITELISTED);
        }
      }
    });

    holder.binding.lockInfoRadioBlack.setOnCheckedChangeListener((compoundButton, b) -> {
      if (b) {
        if (listener != null) {
          listener.onLockStateChange(holder.getAdapterPosition(), entryName, getEntry().lockState(),
              LockState.LOCKED);
        }
      }
    });
  }

  @Override public ViewHolderFactory<? extends ViewHolder> getFactory() {
    return FACTORY;
  }

  interface OnLockStateChangeListener {

    void onLockStateChange(int position, @NonNull String name, @NonNull LockState currentState,
        @NonNull LockState newState);
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
