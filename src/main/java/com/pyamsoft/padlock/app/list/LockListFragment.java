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

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import com.pyamsoft.padlock.PadLock;
import com.pyamsoft.padlock.R;
import com.pyamsoft.padlock.app.db.DBPresenter;
import com.pyamsoft.padlock.app.main.MainActivity;
import com.pyamsoft.padlock.app.pinentry.MasterPinSubmitCallback;
import com.pyamsoft.padlock.app.pinentry.PinEntryDialog;
import com.pyamsoft.padlock.app.settings.SettingsFragment;
import com.pyamsoft.padlock.dagger.list.DaggerLockListComponent;
import com.pyamsoft.padlock.model.AppEntry;
import com.pyamsoft.pydroid.base.Presenter;
import com.pyamsoft.pydroid.behavior.HideScrollFABBehavior;
import com.pyamsoft.pydroid.model.AsyncDrawable;
import com.pyamsoft.pydroid.tool.AsyncVectorDrawableTask;
import com.pyamsoft.pydroid.tool.DataHolderFragment;
import com.pyamsoft.pydroid.tool.DividerItemDecoration;
import com.pyamsoft.pydroid.util.AnimUtil;
import com.pyamsoft.pydroid.util.AppUtil;
import javax.inject.Inject;
import timber.log.Timber;
import uk.co.deanwild.materialshowcaseview.IShowcaseListener;
import uk.co.deanwild.materialshowcaseview.MaterialShowcaseView;

