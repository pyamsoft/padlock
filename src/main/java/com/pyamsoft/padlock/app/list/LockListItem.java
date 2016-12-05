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
import android.graphics.drawable.Drawable;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import com.mikepenz.fastadapter.items.AbstractItem;
import com.mikepenz.fastadapter.utils.ViewHolderFactory;
import com.pyamsoft.padlock.Injector;
import com.pyamsoft.padlock.R;
import com.pyamsoft.padlock.app.iconloader.AppIconLoaderPresenter;
import com.pyamsoft.padlock.app.iconloader.AppIconLoaderView;
import com.pyamsoft.padlock.databinding.AdapterItemLocklistEntryBinding;
import com.pyamsoft.padlock.model.AppEntry;
import java.util.List;
import javax.inject.Inject;
import timber.log.Timber;

public class LockListItem extends AbstractItem<LockListItem, LockListItem.ViewHolder> {

  @NonNull private static final ViewHolderFactory<? extends ViewHolder> FACTORY = new ItemFactory();

  @NonNull private final AppEntry entry;

  LockListItem(@NonNull AppEntry entry) {
    this.entry = entry;
  }

  @NonNull @CheckResult AppEntry getEntry() {
    return entry;
  }

  @Override public int getType() {
    return R.id.adapter_lock_item;
  }

  @Override public int getLayoutRes() {
    return R.layout.adapter_item_locklist_entry;
  }

  @Override public ViewHolderFactory<? extends ViewHolder> getFactory() {
    return FACTORY;
  }

  @Override public void bindView(ViewHolder holder, List payloads) {
    super.bindView(holder, payloads);
    holder.binding.lockListTitle.setText(entry.name());
    holder.loadImage(entry.packageName());
    holder.binding.lockListToggle.setOnCheckedChangeListener(null);
    holder.binding.lockListToggle.setChecked(entry.locked());
  }

  @Override public void unbindView(ViewHolder holder) {
    super.unbindView(holder);
    holder.binding.lockListTitle.setText(null);
    holder.binding.lockListTitle.setOnClickListener(null);
    holder.binding.lockListIcon.setOnClickListener(null);
    holder.binding.lockListIcon.setImageDrawable(null);
    holder.binding.lockListToggle.setOnCheckedChangeListener(null);
  }

  @SuppressWarnings("WeakerAccess") protected static class ItemFactory
      implements ViewHolderFactory<ViewHolder> {
    @Override public ViewHolder create(View v) {
      return new ViewHolder(v);
    }
  }

  public static final class ViewHolder extends RecyclerView.ViewHolder
      implements AppIconLoaderView {

    @NonNull final AdapterItemLocklistEntryBinding binding;
    @Inject AppIconLoaderPresenter<ViewHolder> appIconLoaderPresenter;

    public ViewHolder(View itemView) {
      super(itemView);
      binding = DataBindingUtil.bind(itemView);

      Injector.get().provideComponent().plusAppIconLoaderComponent().inject(this);
      appIconLoaderPresenter.bindView(this);
    }

    void loadImage(@NonNull String packageName) {
      appIconLoaderPresenter.loadApplicationIcon(packageName);
    }

    @Override public void onApplicationIconLoadedSuccess(@NonNull Drawable icon) {
      binding.lockListIcon.setImageDrawable(icon);
    }

    @Override public void onApplicationIconLoadedError() {
      Timber.e("Failed to load icon into ViewHolder");
    }
  }
}
