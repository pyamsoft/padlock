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
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.CompoundButton;
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
  @Nullable private OnDatabaseModifyListener modifyListener;
  @Nullable private OnOpenDialogRequestListener requestListener;

  LockListItem(@NonNull AppEntry entry, @NonNull OnOpenDialogRequestListener requestListener,
      @NonNull OnDatabaseModifyListener modifyListener) {
    this.entry = entry;
    this.requestListener = requestListener;
    this.modifyListener = modifyListener;
  }

  @NonNull @CheckResult AppEntry getEntry() {
    return entry;
  }

  @NonNull @CheckResult OnOpenDialogRequestListener getRequestListener() {
    if (requestListener == null) {
      throw new NullPointerException("RequestListener is NULL");
    }
    return requestListener;
  }

  void setRequestListener(@NonNull OnOpenDialogRequestListener requestListener) {
    this.requestListener = requestListener;
  }

  void cleanup() {
    modifyListener = null;
    requestListener = null;
  }

  @NonNull @CheckResult OnDatabaseModifyListener getModifyListener() {
    if (modifyListener == null) {
      throw new NullPointerException("Modify listener is NULL");
    }
    return modifyListener;
  }

  void setModifyListener(@NonNull OnDatabaseModifyListener modifyListener) {
    this.modifyListener = modifyListener;
  }

  @Override public int getType() {
    return R.id.adapter_lock_item;
  }

  @Override public int getLayoutRes() {
    return R.layout.adapter_item_locklist_entry;
  }

  @Override public void unbindView(ViewHolder holder) {
    super.unbindView(holder);
    holder.binding.lockListTitle.setText(null);
    holder.binding.lockListTitle.setOnClickListener(null);
    holder.binding.lockListIcon.setOnClickListener(null);
    holder.binding.lockListIcon.setImageDrawable(null);
    holder.binding.lockListToggle.setOnCheckedChangeListener(null);
  }

  @Override public void bindView(ViewHolder holder, List payloads) {
    super.bindView(holder, payloads);

    holder.binding.lockListTitle.setText(entry.name());
    holder.loadImage(entry.packageName());
    holder.binding.lockListToggle.setOnCheckedChangeListener(null);
    holder.binding.lockListToggle.setChecked(entry.locked());
    final CompoundButton.OnCheckedChangeListener listener =
        new CompoundButton.OnCheckedChangeListener() {
          @Override
          public void onCheckedChanged(@NonNull CompoundButton compoundButton, boolean b) {
            // Don't check it yet, get auth first
            compoundButton.setOnCheckedChangeListener(null);
            compoundButton.setChecked(!b);
            compoundButton.setOnCheckedChangeListener(this);

            // Authorize for package access
            authorizeAccess(holder.getAdapterPosition(), true, b);
          }
        };

    holder.binding.lockListToggle.setOnCheckedChangeListener(listener);
    holder.binding.lockListTitle.setOnClickListener(
        view -> authorizeAccess(holder.getAdapterPosition(), false, false));
    holder.binding.lockListIcon.setOnClickListener(
        view -> authorizeAccess(holder.getAdapterPosition(), false, false));
  }

  @SuppressWarnings("WeakerAccess") void authorizeAccess(int position, boolean accessPackage,
      boolean isChecked) {
    // TODO some kind of observable which can confirm correct passcode entry

    Timber.d("Access authorized");
    if (accessPackage) {
      Timber.d("Access package");
      accessPackage(position, isChecked);
    } else {
      Timber.d("Access activities");
      openInfo();
    }
  }

  private void accessPackage(int position, boolean isChecked) {
    // TODO app specific codes
    if (modifyListener != null) {
      modifyListener.onDatabaseModify(isChecked, position, entry);
    } else {
      Timber.e("No modify listener attached");
    }
  }

  private void openInfo() {
    if (requestListener != null) {
      requestListener.onOpenLockInfoRequest(entry);
    } else {
      Timber.e("No request listener attached");
    }
  }

  @Override public ViewHolderFactory<? extends ViewHolder> getFactory() {
    return FACTORY;
  }

  interface OnDatabaseModifyListener {

    void onDatabaseModify(boolean lock, int position, @NonNull AppEntry entry);
  }

  interface OnOpenDialogRequestListener {

    void onOpenLockInfoRequest(@NonNull AppEntry entry);
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
