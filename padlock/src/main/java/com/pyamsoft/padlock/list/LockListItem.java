/*
 * Copyright 2017 Peter Kenji Yamanaka
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
import android.widget.CompoundButton;
import com.mikepenz.fastadapter.items.GenericAbstractItem;
import com.pyamsoft.padlock.Injector;
import com.pyamsoft.padlock.R;
import com.pyamsoft.padlock.databinding.AdapterItemLocklistBinding;
import com.pyamsoft.padlock.uicommon.AppIconLoader;
import com.pyamsoft.padlock.model.AppEntry;
import com.pyamsoft.pydroid.loader.ImageLoader;
import com.pyamsoft.pydroid.loader.LoaderMap;
import com.pyamsoft.pydroid.loader.loaded.Loaded;
import com.pyamsoft.pydroid.ui.util.DialogUtil;
import java.util.List;
import javax.inject.Inject;
import timber.log.Timber;

public class LockListItem
    extends GenericAbstractItem<AppEntry, LockListItem, LockListItem.ViewHolder>
    implements FilterableItem<LockListItem, LockListItem.ViewHolder> {

  @NonNull private final LoaderMap loaderMap = new LoaderMap();
  @SuppressWarnings("WeakerAccess") @Inject LockListItemPresenter presenter;

  LockListItem(@NonNull AppEntry entry) {
    super(entry);
    Injector.get().provideComponent().inject(this);
  }

  @Override public int getType() {
    return R.id.adapter_lock_item;
  }

  @Override public int getLayoutRes() {
    return R.layout.adapter_item_locklist;
  }

  @Override public ViewHolder getViewHolder(View view) {
    return new ViewHolder(view);
  }

  @Override public boolean filterAgainst(@NonNull String query) {
    final String name = getModel().name().toLowerCase().trim();
    Timber.d("Filter predicate: '%s' against %s", query, name);
    return !name.startsWith(query);
  }

  @Override public void bindView(ViewHolder holder, List<Object> payloads) {
    super.bindView(holder, payloads);
    holder.binding.lockListTitle.setText(getModel().name());
    holder.binding.lockListToggle.setOnCheckedChangeListener(null);
    holder.binding.lockListToggle.setChecked(getModel().locked());

    Loaded appIcon = ImageLoader.fromLoader(AppIconLoader.forPackageName(getModel().packageName()))
        .into(holder.binding.lockListIcon);
    loaderMap.put("locked", appIcon);

    holder.binding.lockListToggle.setOnCheckedChangeListener(
        new CompoundButton.OnCheckedChangeListener() {
          @Override public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            buttonView.setOnCheckedChangeListener(null);
            buttonView.setChecked(!isChecked);

            final CompoundButton.OnCheckedChangeListener listener = this;
            presenter.modifyDatabaseEntry(isChecked, getModel().packageName(), null,
                getModel().system(), new LockListItemPresenter.DatabaseCallback() {

                  @Override public void onDatabaseEntryError() {
                    DialogUtil.onlyLoadOnceDialogFragment(
                        (FragmentActivity) holder.itemView.getContext(), new ErrorDialog(),
                        "error");
                  }

                  private void updateModel(boolean locked) {
                    withModel(getModel().toBuilder().locked(locked).build());
                  }

                  @Override public void onDatabaseEntryCreated() {
                    updateModel(true);
                    buttonView.setChecked(true);
                    buttonView.setOnCheckedChangeListener(listener);
                  }

                  @Override public void onDatabaseEntryDeleted() {
                    updateModel(false);
                    buttonView.setChecked(false);
                    buttonView.setOnCheckedChangeListener(listener);
                  }
                });
          }
        });
  }

  @Override public void unbindView(ViewHolder holder) {
    super.unbindView(holder);
    holder.binding.lockListTitle.setText(null);
    holder.binding.lockListIcon.setImageDrawable(null);
    holder.binding.lockListToggle.setOnCheckedChangeListener(null);

    presenter.stop();
    presenter.destroy();

    loaderMap.clear();
  }

  static final class ViewHolder extends RecyclerView.ViewHolder {

    @NonNull final AdapterItemLocklistBinding binding;

    ViewHolder(View itemView) {
      super(itemView);
      binding = DataBindingUtil.bind(itemView);
    }
  }
}
