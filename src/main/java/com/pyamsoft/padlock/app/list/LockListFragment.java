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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
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
import com.pyamsoft.padlock.app.GlobalConstants;
import com.pyamsoft.padlock.app.main.PageAwareFragment;
import com.pyamsoft.padlock.app.pinentry.MasterPinSubmitCallback;
import com.pyamsoft.padlock.app.pinentry.PinEntryDialog;
import com.pyamsoft.padlock.app.service.PadLockService;
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

public final class LockListFragment extends PageAwareFragment
    implements LockListPresenter.LockList, PinEntryDialogRequest, MasterPinSubmitCallback {

  private static final String PIN_DIALOG_TAG = "pin_dialog";

  @BindView(R.id.applist_fab) FloatingActionButton fab;
  @BindView(R.id.applist_recyclerview) RecyclerView recyclerView;
  @BindView(R.id.applist_swipe_refresh) SwipeRefreshLayout swipeRefreshLayout;
  @Inject LockListPresenter presenter;
  @Inject AdapterPresenter<AppEntry> adapterPresenter;
  private DataHolderFragment<Presenter> presenterDataHolder;
  private LockListAdapter adapter;
  private LockListLayoutManager lockListLayoutManager;
  private boolean firstRefresh;
  private AsyncVectorDrawableTask fabIconTask;
  private SwitchCompat displaySystem;
  private Unbinder unbinder;

  @Nullable @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    final View view = inflater.inflate(R.layout.fragment_applist, container, false);
    unbinder = ButterKnife.bind(this, view);
    presenter.onCreateView(this);
    adapter.bind(this);
    return view;
  }

  @SuppressLint("ShowToast") @Override
  public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
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
    presenter.onResume();
  }

  @Override public void onPause() {
    super.onPause();
    AnimUtil.popHide(fab, 300, 300);
    presenter.onPause();
  }

  private void setupRecyclerView() {
    lockListLayoutManager = new LockListLayoutManager(getContext());
    lockListLayoutManager.setVerticalScrollEnabled(true);
    final RecyclerView.ItemDecoration dividerDecoration =
        new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL_LIST);
    recyclerView.setLayoutManager(lockListLayoutManager);
    recyclerView.setAdapter(adapter);
    recyclerView.addItemDecoration(dividerDecoration);
  }

  @Override public void onCreate(@Nullable Bundle savedInstanceState) {
    Timber.d("onCreate");
    super.onCreate(savedInstanceState);

    presenterDataHolder = DataHolderFragment.getInstance(getFragmentManager(), Presenter.class);

    final LockListPresenter lockListPresenter = (LockListPresenter) presenterDataHolder.pop(
        GlobalConstants.DATA_HOLDER_ID_LOCK_LIST_PRESENTER);
    @SuppressWarnings("unchecked") final AdapterPresenter<AppEntry> entryAdapterPresenter =
        (AdapterPresenter<AppEntry>) presenterDataHolder.pop(
            GlobalConstants.DATA_HOLDER_ID_LOCK_LIST_ADAPTER_PRESENTER);
    if (lockListPresenter == null || entryAdapterPresenter == null) {
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
    }

    adapter = new LockListAdapter(adapterPresenter);

    setHasOptionsMenu(true);
  }

  @Override public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    super.onCreateOptionsMenu(menu, inflater);
    inflater.inflate(R.menu.locklist_menu, menu);
  }

  @Override public void onPrepareOptionsMenu(Menu menu) {
    super.onPrepareOptionsMenu(menu);
    setupLockListMenuItems(menu);
  }

  private void setupLockListMenuItems(final Menu menu) {
    final MenuItem displaySystemItem = menu.findItem(R.id.menu_is_system);
    if (displaySystemItem == null) {
      Timber.e("Item is NULL");
      return;
    }

    displaySystemItem.setVisible(PadLockService.isEnabled());
    displaySystemItem.setActionView(R.layout.action_view_toolbar_switch);
    displaySystem =
        (SwitchCompat) displaySystemItem.getActionView().findViewById(R.id.action_view_switch);
    if (displaySystem == null) {
      Timber.e("Action view is NULL");
      return;
    }

    displaySystem.setEnabled(!swipeRefreshLayout.isRefreshing());
    presenter.setSystemVisibilityFromPreference();
  }

  private void setSystemCheckListener() {
    displaySystem.setOnCheckedChangeListener((buttonView, isChecked) -> {
      if (isChecked) {
        presenter.setSystemVisible();
      } else {
        presenter.setSystemInvisible();
      }
      refreshList();
    });
  }

  @Override public void setSystemVisible() {
    if (displaySystem != null) {
      displaySystem.setOnCheckedChangeListener(null);
      displaySystem.setChecked(true);
      setSystemCheckListener();
    }
  }

  @Override public void setSystemInvisible() {
    if (displaySystem != null) {
      displaySystem.setOnCheckedChangeListener(null);
      displaySystem.setChecked(false);
      setSystemCheckListener();
    }
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

    adapter.unbind();
    presenter.onDestroyView();

    cancelFabTask();

    if (unbinder != null) {
      unbinder.unbind();
    }
  }

  private void cancelFabTask() {
    if (fabIconTask != null) {
      if (!fabIconTask.isCancelled()) {
        fabIconTask.cancel(true);
      }
      fabIconTask = null;
    }
  }

  @Override public void onDestroy() {
    super.onDestroy();

    adapter.onDestroy();
    presenter.onDestroy();
  }

  private void setupFAB() {
    if (fab != null) {
      fab.setOnClickListener(view -> presenter.clickPinFAB());
      AppUtil.setupFABBehavior(fab, new HideScrollFABBehavior(24));
      presenter.setFABStateFromPreference();
    }
  }

  @Override public void setFABStateEnabled() {
    cancelFabTask();
    fabIconTask = new AsyncVectorDrawableTask(fab);
    fabIconTask.execute(
        new AsyncDrawable(getContext().getApplicationContext(), R.drawable.ic_lock_outline_24dp));
  }

  @Override public void setFABStateDisabled() {
    cancelFabTask();
    fabIconTask = new AsyncVectorDrawableTask(fab);
    fabIconTask.execute(
        new AsyncDrawable(getContext().getApplicationContext(), R.drawable.ic_lock_open_24dp));
  }

  @Override public void onPinEntryDialogRequested(String packageName, String activityName) {
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
    if (!swipeRefreshLayout.isRefreshing()) {
      swipeRefreshLayout.post(() -> swipeRefreshLayout.setRefreshing(true));
    }

    adapter.addItem(entry);
  }

  @Override public void onStart() {
    super.onStart();
    adapter.onStart();
  }

  @Override public void onStop() {
    super.onStop();
    adapter.onStop();
  }

  @Override public void onPageUnselected() {
    if (swipeRefreshLayout != null) {
      if (!swipeRefreshLayout.isRefreshing()) {
        AnimUtil.popHide(fab, 300, 300);
      }
    }
  }

  @Override public void onPageSelected() {
    if (swipeRefreshLayout != null) {
      if (!swipeRefreshLayout.isRefreshing()) {
        AnimUtil.popShow(fab, 300, 300);
      }
    }
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
            presenter.setOnBoard();
          }
        })
        .build()
        .show(getActivity());
  }

  @Override public void onListCleared() {
    Timber.d("onListCleared");
    lockListLayoutManager.setVerticalScrollEnabled(false);
    swipeRefreshLayout.post(() -> {
      swipeRefreshLayout.setRefreshing(true);
      final FragmentActivity activity = getActivity();
      if (activity != null) {
        Timber.d("Reload options");
        activity.supportInvalidateOptionsMenu();
      }
    });
    fab.hide();
  }

  @Override public void onListPopulated() {
    Timber.d("onListPopulated");
    lockListLayoutManager.setVerticalScrollEnabled(true);
    swipeRefreshLayout.post(() -> {
      swipeRefreshLayout.setRefreshing(false);
      final FragmentActivity activity = getActivity();
      if (activity != null) {
        Timber.d("Reload options");
        activity.supportInvalidateOptionsMenu();
      }
    });
    fab.show();

    presenter.showOnBoarding();
  }

  @Override public void refreshList() {
    final int oldSize = adapter.getItemCount() - 1;
    for (int i = oldSize; i >= 0; --i) {
      adapter.removeItem();
    }
    onListCleared();
    presenter.populateList();
  }

  @Override public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    if (getActivity().isChangingConfigurations()) {
      presenterDataHolder.put(GlobalConstants.DATA_HOLDER_ID_LOCK_LIST_PRESENTER, presenter);
      presenterDataHolder.put(GlobalConstants.DATA_HOLDER_ID_LOCK_LIST_ADAPTER_PRESENTER,
          adapterPresenter);
    } else {
      presenterDataHolder.clear();
    }
  }
}
