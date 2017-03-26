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

package com.pyamsoft.padlock.list;

import android.databinding.DataBindingUtil;
import android.graphics.drawable.Drawable;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.CompoundButton;
import com.mikepenz.fastadapter.items.GenericAbstractItem;
import com.mikepenz.fastadapter.utils.ViewHolderFactory;
import com.pyamsoft.padlock.Injector;
import com.pyamsoft.padlock.R;
import com.pyamsoft.padlock.databinding.AdapterItemLocklistEntryBinding;
import com.pyamsoft.padlock.iconloader.AppIconLoaderPresenter;
import com.pyamsoft.padlock.model.AppEntry;
import com.pyamsoft.pydroid.function.ActionSingle;
import java.lang.ref.WeakReference;
import java.util.List;
import javax.inject.Inject;
import timber.log.Timber;

public class LockListItem
    extends GenericAbstractItem<AppEntry, LockListItem, LockListItem.ViewHolder>
    implements FilterableItem<LockListItem, LockListItem.ViewHolder> {

  @NonNull private static final ViewHolderFactory<? extends ViewHolder> FACTORY = new ItemFactory();

  LockListItem(@NonNull AppEntry entry) {
    super(entry);
  }

  @NonNull @CheckResult String getName() {
    return getModel().name();
  }

  @NonNull @CheckResult String getPackageName() {
    return getModel().packageName();
  }

  @CheckResult @NonNull LockListItem copyWithNewLockState(boolean locked) {
    return new LockListItem(AppEntry.builder()
        .name(getModel().name())
        .packageName(getModel().packageName())
        .system(getModel().system())
        .locked(locked)
        .build());
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

  @Override public boolean filterAgainst(@NonNull String query) {
    final String name = getModel().name().toLowerCase().trim();
    Timber.d("Filter predicate: '%s' against %s", query, name);
    return !name.startsWith(query);
  }

  @Override public void bindView(ViewHolder holder, List<Object> payloads) {
    super.bindView(holder, payloads);
    holder.bind(getModel());
  }

  @Override public void unbindView(ViewHolder holder) {
    super.unbindView(holder);
    holder.unbind();
  }

  void onClick(@NonNull ActionSingle<AppEntry> click) {
    click.call(getModel());
  }

  interface OnLockSwitchCheckedChanged {

    void call(boolean isChecked, int position, @NonNull AppEntry entry);
  }

  @SuppressWarnings("WeakerAccess") protected static class ItemFactory
      implements ViewHolderFactory<ViewHolder> {
    @Override public ViewHolder create(View v) {
      return new ViewHolder(v);
    }
  }

  public static final class ViewHolder extends RecyclerView.ViewHolder {

    @NonNull final AdapterItemLocklistEntryBinding binding;
    @Inject AppIconLoaderPresenter appIconLoaderPresenter;
    @NonNull WeakReference<AppEntry> weakEntry;

    ViewHolder(View itemView) {
      super(itemView);
      binding = DataBindingUtil.bind(itemView);
      weakEntry = new WeakReference<>(null);

      Injector.get().provideComponent().plusLockListComponent().inject(this);
    }

    @NonNull @CheckResult AdapterItemLocklistEntryBinding getBinding() {
      return binding;
    }

    void loadImage(@NonNull String packageName) {
      appIconLoaderPresenter.loadApplicationIcon(packageName,
          new AppIconLoaderPresenter.LoadCallback() {
            @Override public void onApplicationIconLoadedSuccess(@NonNull Drawable icon) {
              binding.lockListIcon.setImageDrawable(icon);
            }

            @Override public void onApplicationIconLoadedError() {
              Timber.e("Failed to load icon into ViewHolder");
            }
          });
    }

    void bind(@NonNull AppEntry entry) {
      appIconLoaderPresenter.bindView(null);
      binding.lockListTitle.setText(entry.name());
      binding.lockListToggle.setOnCheckedChangeListener(null);
      binding.lockListToggle.setChecked(entry.locked());
      loadImage(entry.packageName());

      weakEntry.clear();
      weakEntry = new WeakReference<>(entry);
    }

    void unbind() {
      appIconLoaderPresenter.unbindView();
      binding.lockListTitle.setText(null);
      binding.lockListIcon.setImageDrawable(null);
      binding.lockListToggle.setOnCheckedChangeListener(null);
      weakEntry.clear();
    }

    void bind(@NonNull OnLockSwitchCheckedChanged onCheckChanged) {
      final CompoundButton.OnCheckedChangeListener listener =
          new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(@NonNull CompoundButton compoundButton, boolean b) {
              // Don't check it yet, get auth first
              compoundButton.setOnCheckedChangeListener(null);
              compoundButton.setChecked(!b);
              compoundButton.setOnCheckedChangeListener(this);

              // TODO Authorize for package access
              final AppEntry entry = weakEntry.get();
              if (entry != null) {
                onCheckChanged.call(b, getAdapterPosition(), entry);
              } else {
                Timber.e("AppEntry is NULL");
              }
            }
          };
      binding.lockListToggle.setOnCheckedChangeListener(listener);
    }
  }
}
