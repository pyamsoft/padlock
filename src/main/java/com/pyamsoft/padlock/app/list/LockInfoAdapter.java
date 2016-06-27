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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckedTextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.pyamsoft.padlock.R;
import com.pyamsoft.padlock.app.db.DBPresenter;
import com.pyamsoft.padlock.model.ActivityEntry;
import com.pyamsoft.padlock.model.AppEntry;
import timber.log.Timber;

public final class LockInfoAdapter extends BaseRecyclerAdapter<LockInfoAdapter.ViewHolder>
    implements LockListItem<ActivityEntry>, DBPresenter.DBView,
    AdapterPresenter.AdapterView<LockInfoAdapter.ViewHolder> {

  @NonNull private final DBPresenter.DBView lockInfoDialog;
  @NonNull private final AdapterPresenter<ActivityEntry, ViewHolder> adapterPresenter;
  @NonNull private final DBPresenter dbPresenter;
  @NonNull private final AppEntry appEntry;

  public LockInfoAdapter(@NonNull DBPresenter.DBView lockInfoDialog, @NonNull AppEntry appEntry,
      @NonNull AdapterPresenter<ActivityEntry, ViewHolder> adapterPresenter,
      @NonNull DBPresenter dbPresenter) {
    this.lockInfoDialog = lockInfoDialog;
    this.appEntry = appEntry;
    this.adapterPresenter = adapterPresenter;
    this.dbPresenter = dbPresenter;
  }

  @Override public void onCreate() {
    super.onCreate();
    dbPresenter.bindView(this);
    adapterPresenter.bindView(this);
  }

  @Override public void onDestroy() {
    super.onDestroy();

    adapterPresenter.unbindView();
    dbPresenter.unbindView();
  }

  @Override public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    final View view = LayoutInflater.from(parent.getContext())
        .inflate(R.layout.adapter_item_lockinfo, parent, false);
    return new ViewHolder(view);
  }

  @Override public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
    Timber.d("onBindViewHolder: %d", position);
    final ActivityEntry entry = adapterPresenter.get(position);

    removeViewActionListeners(holder);
    holder.checkBox.setChecked(entry.locked());

    String activityName;
    final String entryName = entry.name();
    if (entryName.startsWith(appEntry.packageName())) {
      activityName = entryName.replace(appEntry.packageName(), "");
    } else {
      activityName = entryName;
    }
    holder.checkBox.setText(activityName);

    holder.checkBox.setOnClickListener(
        view -> dbPresenter.attemptDBModification(position, !holder.checkBox.isChecked(),
            appEntry.packageName(), entryName, null, appEntry.system()));
  }

  @Override public void onViewRecycled(@NonNull ViewHolder holder) {
    super.onViewRecycled(holder);
    removeViewActionListeners(holder);
  }

  private void removeViewActionListeners(@NonNull ViewHolder holder) {
    holder.checkBox.setOnClickListener(null);
  }

  @Override public int getItemCount() {
    return adapterPresenter.size();
  }

  @Override public void addItem(@NonNull ActivityEntry entry) {
    final int next = adapterPresenter.add(entry);
    notifyItemInserted(next);
  }

  @Override public void removeItem() {
    final int old = adapterPresenter.remove();
    notifyItemRemoved(old);
  }

  @Override public void onDBCreateEvent(int position) {
    if (position < 0) {
      Timber.d("Pass create event to dialog");
      lockInfoDialog.onDBCreateEvent(position);
    } else {
      Timber.d("onDBCreateEvent");
      adapterPresenter.setLocked(position, true);
      notifyItemChanged(position);
    }
  }

  @Override public void onDBDeleteEvent(int position) {
    if (position < 0) {
      Timber.d("Pass delete event to dialog");
      lockInfoDialog.onDBDeleteEvent(position);
    } else {
      Timber.d("onDBDeleteEvent");
      adapterPresenter.setLocked(position, false);
      notifyItemChanged(position);
    }
  }

  @Override public void onDBError() {
    // TODO handle exception, show error dialog or something
  }

  public static final class ViewHolder extends RecyclerView.ViewHolder {

    @BindView(R.id.lock_info_activity_locked) CheckedTextView checkBox;

    public ViewHolder(@NonNull View itemView) {
      super(itemView);
      ButterKnife.bind(this, itemView);
    }
  }
}
