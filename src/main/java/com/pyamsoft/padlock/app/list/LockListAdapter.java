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
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.pyamsoft.padlock.PadLock;
import com.pyamsoft.padlock.R;
import com.pyamsoft.padlock.app.BaseRecyclerAdapter;
import com.pyamsoft.padlock.app.db.DBPresenter;
import com.pyamsoft.padlock.app.db.DBProgressDialog;
import com.pyamsoft.padlock.app.db.DBView;
import com.pyamsoft.padlock.app.list.info.LockInfoDialog;
import com.pyamsoft.padlock.dagger.db.DBModule;
import com.pyamsoft.padlock.dagger.db.DaggerDBComponent;
import com.pyamsoft.padlock.model.AppEntry;
import com.pyamsoft.pydroid.util.AppUtil;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import timber.log.Timber;

public final class LockListAdapter extends BaseRecyclerAdapter<LockListAdapter.ViewHolder>
    implements LockListItem, DBView {

  private static final String DB_PROGRESS_TAG = "db_progress";
  @NonNull private final List<AppEntry> appEntryList;
  @Inject DBPresenter dbPresenter;
  private WeakReference<AppCompatActivity> weakActivity;

  public LockListAdapter() {
    appEntryList = new ArrayList<>();
  }

  public final void bind(LockListFragment fragment) {
    weakActivity = new WeakReference<>((AppCompatActivity) fragment.getActivity());
    DaggerDBComponent.builder()
        .padLockComponent(PadLock.padLockComponent(fragment))
        .dBModule(new DBModule())
        .build()
        .inject(this);
    dbPresenter.bind(this);
  }

  public final void unbind() {
    if (weakActivity != null) {
      weakActivity.clear();
    }
    dbPresenter.unbind();
  }

  @Override public void onDestroy() {
    super.onDestroy();
    appEntryList.clear();
  }

  @Override public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    final View view = LayoutInflater.from(parent.getContext())
        .inflate(R.layout.adapter_item_locklist_entry, parent, false);
    return new ViewHolder(view);
  }

  @Override public void onViewRecycled(ViewHolder holder) {
    super.onViewRecycled(holder);
    removeViewActionListeners(holder);
  }

  private void removeViewActionListeners(ViewHolder holder) {
    holder.toggle.setOnCheckedChangeListener(null);
    holder.name.setOnClickListener(null);
    holder.icon.setOnClickListener(null);
  }

  @Override public void onBindViewHolder(ViewHolder holder, int position) {
    Timber.d("onBindViewHolder: %s", position);
    final AppEntry entry = appEntryList.get(position);
    removeViewActionListeners(holder);
    holder.name.setText(entry.name());
    holder.icon.setImageBitmap(entry.icon());

    holder.toggle.setChecked(entry.locked());
    final CompoundButton.OnCheckedChangeListener listener =
        new CompoundButton.OnCheckedChangeListener() {
          @Override public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
            // Don't check it yet, get auth first
            holder.toggle.setOnCheckedChangeListener(null);
            holder.toggle.setChecked(!b);
            holder.toggle.setOnCheckedChangeListener(this);

            // Authorize for package access
            authorizeAccess(holder, true, b);
          }
        };

    holder.toggle.setOnCheckedChangeListener(listener);
    holder.name.setOnClickListener(view -> authorizeAccess(holder, false, false));
    holder.icon.setOnClickListener(view -> authorizeAccess(holder, false, false));
  }

  private void accessPackage(final AppEntry entry, final int position, final boolean checked) {
    // TODO app specific codes
    final AppCompatActivity appCompatActivity = weakActivity.get();
    if (appCompatActivity != null) {
      AppUtil.guaranteeSingleDialogFragment(appCompatActivity.getSupportFragmentManager(),
          DBProgressDialog.newInstance(entry.name()), DB_PROGRESS_TAG);
    }
    dbPresenter.attemptDBModification(position, checked, entry.packageName(), entry.name(), null,
        entry.system());
  }

  private void authorizeAccess(final ViewHolder holder, final boolean accessPackage,
      final boolean checked) {
    // TODO some kind of observable which can confirm correct passcode entry

    Timber.d("Access authorized");
    final int position = holder.getAdapterPosition();
    final AppEntry entry = appEntryList.get(position);
    if (accessPackage) {
      Timber.d("Access package");
      accessPackage(entry, position, checked);
    } else {
      Timber.d("Access activities");
      openInfo(entry);
    }
  }

  private void openInfo(AppEntry entry) {
    // TODO add optional auth check

    final AppCompatActivity appCompatActivity = weakActivity.get();
    if (appCompatActivity != null) {
      AppUtil.guaranteeSingleDialogFragment(appCompatActivity.getSupportFragmentManager(),
          LockInfoDialog.newInstance(entry), "lock_info");
    }
  }

  @Override public int getItemCount() {
    return appEntryList.size();
  }

  @Override public void onListItemAdded(int position) {
    Timber.d("item inserted: %d", position);
    notifyItemInserted(position);
  }

  @Override public void onListItemRemoved(int position) {
    Timber.d("item removed: %d", position);
    notifyItemRemoved(position);
  }

  @Override public void addItem(AppEntry entry) {
    final int next = appEntryList.size();
    appEntryList.add(next, entry);
    notifyItemInserted(next);
  }

  @Override public void removeItem() {
    final int old = appEntryList.size() - 1;
    appEntryList.remove(old);
    notifyItemRemoved(old);
  }

  @Override public void onDBCreateEvent(int position) {
    Timber.d("onDBCreateEvent");
    appEntryList.set(position, AppEntry.builder(appEntryList.get(position)).locked(true).build());
    notifyItemChanged(position);

    final AppCompatActivity appCompatActivity = weakActivity.get();
    if (appCompatActivity == null) {
      throw new NullPointerException("Activity is NULL, cannot dismiss DBProgressDialog");
    }

    DBProgressDialog.remove(appCompatActivity.getSupportFragmentManager(), DB_PROGRESS_TAG);
  }

  @Override public void onDBDeleteEvent(int position) {
    Timber.d("onDBDeleteEvent");
    appEntryList.set(position, AppEntry.builder(appEntryList.get(position)).locked(false).build());
    notifyItemChanged(position);

    final AppCompatActivity appCompatActivity = weakActivity.get();
    if (appCompatActivity == null) {
      throw new NullPointerException("Activity is NULL, cannot dismiss DBProgressDialog");
    }

    DBProgressDialog.remove(appCompatActivity.getSupportFragmentManager(), DB_PROGRESS_TAG);
  }

  @Override public void onDBError() {
    Timber.e("onDBError");
    final AppCompatActivity appCompatActivity = weakActivity.get();
    if (appCompatActivity == null) {
      throw new NullPointerException("Activity is NULL, cannot dismiss DBProgressDialog");
    }

    DBProgressDialog.remove(appCompatActivity.getSupportFragmentManager(), DB_PROGRESS_TAG);
    // TODO handle exception, show error dialog or something
  }

  static final class ViewHolder extends RecyclerView.ViewHolder {

    @BindView(R.id.lock_list_title) TextView name;
    @BindView(R.id.lock_list_icon) ImageView icon;
    @BindView(R.id.lock_list_toggle) SwitchCompat toggle;

    public ViewHolder(final View itemView) {
      super(itemView);
      ButterKnife.bind(this, itemView);
    }
  }
}
