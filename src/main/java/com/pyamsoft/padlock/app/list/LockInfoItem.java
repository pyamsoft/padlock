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

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import com.mikepenz.fastadapter.items.AbstractItem;
import com.mikepenz.fastadapter.utils.ViewHolderFactory;
import com.pyamsoft.padlock.R;
import com.pyamsoft.padlock.model.ActivityEntry;
import java.util.List;

public class LockInfoItem extends AbstractItem<LockInfoItem, LockInfoItem.ViewHolder> {

  @NonNull static final ViewHolderFactory<? extends ViewHolder> FACTORY = new ItemFactory();
  @NonNull final String packageName;
  @NonNull final ActivityEntry entry;

  public LockInfoItem(@NonNull String packageName, @NonNull ActivityEntry entry) {
    this.packageName = packageName;
    this.entry = entry;
  }

  @Override public int getType() {
    return R.id.adapter_lock_info;
  }

  @Override public int getLayoutRes() {
    return R.layout.adapter_item_lockinfo;
  }

  @Override public void bindView(ViewHolder holder, List payloads) {
    super.bindView(holder, payloads);

    switch (entry.lockState()) {
      case DEFAULT:
        holder.defaultLockState.setChecked(true);
        break;
      case WHITELISTED:
        holder.whiteLockState.setChecked(true);
        break;
      case LOCKED:
        holder.blackLockState.setChecked(true);
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
    holder.name.setText(activityName);

    // TODO set on check change listener
    //holder.name.setOnClickListener(
    //    view -> dbPresenter.attemptDBModification(position, !holder.name.isChecked(),
    //        appEntry.packageName(), entryName, null, appEntry.system()));
  }

  @Override public ViewHolderFactory<? extends ViewHolder> getFactory() {
    return FACTORY;
  }

  protected static class ItemFactory implements ViewHolderFactory<ViewHolder> {
    public ViewHolder create(View v) {
      return new ViewHolder(v);
    }
  }

  protected static final class ViewHolder extends RecyclerView.ViewHolder {

    @NonNull final Unbinder unbinder;
    @BindView(R.id.lock_info_activity) TextView name;
    @BindView(R.id.lock_info_tristate_radiogroup) RadioGroup triStateGroup;
    @BindView(R.id.lock_info_radio_default) RadioButton defaultLockState;
    @BindView(R.id.lock_info_radio_white) RadioButton whiteLockState;
    @BindView(R.id.lock_info_radio_black) RadioButton blackLockState;

    public ViewHolder(View itemView) {
      super(itemView);
      unbinder = ButterKnife.bind(this, itemView);
    }
  }
}
