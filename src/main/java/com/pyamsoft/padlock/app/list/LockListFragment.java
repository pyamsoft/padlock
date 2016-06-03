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
  @NonNull private final Handler handler = new Handler();
  @Nullable @BindView(R.id.applist_fab) FloatingActionButton fab;
  @Nullable @BindView(R.id.applist_recyclerview) RecyclerView recyclerView;
  @Nullable @BindView(R.id.applist_swipe_refresh) SwipeRefreshLayout swipeRefreshLayout;
  @Nullable @Inject LockListPresenter presenter;
  @Nullable @Inject AdapterPresenter<AppEntry> adapterPresenter;
  @Nullable @Inject DBPresenter dbPresenter;
  @Nullable private DataHolderFragment<Presenter> presenterDataHolder;
  @Nullable private LockListAdapter adapter;
  @Nullable private LockListLayoutManager lockListLayoutManager;
  @NonNull private final Runnable startRefreshRunnable = new Runnable() {
    @Override public void run() {
      assert swipeRefreshLayout != null;
      swipeRefreshLayout.setRefreshing(true);

      assert lockListLayoutManager != null;
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
      assert swipeRefreshLayout != null;
      swipeRefreshLayout.setRefreshing(false);

      assert lockListLayoutManager != null;
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
    Timber.d("onCreateView");

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

    assert adapterPresenter != null;
    assert dbPresenter != null;
    adapter = new LockListAdapter(this, adapterPresenter, dbPresenter);


    final View view = inflater.inflate(R.layout.fragment_applist, container, false);
    unbinder = ButterKnife.bind(this, view);
    assert presenter != null;
    presenter.onCreateView(this);

    assert adapter != null;
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
    assert swipeRefreshLayout != null;
    swipeRefreshLayout.setColorSchemeResources(R.color.blue500, R.color.amber700, R.color.blue700,
        R.color.amber500);
    swipeRefreshLayout.setOnRefreshListener(() -> {
      Timber.d("onRefresh");
      refreshList();
    });
  }

  @Override public void onResume() {
    super.onResume();
    assert fab != null;
    AnimUtil.popShow(fab, 500, 300);

    assert presenter != null;
    presenter.onResume();
  }

  @Override public void onPause() {
    super.onPause();
    assert fab != null;
    AnimUtil.popHide(fab, 300, 300);

    assert presenter != null;
    presenter.onPause();
  }

  private void setupRecyclerView() {
    lockListLayoutManager = new LockListLayoutManager(getContext());
    lockListLayoutManager.setVerticalScrollEnabled(true);
    final RecyclerView.ItemDecoration dividerDecoration =
        new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL_LIST);

    assert recyclerView != null;
    recyclerView.setLayoutManager(lockListLayoutManager);
    recyclerView.setAdapter(adapter);
    recyclerView.addItemDecoration(dividerDecoration);
  }

  @Override public void onCreate(@Nullable Bundle savedInstanceState) {
    Timber.d("onCreate");
    super.onCreate(savedInstanceState);
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
    assert presenter != null;
    presenter.setSystemVisibilityFromPreference();
  }

  private void setSystemCheckListener() {
    assert displaySystemItem != null;
    displaySystemItem.setOnMenuItemClickListener(item -> {
      if (swipeRefreshLayout != null && !swipeRefreshLayout.isRefreshing()) {
        Timber.d("List is not refreshing. Allow change of system preference");
        assert presenter != null;
        if (item.isChecked()) {
          presenter.setSystemInvisible();
        } else {
          presenter.setSystemVisible();
        }

        refreshList();
      }
      return true;
    });
  }

  private void setSystemVisible(boolean visible) {
    assert displaySystemItem != null;
    displaySystemItem.setOnMenuItemClickListener(null);
    displaySystemItem.setChecked(visible);
    setSystemCheckListener();
  }

  @Override public boolean onOptionsItemSelected(MenuItem item) {
    boolean handled;
    switch (item.getItemId()) {
      case R.id.menu_settings:
        handled = true;
        if (swipeRefreshLayout != null && !swipeRefreshLayout.isRefreshing()) {
          Timber.d("List is not refreshing. Do settings click");
          showSettingsScreen();
        }
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

    assert recyclerView != null;
    recyclerView.setOnClickListener(null);
    recyclerView.setLayoutManager(null);
    recyclerView.setAdapter(null);

    assert fab != null;
    AppUtil.nullifyCallback(fab);
    fab.setOnClickListener(null);

    assert swipeRefreshLayout != null;
    swipeRefreshLayout.setOnRefreshListener(null);

    assert adapter != null;
    adapter.onDestroy();
    if (!getActivity().isChangingConfigurations()) {
      assert presenter != null;
      presenter.onDestroyView();
    }

    cancelFabTask();

    assert unbinder != null;
    unbinder.unbind();

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
    assert fab != null;
    fab.setOnClickListener(view -> {
      assert presenter != null;
      presenter.clickPinFAB();
    });

    AppUtil.setupFABBehavior(fab, new HideScrollFABBehavior(24));
    assert presenter != null;
    presenter.setFABStateFromPreference();
  }

  @Override public void setFABStateEnabled() {
    cancelFabTask();
    assert fab != null;
    fabIconTask = new AsyncVectorDrawableTask(fab);
    fabIconTask.execute(
        new AsyncDrawable(getContext().getApplicationContext(), R.drawable.ic_lock_outline_24dp));
  }

  @Override public void setFABStateDisabled() {
    cancelFabTask();
    assert fab != null;
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
    assert swipeRefreshLayout != null;
    if (!swipeRefreshLayout.isRefreshing()) {
      swipeRefreshLayout.post(() -> swipeRefreshLayout.setRefreshing(true));
    }

    assert adapter != null;
    adapter.addItem(entry);
  }

  @Override public void onStart() {
    super.onStart();
    assert adapter != null;
    adapter.onStart();
  }

  @Override public void onStop() {
    super.onStop();
    assert adapter != null;
    adapter.onStop();
  }

  @Override public void onListPopulateError() {
    // TODO handle list populate error
  }

  @Override public void showOnBoarding() {
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
            assert presenter != null;
            presenter.setOnBoard();
          }
        })
        .build()
        .show(getActivity());
  }

  @Override public void onListCleared() {
    Timber.d("onListCleared");
    handler.post(startRefreshRunnable);
    assert fab != null;
    fab.hide();
  }

  @Override public void onListPopulated() {
    Timber.d("onListPopulated");
    handler.post(stopRefreshRunnable);
    assert fab != null;
    fab.show();

    assert presenter != null;
    presenter.showOnBoarding();
  }

  @Override public void refreshList() {
    assert adapter != null;
    final int oldSize = adapter.getItemCount() - 1;
    for (int i = oldSize; i >= 0; --i) {
      adapter.removeItem();
    }
    onListCleared();
    assert presenter != null;
    presenter.populateList();
  }

  @Override public void onSaveInstanceState(@NonNull Bundle outState) {
    assert presenterDataHolder != null;
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
