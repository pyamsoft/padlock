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
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.RadioButton;
import com.mikepenz.fastadapter.items.GenericAbstractItem;
import com.pyamsoft.padlock.Injector;
import com.pyamsoft.padlock.R;
import com.pyamsoft.padlock.databinding.AdapterItemLockinfoBinding;
import com.pyamsoft.padlock.model.ActivityEntry;
import com.pyamsoft.padlock.model.LockState;
import com.pyamsoft.pydroid.util.DialogUtil;
import java.util.List;
import javax.inject.Inject;
import timber.log.Timber;

import static com.pyamsoft.padlock.model.LockState.DEFAULT;
import static com.pyamsoft.padlock.model.LockState.LOCKED;
import static com.pyamsoft.padlock.model.LockState.WHITELISTED;

public class LockInfoItem
    extends GenericAbstractItem<ActivityEntry, LockInfoItem, LockInfoItem.ViewHolder>
    implements FilterableItem<LockInfoItem, LockInfoItem.ViewHolder> {

  @NonNull private final String packageName;
  private final boolean system;
  @SuppressWarnings("WeakerAccess") @Inject LockInfoItemPresenter presenter;

  LockInfoItem(@NonNull String packageName, boolean system, @NonNull ActivityEntry entry) {
    super(entry);
    this.packageName = packageName;
    this.system = system;

    Injector.get().provideComponent().plusLockInfoComponent().inject(this);
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
    switch (getModel().lockState()) {
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

    final String entryName = getModel().name();
    String activityName = entryName;
    if (entryName.startsWith(packageName)) {
      final String strippedPackageName = entryName.replace(packageName, "");
      if (strippedPackageName.charAt(0) == '.') {
        activityName = strippedPackageName;
      }
    }
    holder.binding.lockInfoActivity.setText(activityName);

    holder.binding.lockInfoTristateRadiogroup.setOnCheckedChangeListener((radioGroup, i) -> {
      final int id = radioGroup.getCheckedRadioButtonId();
      Timber.d("Checked radio id: %d", id);
      if (id == 0) {
        Timber.e("No radiobutton is checked, set to default");
        holder.binding.lockInfoRadioDefault.setChecked(true);
      }
    });

    holder.binding.lockInfoRadioDefault.setOnCheckedChangeListener((buttonView, isChecked) -> {
      if (isChecked) {
        processModifyDatabaseEntry(holder, DEFAULT);
      }
    });

    holder.binding.lockInfoRadioWhite.setOnCheckedChangeListener((buttonView, isChecked) -> {
      if (isChecked) {
        processModifyDatabaseEntry(holder, WHITELISTED);
      }
    });
    holder.binding.lockInfoRadioBlack.setOnCheckedChangeListener((buttonView, isChecked) -> {
      if (isChecked) {
        processModifyDatabaseEntry(holder, LOCKED);
      }
    });
  }

  @SuppressWarnings("WeakerAccess") void processModifyDatabaseEntry(@NonNull ViewHolder viewHolder,
      @NonNull LockState newLockState) {
    presenter.modifyDatabaseEntry(getModel().lockState(), newLockState, packageName,
        getModel().name(), null, system, new LockInfoItemPresenter.ModifyDatabaseCallback() {
          @Override public void onDatabaseEntryError() {
            DialogUtil.onlyLoadOnceDialogFragment(
                (FragmentActivity) viewHolder.itemView.getContext(), new ErrorDialog(), "error");
          }

          private void updateModel(@NonNull LockState newState) {
            withModel(getModel().toBuilder().lockState(newState).build());
          }

          @Override public void onDatabaseEntryWhitelisted() {
            updateModel(WHITELISTED);
            viewHolder.binding.lockInfoRadioWhite.setChecked(true);
          }

          @Override public void onDatabaseEntryCreated() {
            updateModel(LOCKED);
            viewHolder.binding.lockInfoRadioBlack.setChecked(true);
          }

          @Override public void onDatabaseEntryDeleted() {
            updateModel(DEFAULT);
            viewHolder.binding.lockInfoRadioDefault.setChecked(true);
          }
        });
  }

  @Override public void unbindView(ViewHolder holder) {
    super.unbindView(holder);
    presenter.stop();
    presenter.destroy();
    holder.binding.lockInfoActivity.setText(null);
    holder.binding.lockInfoRadioBlack.setOnCheckedChangeListener(null);
    holder.binding.lockInfoRadioWhite.setOnCheckedChangeListener(null);
    holder.binding.lockInfoRadioDefault.setOnCheckedChangeListener(null);
    holder.binding.lockInfoTristateRadiogroup.setOnCheckedChangeListener(null);
  }

  @Override public boolean filterAgainst(@NonNull String query) {
    final String name = getModel().name().toLowerCase().trim();
    Timber.d("Filter predicate: '%s' against %s", query, name);
    return !name.contains(query);
  }

  @Override public ViewHolder getViewHolder(View view) {
    return new ViewHolder(view);
  }

  static final class ViewHolder extends RecyclerView.ViewHolder {

    @NonNull final AdapterItemLockinfoBinding binding;

    ViewHolder(View itemView) {
      super(itemView);
      binding = DataBindingUtil.bind(itemView);
    }
  }
}