public final class LockListFragment extends Fragment
    implements LockListPresenter.LockList, PinEntryDialogRequest, MasterPinSubmitCallback {

  @NonNull private static final String PIN_DIALOG_TAG = "pin_dialog";
  private static final int KEY_PRESENTER = 0;
  private static final int KEY_ADAPTER_PRESENTER = 1;
  private static final int KEY_DB_PRESENTER = 2;
  @Nullable @BindView(R.id.applist_fab) FloatingActionButton fab;
  @Nullable @BindView(R.id.applist_recyclerview) RecyclerView recyclerView;
  @Nullable @BindView(R.id.applist_swipe_refresh) SwipeRefreshLayout swipeRefreshLayout;
  @Nullable @Inject LockListPresenter presenter;
  @Nullable @Inject AdapterPresenter<AppEntry> adapterPresenter;
  @Nullable @Inject DBPresenter dbPresenter;
  @Nullable private DataHolderFragment<Presenter> presenterDataHolder;
  @Nullable private LockListAdapter adapter;
  @Nullable private LockListLayoutManager lockListLayoutManager;
  @NonNull private final Handler handler = new Handler();
  @NonNull private final Runnable startRefreshRunnable = new Runnable() {
    @Override public void run() {
      if (lockListLayoutManager == null || swipeRefreshLayout == null) {
        throw new NullPointerException("UI component is NULL");
      }
      swipeRefreshLayout.setRefreshing(true);
      lockListLayoutManager.setVerticalScrollEnabled(false);
      final FragmentActivity activity = getActivity();
      if (activity != null) {
        Timber.d("Reload options");
        activity.supportInvalidateOptionsMenu();
      }
    }
  };
  @NonNull private final Runnable stopRefreshRunnable = new Runnable() {
    @Override public void run() {
      if (lockListLayoutManager == null || swipeRefreshLayout == null) {
        throw new NullPointerException("UI component is NULL");
      }
      swipeRefreshLayout.setRefreshing(false);
      lockListLayoutManager.setVerticalScrollEnabled(true);
      final FragmentActivity activity = getActivity();
      if (activity != null) {
        Timber.d("Reload options");
        activity.supportInvalidateOptionsMenu();
      }
    }
  };
  @Nullable private AsyncVectorDrawableTask fabIconTask;
  @Nullable private Unbinder unbinder;
  @Nullable private MenuItem displaySystemItem;
  private boolean firstRefresh;

  @Nullable @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    final View view = inflater.inflate(R.layout.fragment_applist, container, false);
    unbinder = ButterKnife.bind(this, view);
    if (presenter == null) {
      throw new NullPointerException("Presenter is NULL");
    }
    presenter.onCreateView(this);

    if (adapter == null) {
      throw new NullPointerException("Adapter is NULL");
    }
    adapter.onCreate();
    return view;
  }

  @SuppressLint("ShowToast") @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    setupRecyclerView();
    setupSwipeRefresh();
    setupFAB();

    if (firstRefresh) {
      Timber.d("Do initial refresh");
      firstRefresh = false;
      refreshList();
    }
  }

  private void setupSwipeRefresh() {
    if (swipeRefreshLayout == null) {
      throw new NullPointerException("SwipeRefreshLayout is NULL");
    }
    swipeRefreshLayout.setColorSchemeResources(R.color.blue500, R.color.amber700, R.color.blue700,
        R.color.amber500);
    swipeRefreshLayout.setOnRefreshListener(() -> {
      Timber.d("onRefresh");
      refreshList();
    });
  }

  @Override public void onResume() {
    super.onResume();
    AnimUtil.popShow(fab, 500, 300);
    if (presenter == null) {
      throw new NullPointerException("Presenter is NULL");
    }
    presenter.onResume();
  }

  @Override public void onPause() {
    super.onPause();
    AnimUtil.popHide(fab, 300, 300);
    if (presenter == null) {
      throw new NullPointerException("Presenter is NULL");
    }
    presenter.onPause();
  }

  private void setupRecyclerView() {
    lockListLayoutManager = new LockListLayoutManager(getContext());
    lockListLayoutManager.setVerticalScrollEnabled(true);
    final RecyclerView.ItemDecoration dividerDecoration =
        new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL_LIST);
    if (recyclerView == null) {
      throw new NullPointerException("RecyclerView is NULL");
    }
    recyclerView.setLayoutManager(lockListLayoutManager);
    recyclerView.setAdapter(adapter);
    recyclerView.addItemDecoration(dividerDecoration);
  }

  @Override public void onCreate(@Nullable Bundle savedInstanceState) {
    Timber.d("onCreate");
    super.onCreate(savedInstanceState);

    presenterDataHolder = DataHolderFragment.getInstance(getActivity(), "lock_list_presenters");

    final LockListPresenter lockListPresenter =
        (LockListPresenter) presenterDataHolder.pop(KEY_PRESENTER);
    @SuppressWarnings("unchecked") final AdapterPresenter<AppEntry> entryAdapterPresenter =
        (AdapterPresenter<AppEntry>) presenterDataHolder.pop(KEY_ADAPTER_PRESENTER);
    final DBPresenter lockDBPresenter = (DBPresenter) presenterDataHolder.pop(KEY_DB_PRESENTER);
    if (lockListPresenter == null || entryAdapterPresenter == null || lockDBPresenter == null) {
      Timber.d("Create new presenters");
      firstRefresh = true;
      DaggerLockListComponent.builder()
          .padLockComponent(PadLock.padLockComponent(this))
          .build()
          .inject(this);
    } else {
      Timber.d("Load cached presenters");
      firstRefresh = false;
      presenter = lockListPresenter;
      adapterPresenter = entryAdapterPresenter;
      dbPresenter = lockDBPresenter;
    }

    if (adapterPresenter == null || dbPresenter == null) {
      throw new NullPointerException("Required presenter is NULL");
    }
    adapter = new LockListAdapter(this, adapterPresenter, dbPresenter);

    setHasOptionsMenu(true);
  }

  @Override public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
    Timber.d("onCreateOptionsMenu");
    super.onCreateOptionsMenu(menu, inflater);
    inflater.inflate(R.menu.locklist_menu, menu);
  }

  @Override public void onPrepareOptionsMenu(@NonNull Menu menu) {
    super.onPrepareOptionsMenu(menu);
    setupLockListMenuItems(menu);
  }

  private void setupLockListMenuItems(final @NonNull Menu menu) {
    displaySystemItem = menu.findItem(R.id.menu_is_system);
    if (displaySystemItem == null) {
      Timber.e("Item is NULL");
      return;
    }
    if (presenter == null) {
      throw new NullPointerException("Presenter is NULL");
    }
    presenter.setSystemVisibilityFromPreference();
  }

  private void setSystemCheckListener() {
    if (presenter == null) {
      throw new NullPointerException("Presenter is NULL");
    }
    if (displaySystemItem != null) {
      displaySystemItem.setOnMenuItemClickListener(item -> {
        if (item.isChecked()) {
          presenter.setSystemInvisible();
        } else {
          presenter.setSystemVisible();
        }
        refreshList();
        return true;
      });
    }
  }

  private void setSystemVisible(boolean visible) {
    if (displaySystemItem != null) {
      displaySystemItem.setOnMenuItemClickListener(null);
      displaySystemItem.setChecked(visible);
      setSystemCheckListener();
    }
  }

  @Override public boolean onOptionsItemSelected(MenuItem item) {
    boolean handled;
    switch (item.getItemId()) {
      case R.id.menu_settings:
        handled = true;
        showSettingsScreen();
        break;
      default:
        handled = false;
    }
    return handled || super.onOptionsItemSelected(item);
  }

  private void showSettingsScreen() {
    final FragmentManager fragmentManager = getFragmentManager();
    if (fragmentManager.findFragmentByTag(MainActivity.SETTINGS_TAG) == null) {
      fragmentManager.beginTransaction()
          .replace(R.id.main_view_container, new SettingsFragment())
          .addToBackStack(null)
          .commit();
      final FragmentActivity activity = getActivity();
      if (activity instanceof MainActivity) {
        final MainActivity mainActivity = (MainActivity) activity;
        mainActivity.setActionBarUpEnabled(true);
      } else {
        throw new ClassCastException("Activity is not MainActivity");
      }
    }
  }

  @Override public void setSystemVisible() {
    setSystemVisible(true);
  }

  @Override public void setSystemInvisible() {
    setSystemVisible(false);
  }

  @Override public void onDestroyView() {
    super.onDestroyView();

    if (recyclerView != null) {
      recyclerView.setOnClickListener(null);
      recyclerView.setLayoutManager(null);
      recyclerView.setAdapter(null);
    }

    if (fab != null) {
      AppUtil.nullifyCallback(fab);
      fab.setOnClickListener(null);
    }

    if (swipeRefreshLayout != null) {
      swipeRefreshLayout.setOnRefreshListener(null);
    }

    if (adapter != null) {
      adapter.onDestroy();
    }
    if (presenter != null && !getActivity().isChangingConfigurations()) {
      presenter.onDestroyView();
    }

    cancelFabTask();

    if (unbinder != null) {
      unbinder.unbind();
    }

    handler.removeCallbacksAndMessages(null);
  }

  private void cancelFabTask() {
    if (fabIconTask != null) {
      if (!fabIconTask.isCancelled()) {
        fabIconTask.cancel(true);
      }
      fabIconTask = null;
    }
  }

  private void setupFAB() {
    if (presenter == null) {
      throw new NullPointerException("Presenter is NULL");
    }
    if (fab != null) {
      fab.setOnClickListener(view -> presenter.clickPinFAB());
      AppUtil.setupFABBehavior(fab, new HideScrollFABBehavior(24));
      presenter.setFABStateFromPreference();
    }
  }

  @Override public void setFABStateEnabled() {
    if (fab == null) {
      throw new NullPointerException("FloatingActionButton is NULL");
    }
    cancelFabTask();
    fabIconTask = new AsyncVectorDrawableTask(fab);
    fabIconTask.execute(
        new AsyncDrawable(getContext().getApplicationContext(), R.drawable.ic_lock_outline_24dp));
  }

  @Override public void setFABStateDisabled() {
    if (fab == null) {
      throw new NullPointerException("FloatingActionButton is NULL");
    }
    cancelFabTask();
    fabIconTask = new AsyncVectorDrawableTask(fab);
    fabIconTask.execute(
        new AsyncDrawable(getContext().getApplicationContext(), R.drawable.ic_lock_open_24dp));
  }

  @Override
  public void onPinEntryDialogRequested(@NonNull String packageName, @NonNull String activityName) {
    AppUtil.guaranteeSingleDialogFragment(getFragmentManager(),
        PinEntryDialog.newInstance(packageName, activityName), PIN_DIALOG_TAG);
  }

  @Override public void onCreateMasterPin() {
    setFABStateEnabled();
    final View v = getView();
    if (v != null) {
      Snackbar.make(v, "PadLock Enabled", Snackbar.LENGTH_SHORT).show();
    }
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

  @Override public void onPinFABClicked() {
    onPinEntryDialogRequested(getContext().getPackageName(), getActivity().getClass().getName());
  }

  @Override public void onEntryAddedToList(@NonNull AppEntry entry) {
    Timber.d("Add entry: %s", entry);

    // In case the configuration changes, we do the animation again
    if (swipeRefreshLayout != null && !swipeRefreshLayout.isRefreshing()) {
      swipeRefreshLayout.post(() -> swipeRefreshLayout.setRefreshing(true));
    }

    if (adapter == null) {
      throw new NullPointerException("Adapter is NULL");
    }
    adapter.addItem(entry);
  }

  @Override public void onStart() {
    super.onStart();
    if (adapter == null) {
      throw new NullPointerException("Adapter is NULL");
    }
    adapter.onStart();
  }

  @Override public void onStop() {
    super.onStop();
    if (adapter == null) {
      throw new NullPointerException("Adapter is NULL");
    }
    adapter.onStop();
  }

  @Override public void onListPopulateError() {
    // TODO handle list populate error
  }

  @Override public void showOnBoarding() {
    if (presenter == null) {
      throw new NullPointerException("Presenter is NULL");
    }
    new MaterialShowcaseView.Builder(getActivity()).setTarget(fab)
        .setTargetTouchable(false)
        .setMaskColour(ContextCompat.getColor(getContext(), R.color.blue500))
        .setTitleText(R.string.app_name)
        .setContentText(R.string.getting_started)
        .setDismissText(R.string.got_it)
        .setListener(new IShowcaseListener() {
          @Override public void onShowcaseDisplayed(MaterialShowcaseView materialShowcaseView) {
            Timber.d("onShowcaseDisplayed");
          }

          @Override public void onShowcaseDismissed(MaterialShowcaseView materialShowcaseView) {
            Timber.d("onShowcaseDismissed");
            presenter.setOnBoard();
          }
        })
        .build()
        .show(getActivity());
  }

  @Override public void onListCleared() {
    Timber.d("onListCleared");
    handler.post(startRefreshRunnable);
    if (fab != null) {
      fab.hide();
    }
  }

  @Override public void onListPopulated() {
    Timber.d("onListPopulated");
    handler.post(stopRefreshRunnable);
    if (fab != null) {
      fab.show();
    }

    if (presenter == null) {
      throw new NullPointerException("Presenter is NULL");
    }
    presenter.showOnBoarding();
  }

  @Override public void refreshList() {
    if (presenter == null) {
      throw new NullPointerException("Presenter is NULL");
    }
    if (adapter == null) {
      throw new NullPointerException("Adapter is NULL");
    }
    final int oldSize = adapter.getItemCount() - 1;
    for (int i = oldSize; i >= 0; --i) {
      adapter.removeItem();
    }
    onListCleared();
    presenter.populateList();
  }

  @Override public void onSaveInstanceState(@NonNull Bundle outState) {
    if (presenterDataHolder == null) {
      throw new NullPointerException("Presenter dataHolder is NULL");
    }
    if (getActivity().isChangingConfigurations()) {
      presenterDataHolder.put(KEY_PRESENTER, presenter);
      presenterDataHolder.put(KEY_ADAPTER_PRESENTER, adapterPresenter);
      presenterDataHolder.put(KEY_DB_PRESENTER, dbPresenter);
    } else {
      presenterDataHolder.clear();
    }
    super.onSaveInstanceState(outState);
  }
}
