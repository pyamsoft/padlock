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
import android.widget.CompoundButton;
import android.widget.RadioButton;
import com.mikepenz.fastadapter.items.GenericAbstractItem;
import com.mikepenz.fastadapter.utils.ViewHolderFactory;
import com.pyamsoft.padlock.R;
import com.pyamsoft.padlock.databinding.AdapterItemLockinfoBinding;
import com.pyamsoft.padlock.model.ActivityEntry;
import com.pyamsoft.padlock.model.LockState;
import java.lang.ref.WeakReference;
import java.util.List;
import timber.log.Timber;

class LockInfoItem extends GenericAbstractItem<ActivityEntry, LockInfoItem, LockInfoItem.ViewHolder>
    implements FilterableItem<LockInfoItem, LockInfoItem.ViewHolder> {

  @NonNull private static final ViewHolderFactory<? extends ViewHolder> FACTORY = new ItemFactory();
  @NonNull private final String packageName;

  LockInfoItem(@NonNull String packageName, @NonNull ActivityEntry entry) {
    super(entry);
    this.packageName = packageName;
  }

  @NonNull @CheckResult String getName() {
    return getModel().name();
  }

  @CheckResult @NonNull LockInfoItem copyWithNewLockState(@NonNull LockState newState) {
    return new LockInfoItem(packageName,
        ActivityEntry.builder(getModel()).lockState(newState).build());
  }

  @Override public int getType() {
    return R.id.adapter_lock_info;
  }

  @Override public int getLayoutRes() {
    return R.layout.adapter_item_lockinfo;
  }

  @Override public void bindView(ViewHolder holder, List<Object> payloads) {
    super.bindView(holder, payloads);
    holder.bind(packageName, getModel());
  }

  @Override public void unbindView(ViewHolder holder) {
    super.unbindView(holder);
    holder.unbind();
  }

  @Override public boolean filterAgainst(@NonNull String query) {
    final String name = getModel().name().toLowerCase().trim();
    Timber.d("Filter predicate: '%s' against %s", query, name);
    return !name.contains(query);
  }

  @Override public ViewHolderFactory<? extends ViewHolder> getFactory() {
    return FACTORY;
  }

  interface OnLockRadioCheckedChanged {

    void call(int position, @NonNull String name, @NonNull LockState oldState,
        @NonNull LockState newState);
  }

  @SuppressWarnings("WeakerAccess") protected static class ItemFactory
      implements ViewHolderFactory<ViewHolder> {
    @Override public ViewHolder create(View v) {
      return new ViewHolder(v);
    }
  }

  protected static final class ViewHolder extends RecyclerView.ViewHolder {

    @NonNull private final AdapterItemLockinfoBinding binding;
    @NonNull private WeakReference<ActivityEntry> weakEntry;

    ViewHolder(View itemView) {
      super(itemView);
      binding = DataBindingUtil.bind(itemView);
      weakEntry = new WeakReference<>(null);
    }

    @CheckResult @NonNull AdapterItemLockinfoBinding getBinding() {
      return binding;
    }

    void bind(@NonNull String packageName, @NonNull ActivityEntry entry) {
      // Remove any old binds
      final RadioButton lockedButton;
      switch (entry.lockState()) {
        case DEFAULT:
          lockedButton = binding.lockInfoRadioDefault;
          break;
        case WHITELISTED:
          lockedButton = binding.lockInfoRadioWhite;
          break;
        case LOCKED:
          lockedButton = binding.lockInfoRadioBlack;
          break;
        default:
          throw new IllegalStateException("Illegal enum state");
      }
      binding.lockInfoRadioBlack.setOnCheckedChangeListener(null);
      binding.lockInfoRadioWhite.setOnCheckedChangeListener(null);
      binding.lockInfoRadioDefault.setOnCheckedChangeListener(null);
      lockedButton.setChecked(true);

      final String entryName = entry.name();
      String activityName = entryName;
      if (entryName.startsWith(packageName)) {
        final String strippedPackageName = entryName.replace(packageName, "");
        if (strippedPackageName.charAt(0) == '.') {
          activityName = strippedPackageName;
        }
      }
      binding.lockInfoActivity.setText(activityName);

      weakEntry.clear();
      weakEntry = new WeakReference<>(entry);
    }

    void bind(@NonNull OnLockRadioCheckedChanged onCheckedChanged) {
      binding.lockInfoTristateRadiogroup.setOnCheckedChangeListener((radioGroup, i) -> {
        final int id = radioGroup.getCheckedRadioButtonId();
        Timber.d("Checked radio id: %d", id);
        if (id == 0) {
          Timber.e("No radiobutton is checked, set to default");
          getBinding().lockInfoRadioDefault.setChecked(true);
        }
      });
      binding.lockInfoRadioDefault.setOnCheckedChangeListener(
          new OnRadioCheckChangedListener(getAdapterPosition(), weakEntry, onCheckedChanged,
              LockState.DEFAULT));
      binding.lockInfoRadioWhite.setOnCheckedChangeListener(
          new OnRadioCheckChangedListener(getAdapterPosition(), weakEntry, onCheckedChanged,
              LockState.WHITELISTED));
      binding.lockInfoRadioBlack.setOnCheckedChangeListener(
          new OnRadioCheckChangedListener(getAdapterPosition(), weakEntry, onCheckedChanged,
              LockState.LOCKED));
    }

    void unbind() {
      binding.lockInfoActivity.setText(null);
      binding.lockInfoRadioBlack.setOnCheckedChangeListener(null);
      binding.lockInfoRadioWhite.setOnCheckedChangeListener(null);
      binding.lockInfoRadioDefault.setOnCheckedChangeListener(null);
      binding.lockInfoTristateRadiogroup.setOnCheckedChangeListener(null);
      weakEntry.clear();
    }

    static class OnRadioCheckChangedListener implements CompoundButton.OnCheckedChangeListener {

      private final int position;
      @NonNull private final WeakReference<ActivityEntry> weakEntry;
      @NonNull private final OnLockRadioCheckedChanged onCheckedChanged;
      @NonNull private final LockState changeToState;

      OnRadioCheckChangedListener(int position, @NonNull WeakReference<ActivityEntry> weakEntry,
          @NonNull OnLockRadioCheckedChanged onCheckedChanged, @NonNull LockState changeToState) {
        this.position = position;
        this.weakEntry = weakEntry;
        this.onCheckedChanged = onCheckedChanged;
        this.changeToState = changeToState;
      }

      @Override public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
        if (b) {
          final ActivityEntry entry = weakEntry.get();
          if (entry != null) {
            onCheckedChanged.call(position, entry.name(), entry.lockState(), changeToState);
          } else {
            Timber.e("Entry is NULL");
          }
        }
      }
    }
  }
}
