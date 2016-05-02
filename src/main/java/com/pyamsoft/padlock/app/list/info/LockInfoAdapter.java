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

package com.pyamsoft.padlock.app.list.info;

import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckedTextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.pyamsoft.padlock.PadLock;
import com.pyamsoft.padlock.R;
import com.pyamsoft.padlock.app.BaseRecyclerAdapter;
import com.pyamsoft.padlock.app.db.DBPresenter;
import com.pyamsoft.padlock.app.db.DBView;
import com.pyamsoft.padlock.dagger.db.DBModule;
import com.pyamsoft.padlock.dagger.db.DaggerDBComponent;
import com.pyamsoft.padlock.model.ActivityEntry;
import com.pyamsoft.padlock.model.AppEntry;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import timber.log.Timber;

public final class LockInfoAdapter extends BaseRecyclerAdapter<LockInfoAdapter.ViewHolder>
    implements LockInfoItem, DBView {

  @NonNull private final List<ActivityEntry> activityEntryList;
  @Inject DBPresenter dbPresenter;

  private WeakReference<FragmentActivity> weakActivity;
  private WeakReference<AppEntry> weakEntry;

  public LockInfoAdapter() {
    activityEntryList = new ArrayList<>();
  }

  public void bind(@NonNull AppEntry appEntry, @NonNull FragmentActivity activity) {
    this.weakEntry = new WeakReference<>(appEntry);
    this.weakActivity = new WeakReference<>(activity);

    DaggerDBComponent.builder()
        .padLockComponent(PadLock.padLockComponent(activity))
        .dBModule(new DBModule())
        .build()
        .inject(this);
    dbPresenter.create();
    dbPresenter.bind(this);
  }

  public final void unbind() {
    if (weakEntry != null) {
      weakEntry.clear();
    }

    if (weakActivity != null) {
      weakActivity.clear();
    }

    dbPresenter.unbind();
    dbPresenter.destroy();
  }

  @Override public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    final View view = LayoutInflater.from(parent.getContext())
        .inflate(R.layout.adapter_item_lockinfo, parent, false);
    return new ViewHolder(view);
  }

  @Override public void onBindViewHolder(ViewHolder holder, int position) {
    Timber.d("onBindViewHolder: %d", position);
    final ActivityEntry entry = activityEntryList.get(position);

    removeViewActionListeners(holder);
    holder.checkBox.setChecked(entry.locked());

    final AppEntry appEntry = weakEntry.get();
    if (appEntry == null) {
      throw new NullPointerException("Cannot bind with NULL app entry");
    }

    String name;
    final String entryName = entry.name();
    if (entryName.startsWith(appEntry.packageName())) {
      name = entryName.replace(appEntry.packageName(), "");
    } else {
      name = entryName;
    }
    holder.checkBox.setText(name);

    holder.checkBox.setOnClickListener(
        view -> dbPresenter.attemptDBModification(position, !holder.checkBox.isChecked(),
            appEntry.packageName(), entry.name(), appEntry.name(), null, appEntry.system()));
  }

  @Override public void onViewRecycled(ViewHolder holder) {
    super.onViewRecycled(holder);
    removeViewActionListeners(holder);
  }

  private void removeViewActionListeners(ViewHolder holder) {
    holder.checkBox.setOnClickListener(null);
  }

  @Override public int getItemCount() {
    return activityEntryList.size();
  }

  @Override public void addItem(ActivityEntry entry) {
    final int next = activityEntryList.size();
    activityEntryList.add(next, entry);
    notifyItemInserted(next);
  }

  @Override public void removeItem() {
    final int old = activityEntryList.size() - 1;
    activityEntryList.remove(old);
    notifyItemRemoved(old);
  }

  @Override public void onDBCreateEvent(int position) {
    Timber.d("onDBCreateEvent");
    activityEntryList.set(position,
        ActivityEntry.builder(activityEntryList.get(position)).locked(true).build());
    notifyItemChanged(position);
  }

  @Override public void onDBDeleteEvent(int position) {
    Timber.d("onDBDeleteEvent");
    activityEntryList.set(position,
        ActivityEntry.builder(activityEntryList.get(position)).locked(false).build());
    notifyItemChanged(position);
  }

  @Override public void onDBError() {
    Timber.e("onDBError");
    // TODO handle exception, show error dialog or something
  }

  static final class ViewHolder extends RecyclerView.ViewHolder {

    @BindView(R.id.lock_info_activity_locked) CheckedTextView checkBox;

    public ViewHolder(View itemView) {
      super(itemView);
      ButterKnife.bind(this, itemView);
    }
  }
}
