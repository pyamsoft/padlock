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
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
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
import com.pyamsoft.padlock.R;
import com.pyamsoft.padlock.app.base.ErrorDialog;
import com.pyamsoft.padlock.app.db.DBPresenter;
import com.pyamsoft.padlock.app.db.DBProgressDialog;
import com.pyamsoft.padlock.model.AppEntry;
import com.pyamsoft.pydroid.util.AppUtil;
import timber.log.Timber;

public final class LockListAdapter extends BaseRecyclerAdapter<LockListAdapter.ViewHolder>
    implements LockListItem<AppEntry>, DBPresenter.DBView,
    AdapterPresenter.AdapterView<LockListAdapter.ViewHolder> {

  private static final long DB_PROGRESS_DELAY = 500L;
  @NonNull private final AdapterPresenter<AppEntry, ViewHolder> adapterPresenter;
  @NonNull private final Handler handler = new Handler(Looper.getMainLooper());
  @NonNull private final Fragment fragment;
  @NonNull private final DBPresenter dbPresenter;

  public LockListAdapter(@NonNull LockListFragment fragment,
      @NonNull AdapterPresenter<AppEntry, ViewHolder> adapterPresenter,
      @NonNull DBPresenter dbPresenter) {
    this.fragment = fragment;
    this.dbPresenter = dbPresenter;
    this.adapterPresenter = adapterPresenter;
  }

  @Override public void onCreate() {
    super.onCreate();
    adapterPresenter.bindView(this);
    dbPresenter.bindView(this);
  }

  @Override public void onDestroy() {
    super.onDestroy();
    handler.removeCallbacksAndMessages(null);
    adapterPresenter.unbindView();
    dbPresenter.unbindView();
  }

  @Override public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    final View view = LayoutInflater.from(parent.getContext())
        .inflate(R.layout.adapter_item_locklist_entry, parent, false);
    return new ViewHolder(view);
  }

  @Override public void onViewRecycled(@NonNull ViewHolder holder) {
    super.onViewRecycled(holder);
    removeViewActionListeners(holder);
  }

  private void removeViewActionListeners(@NonNull ViewHolder holder) {
    holder.toggle.setOnCheckedChangeListener(null);
    holder.name.setOnClickListener(null);
    holder.icon.setOnClickListener(null);
  }

  @Override public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
    Timber.d("onBindViewHolder: %s", position);
    final AppEntry entry = adapterPresenter.get(position);
    removeViewActionListeners(holder);
    holder.name.setText(entry.name());

    adapterPresenter.loadApplicationIcon(holder, entry.packageName());

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
            authorizeAccess(holder, true, b);
          }
        };

    holder.toggle.setOnCheckedChangeListener(listener);
    holder.name.setOnClickListener(view -> authorizeAccess(holder, false, false));
    holder.icon.setOnClickListener(view -> authorizeAccess(holder, false, false));
  }

  private void accessPackage(final @NonNull AppEntry entry, final int position,
      final boolean checked) {
    // TODO app specific codes
    DBProgressDialog.add(fragment.getFragmentManager(), entry.name());

    dbPresenter.attemptDBModification(position, checked, entry.packageName(), null, entry.system());
  }

  private void authorizeAccess(final @NonNull ViewHolder holder, final boolean accessPackage,
      final boolean checked) {
    // TODO some kind of observable which can confirm correct passcode entry

    Timber.d("Access authorized");
    final int position = holder.getAdapterPosition();
    final AppEntry entry = adapterPresenter.get(position);
    if (accessPackage) {
      Timber.d("Access package");
      accessPackage(entry, position, checked);
    } else {
      Timber.d("Access activities");
      openInfo(entry);
    }
  }

  private void openInfo(@NonNull AppEntry entry) {
    AppUtil.guaranteeSingleDialogFragment(fragment.getFragmentManager(),
        LockInfoDialog.newInstance(entry), "lock_info");
  }

  @Override public int getItemCount() {
    return adapterPresenter.size();
  }

  @Override public void addItem(@NonNull AppEntry entry) {
    final int next = adapterPresenter.add(entry);
    notifyItemInserted(next);
  }

  @Override public void removeItem() {
    final int old = adapterPresenter.remove();
    notifyItemRemoved(old);
  }

  @Override public void onDBCreateEvent(int position) {
    Timber.d("onDBCreateEvent");
    adapterPresenter.setLocked(position, true);
    notifyItemChanged(position);
    handler.postDelayed(() -> DBProgressDialog.remove(fragment.getFragmentManager()),
        DB_PROGRESS_DELAY);
  }

  @Override public void onDBDeleteEvent(int position) {
    Timber.d("onDBDeleteEvent");
    adapterPresenter.setLocked(position, false);
    notifyItemChanged(position);
    handler.postDelayed(() -> DBProgressDialog.remove(fragment.getFragmentManager()),
        DB_PROGRESS_DELAY);
  }

  @Override public void onDBError() {
    Timber.e("onDBError");
    handler.postDelayed(() -> DBProgressDialog.remove(fragment.getFragmentManager()),
        DB_PROGRESS_DELAY);
  }

  @Override public void onApplicationIconLoadedSuccess(@NonNull ViewHolder holder,
      @NonNull Drawable drawable) {
    holder.icon.setImageDrawable(drawable);
  }

  @Override public void onApplicationIconLoadedError(@NonNull ViewHolder holder) {
    AppUtil.guaranteeSingleDialogFragment(fragment.getFragmentManager(), new ErrorDialog(),
        "error");
  }

  public static final class ViewHolder extends RecyclerView.ViewHolder {

    @BindView(R.id.lock_list_title) TextView name;
    @BindView(R.id.lock_list_icon) ImageView icon;
    @BindView(R.id.lock_list_toggle) SwitchCompat toggle;

    public ViewHolder(final @NonNull View itemView) {
      super(itemView);
      ButterKnife.bind(this, itemView);
    }
  }
}
