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

import android.app.Dialog;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;
import com.mikepenz.fastadapter.FastAdapter;
import com.mikepenz.fastadapter.commons.adapters.FastItemAdapter;
import com.pyamsoft.padlock.PadLock;
import com.pyamsoft.padlock.R;
import com.pyamsoft.padlock.databinding.DialogLockInfoBinding;
import com.pyamsoft.padlock.iconloader.AppIconLoaderPresenter;
import com.pyamsoft.padlock.iconloader.AppIconLoaderPresenterLoader;
import com.pyamsoft.padlock.iconloader.AppIconLoaderView;
import com.pyamsoft.padlock.model.ActivityEntry;
import com.pyamsoft.padlock.model.AppEntry;
import com.pyamsoft.padlock.model.LockState;
import com.pyamsoft.pydroid.cache.PersistentCache;
import com.pyamsoft.pydroid.util.AppUtil;
import java.util.List;
import timber.log.Timber;

public class LockInfoDialog extends DialogFragment
    implements LockInfoPresenter.LockInfoView, AppIconLoaderView {

  @NonNull public static final String TAG = "LockInfoDialog";
  @NonNull private static final String ARG_APP_PACKAGE_NAME = "app_packagename";
  @NonNull private static final String ARG_APP_NAME = "app_name";
  @NonNull private static final String ARG_APP_SYSTEM = "app_system";
  @NonNull private static final String KEY_APP_ICON_LOADER = TAG + "key_app_icon_loader";
  @NonNull private static final String KEY_PRESENTER = TAG + "key_info_presenter";
  @NonNull private final Handler handler = new Handler(Looper.getMainLooper());
  @SuppressWarnings("WeakerAccess") LockInfoPresenter presenter;
  @SuppressWarnings("WeakerAccess") AppIconLoaderPresenter appIconLoaderPresenter;
  @SuppressWarnings("WeakerAccess") FastItemAdapter<LockInfoItem> fastItemAdapter;
  @SuppressWarnings("WeakerAccess") DialogLockInfoBinding binding;
  @NonNull private final Runnable startRefreshRunnable =
      () -> binding.lockInfoSwipeRefresh.post(() -> {
        if (binding != null) {
          if (binding.lockInfoSwipeRefresh != null) {
            binding.lockInfoSwipeRefresh.setRefreshing(true);
          }
        }
      });
  @NonNull private final Runnable stopRefreshRunnable =
      () -> binding.lockInfoSwipeRefresh.post(() -> {
        if (binding != null) {
          if (binding.lockInfoSwipeRefresh != null) {
            binding.lockInfoSwipeRefresh.setRefreshing(false);
          }
        }
      });
  private String appPackageName;
  private String appName;
  private boolean appIsSystem;
  private boolean listIsRefreshed;
  private DividerItemDecoration dividerDecoration;
  private FilterListDelegate filterListDelegate;

  @CheckResult @NonNull public static LockInfoDialog newInstance(@NonNull AppEntry appEntry) {
    final LockInfoDialog fragment = new LockInfoDialog();
    final Bundle args = new Bundle();

    args.putString(ARG_APP_PACKAGE_NAME, appEntry.packageName());
    args.putString(ARG_APP_NAME, appEntry.name());
    args.putBoolean(ARG_APP_SYSTEM, appEntry.system());

    fragment.setArguments(args);
    return fragment;
  }

  @NonNull @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
    final Dialog dialog = super.onCreateDialog(savedInstanceState);
    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
    return dialog;
  }

  @Override public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    appPackageName = getArguments().getString(ARG_APP_PACKAGE_NAME, null);
    appName = getArguments().getString(ARG_APP_NAME, null);
    appIsSystem = getArguments().getBoolean(ARG_APP_SYSTEM, false);

    if (appPackageName == null || appName == null) {
      throw new NullPointerException("App information is NULL");
    }

    final String presenterKeyFull = KEY_PRESENTER + appPackageName + appName;
    presenter =
        PersistentCache.load(getActivity(), presenterKeyFull, new LockInfoPresenterLoader());
    appIconLoaderPresenter = PersistentCache.load(getActivity(), KEY_APP_ICON_LOADER,
        new AppIconLoaderPresenterLoader());
  }

  @Nullable @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    listIsRefreshed = false;
    filterListDelegate = new FilterListDelegate();
    fastItemAdapter = new FastItemAdapter<>();
    binding = DialogLockInfoBinding.inflate(inflater, container, false);
    return binding.getRoot();
  }

  @Override public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    setupToolbar();
    binding.lockInfoPackageName.setText(appPackageName);
    binding.lockInfoSystem.setText((appIsSystem ? "YES" : "NO"));
    filterListDelegate.onViewCreated(fastItemAdapter);
    setupSwipeRefresh();
    setupRecyclerView();
  }

  private void setupToolbar() {
    binding.lockInfoToolbar.setTitle(appName);
    binding.lockInfoToolbar.setNavigationOnClickListener(v -> dismiss());
    binding.lockInfoToolbar.inflateMenu(R.menu.search_menu);
    filterListDelegate.onPrepareOptionsMenu(binding.lockInfoToolbar.getMenu(), fastItemAdapter);
  }

  private void setupSwipeRefresh() {
    binding.lockInfoSwipeRefresh.setColorSchemeResources(R.color.blue500, R.color.amber700,
        R.color.blue700, R.color.amber500);
    binding.lockInfoSwipeRefresh.setOnRefreshListener(() -> {
      Timber.d("onRefresh");
      refreshList();
    });
  }

  private void setupRecyclerView() {
    dividerDecoration = new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL);

    fastItemAdapter.withFilterPredicate((item, query) -> {
      final String queryString = String.valueOf(query).toLowerCase().trim();
      return item.filterAgainst(queryString);
    });

    fastItemAdapter.withOnBindViewHolderListener(new FastAdapter.OnBindViewHolderListener() {

      @CheckResult @NonNull
      private LockInfoItem.ViewHolder toLockInfoViewHolder(RecyclerView.ViewHolder viewHolder) {
        if (viewHolder instanceof LockInfoItem.ViewHolder) {
          return (LockInfoItem.ViewHolder) viewHolder;
        } else {
          throw new IllegalStateException("ViewHolder is not LockInfoItem.ViewHolder");
        }
      }

      @Override
      public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int i, List<Object> list) {
        final LockInfoItem.ViewHolder holder = toLockInfoViewHolder(viewHolder);
        fastItemAdapter.getAdapterItem(holder.getAdapterPosition()).bindView(holder, list);
        holder.bind(LockInfoDialog.this::processDatabaseModifyEvent);
      }

      @Override public void unBindViewHolder(RecyclerView.ViewHolder viewHolder, int i) {
        final LockInfoItem.ViewHolder holder = toLockInfoViewHolder(viewHolder);
        final LockInfoItem item = (LockInfoItem) holder.itemView.getTag();
        if (item != null) {
          item.unbindView(holder);
        }
      }
    });

    binding.lockInfoRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
    binding.lockInfoRecycler.addItemDecoration(dividerDecoration);
    binding.lockInfoRecycler.setAdapter(fastItemAdapter);
  }

  @Override public void onDestroyView() {
    super.onDestroyView();
    filterListDelegate.onDestroyView();

    handler.removeCallbacksAndMessages(null);
    binding.lockInfoRecycler.removeItemDecoration(dividerDecoration);
    binding.lockInfoRecycler.setOnClickListener(null);
    binding.lockInfoRecycler.setLayoutManager(null);
    binding.lockInfoRecycler.setAdapter(null);
    binding.unbind();
  }

  @Override public void onDestroy() {
    super.onDestroy();
    PadLock.getRefWatcher(this).watch(this);
  }

  @Override public void onStart() {
    super.onStart();
    presenter.bindView(this);
    appIconLoaderPresenter.bindView(this);
    appIconLoaderPresenter.loadApplicationIcon(appPackageName);
    if (!listIsRefreshed) {
      if (!binding.lockInfoSwipeRefresh.isRefreshing()) {
        binding.lockInfoSwipeRefresh.post(() -> {
          if (binding != null) {
            if (binding.lockInfoSwipeRefresh != null) {
              binding.lockInfoSwipeRefresh.setRefreshing(true);
            }
          }
        });
      }
      presenter.populateList(appPackageName);
    }
  }

  @Override public void onStop() {
    super.onStop();
    presenter.unbindView();
    appIconLoaderPresenter.unbindView();
  }

  @Override public void onResume() {
    super.onResume();
    // The dialog is super small for some reason. We have to set the size manually, in onResume
    final Window window = getDialog().getWindow();
    if (window != null) {
      window.setLayout(WindowManager.LayoutParams.MATCH_PARENT,
          WindowManager.LayoutParams.WRAP_CONTENT);
    }
  }

  @Override public void refreshList() {
    fastItemAdapter.clear();
    presenter.clearList();
    onListCleared();

    binding.lockInfoRecycler.setClickable(false);
    presenter.populateList(appPackageName);
  }

  @Override public void onEntryAddedToList(@NonNull ActivityEntry entry) {
    // In case the configuration changes, we do the animation again
    if (!binding.lockInfoSwipeRefresh.isRefreshing()) {
      binding.lockInfoSwipeRefresh.post(() -> {
        if (binding != null) {
          if (binding.lockInfoSwipeRefresh != null) {
            binding.lockInfoSwipeRefresh.setRefreshing(true);
          }
        }
      });
    }

    fastItemAdapter.add(new LockInfoItem(appPackageName, entry));
  }

  @Override public void onListPopulated() {
    binding.lockInfoRecycler.setClickable(true);
    handler.removeCallbacksAndMessages(null);
    handler.post(stopRefreshRunnable);

    if (fastItemAdapter.getAdapterItemCount() > 0) {
      Timber.d("Refresh finished");
      listIsRefreshed = true;
      presenter.showOnBoarding();
    } else {
      Toast.makeText(getContext(), "Error while loading list. Please try again.",
          Toast.LENGTH_SHORT).show();
    }
  }

  @Override public void onListPopulateError() {
    Timber.e("onListPopulateError");
    onListPopulated();
    AppUtil.guaranteeSingleDialogFragment(getFragmentManager(), new ErrorDialog(), "error");
  }

  @Override public void onListCleared() {
    Timber.d("Prepare for refresh");
    listIsRefreshed = false;

    handler.removeCallbacksAndMessages(null);
    handler.post(startRefreshRunnable);
  }

  @Override public void onApplicationIconLoadedError() {
    // TODO handle
  }

  @Override public void onApplicationIconLoadedSuccess(@NonNull Drawable drawable) {
    binding.lockInfoIcon.setImageDrawable(drawable);
  }

  @Override public void onDatabaseEntryCreated(int position) {
    onDatabaseUpdated(position, LockState.LOCKED);
  }

  @Override public void onDatabaseEntryDeleted(int position) {
    onDatabaseUpdated(position, LockState.DEFAULT);
  }

  @Override public void onDatabaseEntryWhitelisted(int position) {
    onDatabaseUpdated(position, LockState.WHITELISTED);
  }

  private void onDatabaseUpdated(int position, @NonNull LockState newLockState) {
    final LockInfoItem oldItem = fastItemAdapter.getItem(position);
    final LockInfoItem newItem = oldItem.copyWithNewLockState(newLockState);
    fastItemAdapter.set(position, newItem);
    presenter.updateCachedEntryLockState(newItem.getName(), newLockState);
  }

  @Override public void onDatabaseEntryError(int position) {
    AppUtil.guaranteeSingleDialogFragment(getFragmentManager(), new ErrorDialog(), "error");
  }

  @Override public void showOnBoarding() {
    Timber.d("Show onboarding");
    // TODO
  }

  @SuppressWarnings("WeakerAccess") void processDatabaseModifyEvent(int position,
      @NonNull String activityName, @NonNull LockState previousLockState,
      @NonNull LockState newLockState) {
    Timber.d("Received a database modify event request for %s %s at %d [%s]", appPackageName,
        activityName, position, newLockState.name());
    final boolean whitelist = newLockState == LockState.WHITELISTED;
    final boolean forceLock = newLockState == LockState.LOCKED;
    final boolean wasDefault = previousLockState == LockState.DEFAULT;
    presenter.modifyDatabaseEntry(wasDefault, position, appPackageName, activityName, null,
        appIsSystem, whitelist, forceLock);
  }
}
