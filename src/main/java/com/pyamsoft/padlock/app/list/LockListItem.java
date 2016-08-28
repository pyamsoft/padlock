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

import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import com.mikepenz.fastadapter.items.AbstractItem;
import com.pyamsoft.padlock.R;
import com.pyamsoft.padlock.app.base.AppIconLoaderView;
import com.pyamsoft.padlock.app.bus.DBProgressBus;
import com.pyamsoft.padlock.app.bus.LockInfoBus;
import com.pyamsoft.padlock.model.AppEntry;
import com.pyamsoft.padlock.model.event.DBProgressEvent;
import com.pyamsoft.padlock.model.event.LockInfoDisplayEvent;
import java.util.List;
import timber.log.Timber;

public class LockListItem extends AbstractItem<LockListItem, LockListItem.ViewHolder>
    implements AppIconLoaderView {

  @NonNull final AppEntry entry;

  public LockListItem(@NonNull AppEntry entry) {
    this.entry = entry;
  }

  @Override public int getType() {
    return R.id.adapter_lock_item;
  }

  @Override public int getLayoutRes() {
    return R.layout.adapter_item_locklist_entry;
  }

  @Override public void bindView(ViewHolder holder, List payloads) {
    super.bindView(holder, payloads);
    holder.name.setText(entry.name());

    // TODO load application icon
    //adapterPresenter.loadApplicationIcon(holder, entry.packageName());

    holder.toggle.setOnCheckedChangeListener(null);
    holder.toggle.setChecked(entry.locked());
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

    holder.toggle.setOnCheckedChangeListener(listener);
    holder.name.setOnClickListener(
        view -> authorizeAccess(holder.getAdapterPosition(), false, false));
    holder.icon.setOnClickListener(
        view -> authorizeAccess(holder.getAdapterPosition(), false, false));
  }

  void authorizeAccess(int position, boolean accessPackage, boolean checked) {
    // TODO some kind of observable which can confirm correct passcode entry

    Timber.d("Access authorized");
    if (accessPackage) {
      Timber.d("Access package");
      accessPackage(position, checked);
    } else {
      Timber.d("Access activities");
      openInfo();
    }
  }

  void accessPackage(int position, boolean checked) {
    // TODO app specific codes

    // TODO when received post, modify DB package values
    DBProgressBus.get().post(DBProgressEvent.create(position, checked, entry));
  }

  void openInfo() {
    // TODO when received post, show LockInfoDialog for entry
    LockInfoBus.get().post(LockInfoDisplayEvent.create(entry));
  }

  @Override public void onApplicationIconLoadedSuccess(@NonNull Drawable icon) {

  }

  @Override public void onApplicationIconLoadedError() {

  }

  protected static final class ViewHolder extends RecyclerView.ViewHolder {

    @NonNull final Unbinder unbinder;
    @BindView(R.id.lock_list_title) TextView name;
    @BindView(R.id.lock_list_icon) ImageView icon;
    @BindView(R.id.lock_list_toggle) SwitchCompat toggle;

    public ViewHolder(View itemView) {
      super(itemView);
      unbinder = ButterKnife.bind(this, itemView);
    }
  }
}
