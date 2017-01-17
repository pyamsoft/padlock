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

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import com.mikepenz.fastadapter.FastAdapter;
import com.mikepenz.fastadapter.commons.adapters.FastItemAdapter;
import com.pyamsoft.padlock.PadLock;
import com.pyamsoft.padlock.R;
import com.pyamsoft.padlock.databinding.FragmentLockListBinding;
import com.pyamsoft.padlock.lock.PinEntryDialog;
import com.pyamsoft.padlock.main.MainActivity;
import com.pyamsoft.padlock.model.AppEntry;
import com.pyamsoft.padlock.service.PadLockService;
import com.pyamsoft.pydroid.cache.PersistentCache;
import com.pyamsoft.pydroid.design.fab.HideScrollFABBehavior;
import com.pyamsoft.pydroid.design.util.FABUtil;
import com.pyamsoft.pydroid.tool.AsyncDrawable;
import com.pyamsoft.pydroid.tool.AsyncMap;
import com.pyamsoft.pydroid.tool.AsyncMapHelper;
import com.pyamsoft.pydroid.util.AppUtil;
import java.util.List;
import timber.log.Timber;

public class LockListFragment extends Fragment
    implements LockListPresenter.LockList, PinEntryDialogRequest {

  @NonNull public static final String TAG = "LockListFragment";
  @NonNull private static final String PIN_DIALOG_TAG = "pin_dialog";
  @NonNull private static final String KEY_PRESENTER = TAG + "key_presenter";
  @NonNull private final Handler handler = new Handler(Looper.getMainLooper());
  @SuppressWarnings("WeakerAccess") FastItemAdapter<LockListItem> fastItemAdapter;
  @SuppressWarnings("WeakerAccess") LockListPresenter presenter;
  @SuppressWarnings("WeakerAccess") FragmentLockListBinding binding;
  @NonNull private final Runnable startRefreshRunnable = () -> {
    binding.applistSwipeRefresh.post(() -> {
      if (binding != null) {
        if (binding.applistSwipeRefresh != null) {
          binding.applistSwipeRefresh.setRefreshing(true);
        }
      }
    });

    final FragmentActivity activity = getActivity();
    if (activity != null) {
      Timber.d("Reload options");
      activity.supportInvalidateOptionsMenu();
    }
  };
  @NonNull private final Runnable stopRefreshRunnable = () -> {
    binding.applistSwipeRefresh.post(() -> {
      if (binding != null) {
        if (binding.applistSwipeRefresh != null) {
          binding.applistSwipeRefresh.setRefreshing(false);
        }
      }
    });
    final FragmentActivity activity = getActivity();
    if (activity != null) {
      Timber.d("Reload options");
      activity.supportInvalidateOptionsMenu();
    }
  };
  @Nullable private MenuItem displaySystemItem;
  @Nullable private DividerItemDecoration dividerDecoration;
  @Nullable private AsyncMap.Entry fabIconTask;
  private boolean listIsRefreshed;
  private FilterListDelegate filterListDelegate;

  @Override public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setHasOptionsMenu(true);
    presenter = PersistentCache.load(getActivity(), KEY_PRESENTER, new LockListPresenterLoader());
  }

  @Nullable @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    listIsRefreshed = false;
    filterListDelegate = new FilterListDelegate();
    fastItemAdapter = new FastItemAdapter<>();
    binding = FragmentLockListBinding.inflate(inflater, container, false);
    return binding.getRoot();
  }

  @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    filterListDelegate.onViewCreated(fastItemAdapter);
    setupRecyclerView();
    setupSwipeRefresh();
    setupFAB();
  }

  @Override public void onStart() {
    super.onStart();
    presenter.bindView(this);

    presenter.setFABStateFromPreference();
    if (!listIsRefreshed) {
      if (!binding.applistSwipeRefresh.isRefreshing()) {
        binding.applistSwipeRefresh.post(() -> {
          if (binding != null) {
            if (binding.applistSwipeRefresh != null) {
              binding.applistSwipeRefresh.setRefreshing(true);
            }
          }
        });
      }
      presenter.populateList();
    }
  }

  @Override public void onStop() {
    super.onStop();
    presenter.unbindView();
  }

  @Override public void onResume() {
    super.onResume();
    handler.removeCallbacksAndMessages(null);
    handler.postDelayed(() -> binding.applistFab.show(), 300L);

    MainActivity.getNavigationDrawerController(getActivity()).drawerNormalNavigation();
  }

  @Override public void onPause() {
    super.onPause();
    handler.removeCallbacksAndMessages(null);
    handler.postDelayed(() -> binding.applistFab.hide(), 300L);
  }

  private void setupSwipeRefresh() {
    binding.applistSwipeRefresh.setColorSchemeResources(R.color.blue500, R.color.amber700,
        R.color.blue700, R.color.amber500);
    binding.applistSwipeRefresh.setOnRefreshListener(() -> {
      Timber.d("onRefresh");
      refreshList();
    });
  }

  private void setupRecyclerView() {
    dividerDecoration = new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL);

    fastItemAdapter.withOnBindViewHolderListener(new FastAdapter.OnBindViewHolderListener() {

      @CheckResult @NonNull
      private LockListItem.ViewHolder toLockListViewHolder(RecyclerView.ViewHolder viewHolder) {
        if (viewHolder instanceof LockListItem.ViewHolder) {
          return (LockListItem.ViewHolder) viewHolder;
        } else {
          throw new IllegalStateException("ViewHolder is not LockListItem.ViewHolder");
        }
      }

      @Override
      public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int i, List<Object> list) {
        final LockListItem.ViewHolder holder = toLockListViewHolder(viewHolder);
        fastItemAdapter.getAdapterItem(holder.getAdapterPosition()).bindView(holder, list);
        holder.bind(LockListFragment.this::processDatabaseModifyEvent);
      }

      @Override public void unBindViewHolder(RecyclerView.ViewHolder viewHolder, int i) {
        final LockListItem.ViewHolder holder = toLockListViewHolder(viewHolder);
        final LockListItem item = (LockListItem) holder.itemView.getTag();
        if (item != null) {
          item.unbindView(holder);
        }
      }
    });

    fastItemAdapter.withSelectable(true);
    fastItemAdapter.withOnClickListener((view, iAdapter, item, i) -> {
      item.onClick(this::displayLockInfoFragment);
      return true;
    });

    binding.applistRecyclerview.setLayoutManager(new LinearLayoutManager(getContext()));
    binding.applistRecyclerview.addItemDecoration(dividerDecoration);
    binding.applistRecyclerview.setAdapter(fastItemAdapter);
  }

  @Override public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
    super.onCreateOptionsMenu(menu, inflater);
    inflater.inflate(R.menu.locklist_menu, menu);
    inflater.inflate(R.menu.search_menu, menu);
  }

  @Override public void onPrepareOptionsMenu(@NonNull Menu menu) {
    super.onPrepareOptionsMenu(menu);
    setupDisplaySystemVisibleItem(menu);
    filterListDelegate.onPrepareOptionsMenu(menu, fastItemAdapter);
  }

  private void setupDisplaySystemVisibleItem(final @NonNull Menu menu) {
    displaySystemItem = menu.findItem(R.id.menu_is_system);
    presenter.setSystemVisibilityFromPreference();
  }

  @SuppressWarnings("WeakerAccess") void setSystemVisible(boolean visible) {
    if (displaySystemItem == null) {
      throw new IllegalStateException("DisplaySystem menu item is NULL.");
    }

    displaySystemItem.setChecked(visible);
  }

  @Override public void setSystemVisible() {
    setSystemVisible(true);
  }

  @Override public void setSystemInvisible() {
    setSystemVisible(false);
  }

  @Override public void onDestroyView() {
    filterListDelegate.onDestroyView();
    displaySystemItem = null;

    binding.applistRecyclerview.removeItemDecoration(dividerDecoration);
    binding.applistRecyclerview.setOnClickListener(null);
    binding.applistRecyclerview.setLayoutManager(null);
    binding.applistRecyclerview.setAdapter(null);

    binding.applistFab.setOnClickListener(null);
    binding.applistSwipeRefresh.setOnRefreshListener(null);

    AsyncMapHelper.unsubscribe(fabIconTask);
    handler.removeCallbacksAndMessages(null);
    binding.unbind();
    super.onDestroyView();
  }

  @Override public void onDestroy() {
    super.onDestroy();
    PadLock.getRefWatcher(this).watch(this);
  }

  @Override public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.menu_is_system:
        if (binding.applistSwipeRefresh != null && !binding.applistSwipeRefresh.isRefreshing()) {
          Timber.d("List is not refreshing. Allow change of system preference");
          if (item.isChecked()) {
            presenter.setSystemInvisible();
          } else {
            presenter.setSystemVisible();
          }

          refreshList();
        }
        break;
      default:
        Timber.w("Item selected: %d, do nothing", item.getItemId());
    }
    return super.onOptionsItemSelected(item);
  }

  private void setupFAB() {
    binding.applistFab.setOnClickListener(v -> {
      if (PadLockService.isRunning()) {
        presenter.clickPinFABServiceRunning();
      } else {
        presenter.clickPinFABServiceIdle();
      }
    });
    FABUtil.setupFABBehavior(binding.applistFab, new HideScrollFABBehavior(24));
  }

  @Override public void setFABStateEnabled() {
    AsyncMapHelper.unsubscribe(fabIconTask);
    fabIconTask = AsyncDrawable.load(R.drawable.ic_lock_outline_24dp).into(binding.applistFab);
  }

  @Override public void setFABStateDisabled() {
    AsyncMapHelper.unsubscribe(fabIconTask);
    fabIconTask = AsyncDrawable.load(R.drawable.ic_lock_open_24dp).into(binding.applistFab);
  }

  @Override public void onCreateAccessibilityDialog() {
    AppUtil.guaranteeSingleDialogFragment(getFragmentManager(), new AccessibilityRequestDialog(),
        "accessibility");
  }

  @Override public void onCreatePinDialog() {
    onPinEntryDialogRequested(getContext().getPackageName(), getActivity().getClass().getName());
  }

  @Override
  public void onPinEntryDialogRequested(@NonNull String packageName, @NonNull String activityName) {
    AppUtil.guaranteeSingleDialogFragment(getFragmentManager(),
        PinEntryDialog.newInstance(packageName, activityName), PIN_DIALOG_TAG);
  }

  @Override public void onCreateMasterPinSuccess() {
    setFABStateEnabled();
    final View v = getView();
    if (v != null) {
      Snackbar.make(v, "PadLock Enabled", Snackbar.LENGTH_SHORT).show();
    }
  }

  @Override public void onCreateMasterPinFailure() {
    Toast.makeText(getContext(), "Error: Mismatched PIN", Toast.LENGTH_SHORT).show();
  }

  @Override public void onClearMasterPinSuccess() {
    setFABStateDisabled();
    final View v = getView();
    if (v != null) {
      Snackbar.make(v, "PadLock Disabled", Snackbar.LENGTH_SHORT).show();
    }
  }

  @Override public void onClearMasterPinFailure() {
    Toast.makeText(getContext(), "Error: Invalid PIN", Toast.LENGTH_SHORT).show();
  }

  @Override public void onEntryAddedToList(@NonNull AppEntry entry) {
    // In case the configuration changes, we do the animation again
    if (!binding.applistSwipeRefresh.isRefreshing()) {
      binding.applistSwipeRefresh.post(() -> {
        if (binding != null) {
          if (binding.applistSwipeRefresh != null) {
            binding.applistSwipeRefresh.setRefreshing(true);
          }
        }
      });
    }

    fastItemAdapter.add(new LockListItem(entry));
  }

  @Override public void onListPopulateError() {
    Timber.e("onListPopulateError");
    onListPopulated();
    AppUtil.guaranteeSingleDialogFragment(getFragmentManager(), new ErrorDialog(), "error");
  }

  @Override public void showOnBoarding() {
    Timber.d("Show onboarding");
    // TODO
  }

  @Override public void onListCleared() {
    Timber.d("Prepare for refresh");
    listIsRefreshed = false;

    Timber.d("onListCleared");
    handler.removeCallbacksAndMessages(null);
    handler.post(startRefreshRunnable);
    handler.post(() -> binding.applistFab.hide());
  }

  @Override public void onListPopulated() {
    Timber.d("onListPopulated");

    handler.removeCallbacksAndMessages(null);
    handler.post(stopRefreshRunnable);
    handler.post(() -> binding.applistFab.show());

    if (fastItemAdapter.getAdapterItemCount() > 1) {
      Timber.d("We have refreshed");
      listIsRefreshed = true;
      presenter.showOnBoarding();
    } else {
      Toast.makeText(getContext(), "Error while loading list. Please try again.",
          Toast.LENGTH_SHORT).show();
    }
  }

  @Override public void refreshList() {
    fastItemAdapter.clear();
    presenter.clearList();
    onListCleared();
    presenter.populateList();
  }

  @Override public void onDatabaseEntryCreated(int position) {
    onDatabaseUpdated(position, true);
  }

  @Override public void onDatabaseEntryDeleted(int position) {
    onDatabaseUpdated(position, false);
  }

  private void onDatabaseUpdated(int position, boolean locked) {
    final LockListItem oldItem = fastItemAdapter.getItem(position);
    final LockListItem newItem = oldItem.copyWithNewLockState(locked);
    fastItemAdapter.set(position, newItem);
    presenter.updateCachedEntryLockState(newItem.getName(), newItem.getPackageName(), locked);
  }

  @Override public void onDatabaseEntryError(int position) {
    AppUtil.guaranteeSingleDialogFragment(getFragmentManager(), new ErrorDialog(), "error");
  }

  @SuppressWarnings("WeakerAccess") void processDatabaseModifyEvent(boolean lock, int position,
      @NonNull AppEntry entry) {
    Timber.d("Received a database modify event request for %s at %d [%s]", entry.packageName(),
        position, lock ? "LOCK" : "NO LOCK");
    presenter.modifyDatabaseEntry(lock, position, entry.packageName(), null, entry.system());
  }

  @SuppressWarnings("WeakerAccess") void displayLockInfoFragment(@NonNull AppEntry entry) {
    AppUtil.guaranteeSingleDialogFragment(getFragmentManager(), LockInfoDialog.newInstance(entry),
        LockInfoDialog.TAG);
  }
}
