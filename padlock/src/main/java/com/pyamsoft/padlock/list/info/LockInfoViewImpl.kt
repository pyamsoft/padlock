/*
 * Copyright 2019 Peter Kenji Yamanaka
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
 *
 */

package com.pyamsoft.padlock.list.info

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle.Event.ON_DESTROY
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ModelAdapter
import com.pyamsoft.padlock.R
import com.pyamsoft.padlock.databinding.DialogLockInfoBinding
import com.pyamsoft.padlock.helper.ListStateUtil
import com.pyamsoft.padlock.list.FilterListDelegate
import com.pyamsoft.padlock.loader.AppIconLoader
import com.pyamsoft.padlock.model.list.ActivityEntry
import com.pyamsoft.pydroid.loader.ImageLoader
import com.pyamsoft.pydroid.loader.ImageTarget
import com.pyamsoft.pydroid.loader.Loaded
import com.pyamsoft.pydroid.ui.theme.Theming
import com.pyamsoft.pydroid.ui.util.DebouncedOnClickListener
import com.pyamsoft.pydroid.ui.util.Snackbreak
import com.pyamsoft.pydroid.ui.util.refreshing
import com.pyamsoft.pydroid.ui.util.setOnDebouncedClickListener
import com.pyamsoft.pydroid.ui.util.setUpEnabled
import com.pyamsoft.pydroid.util.tintWith
import com.pyamsoft.pydroid.util.toDp
import java.util.Collections
import javax.inject.Inject
import javax.inject.Named

internal class LockInfoViewImpl @Inject internal constructor(
  @Named("app_name") private val applicationName: String,
  @Named("package_name") private val packageName: String,
  @Named("list_state_tag") private val listStateTag: String,
  @Named("app_icon") private val icon: Int,
  @Named("app_system") private val isSystem: Boolean,
  private val activity: FragmentActivity,
  private val theming: Theming,
  private val owner: LifecycleOwner,
  private val inflater: LayoutInflater,
  private val container: ViewGroup?,
  private val savedInstanceState: Bundle?,
  private val appIconLoader: AppIconLoader,
  private val imageLoader: ImageLoader
) : LockInfoView, LifecycleObserver {

  private lateinit var binding: DialogLockInfoBinding
  private lateinit var modelAdapter: ModelAdapter<ActivityEntry, LockInfoBaseItem<*, *, *>>
  private lateinit var filterListDelegate: FilterListDelegate

  private var appIconLoaded: Loaded? = null
  private var toolbarIconLoaded: Loaded? = null

  private var lastPosition: Int = 0

  init {
    owner.lifecycle.addObserver(this)
  }

  @Suppress("unused")
  @OnLifecycleEvent(ON_DESTROY)
  internal fun destroy() {
    owner.lifecycle.removeObserver(this)

    filterListDelegate.onDestroyView()
    binding.apply {
      lockInfoRecycler.setOnDebouncedClickListener(null)
      lockInfoRecycler.layoutManager = null
      lockInfoRecycler.adapter = null
      unbind()
    }

    appIconLoaded?.dispose()
    toolbarIconLoaded?.dispose()

    modelAdapter.clear()
  }

  override fun root(): View {
    return binding.root
  }

  override fun create() {
    binding = DialogLockInfoBinding.inflate(inflater, container, false)

    setupSwipeRefresh()
    setupRecyclerView()
    setupPackageInfo()
    setupToolbar()
    loadToolbarIcon()
    prepareFilterDelegate()
    restoreListPosition()
    loadAppIcon()
  }

  private fun loadToolbarIcon() {
    toolbarIconLoaded?.dispose()
    toolbarIconLoaded = imageLoader.load(R.drawable.ic_close_24dp)
        .into(object : ImageTarget<Drawable> {
          override fun clear() {
            binding.lockInfoToolbar.navigationIcon = null
          }

          override fun setError(error: Drawable?) {
            binding.lockInfoToolbar.navigationIcon = error
          }

          override fun setImage(image: Drawable) {
            val color = ContextCompat.getColor(binding.lockInfoToolbar.context, R.color.white)
            binding.lockInfoToolbar.navigationIcon = image.tintWith(color)
          }

          override fun setPlaceholder(placeholder: Drawable?) {
            binding.lockInfoToolbar.navigationIcon = placeholder
          }

          override fun view(): View {
            return binding.lockInfoToolbar
          }

        })
  }

  private fun loadAppIcon() {
    appIconLoaded?.dispose()
    appIconLoaded = appIconLoader.loadAppIcon(packageName, icon)
        .into(binding.lockInfoIcon)
  }

  override fun commitListState(outState: Bundle?) {
    if (outState == null) {
      // If we are not commiting to bundle, commit to local var
      lastPosition = ListStateUtil.getCurrentPosition(binding.lockInfoRecycler)
    }

    ListStateUtil.saveState(listStateTag, outState, binding.lockInfoRecycler)
  }

  override fun getListData(): List<ActivityEntry> {
    return Collections.unmodifiableList(modelAdapter.models)
  }

  private fun setupSwipeRefresh() {
    binding.lockInfoSwipeRefresh.apply {
      setColorSchemeResources(R.color.blue500, R.color.blue700)
    }
  }

  override fun onSwipeRefresh(onSwipe: () -> Unit) {
    binding.lockInfoSwipeRefresh.setOnRefreshListener {
      startRefreshing()
      onSwipe()
    }
  }

  private fun restoreListPosition() {
    lastPosition = ListStateUtil.restoreState(listStateTag, savedInstanceState)
  }

  private fun setupRecyclerView() {
    // Setup adapter
    modelAdapter = ModelAdapter {
      return@ModelAdapter when (it) {
        is ActivityEntry.Item -> LockInfoItem(it, isSystem)
        is ActivityEntry.Group -> LockInfoGroup(packageName, it)
      }
    }

    binding.lockInfoRecycler.apply {
      layoutManager = LinearLayoutManager(activity).apply {
        isItemPrefetchEnabled = true
        initialPrefetchItemCount = 3
      }

      setHasFixedSize(true)
      addItemDecoration(DividerItemDecoration(activity, DividerItemDecoration.VERTICAL))
      adapter = FastAdapter.with<
          LockInfoBaseItem<*, *, *>,
          ModelAdapter<ActivityEntry, LockInfoBaseItem<*, *, *>>
          >(modelAdapter)
    }

    // Set initial view state
    showRecycler()
  }

  private fun startRefreshing() {
    filterListDelegate.setEnabled(false)
    binding.lockInfoSwipeRefresh.refreshing(true)
  }

  private fun doneRefreshing() {
    filterListDelegate.setEnabled(true)
    binding.lockInfoSwipeRefresh.refreshing(false)

    if (modelAdapter.adapterItemCount > 0) {
      showRecycler()

      // Restore last position
      lastPosition = ListStateUtil.restorePosition(lastPosition, binding.lockInfoRecycler)
    } else {
      hideRecycler()
    }
  }

  private fun setupToolbar() {
    val theme: Int
    if (theming.isDarkTheme()) {
      theme = R.style.ThemeOverlay_MaterialComponents
    } else {
      theme = R.style.ThemeOverlay_MaterialComponents_Light
    }
    binding.lockInfoToolbar.apply {
      popupTheme = theme
      title = applicationName
      inflateMenu(R.menu.search_menu)
      inflateMenu(R.menu.lockinfo_menu)

      // Tint search icon white to match Toolbar
      val searchItem = menu.findItem(R.id.menu_search)
      val searchIcon = searchItem.icon
      searchIcon.mutate()
          .also { icon ->
            val tintedIcon = icon.tintWith(context, R.color.white)
            searchItem.icon = tintedIcon
          }


      ViewCompat.setElevation(this, 4f.toDp(context).toFloat())

      setUpEnabled(true)
    }
  }

  private fun prepareFilterDelegate() {
    filterListDelegate = FilterListDelegate()
    filterListDelegate.onViewCreated(modelAdapter)
    filterListDelegate.onPrepareOptionsMenu(binding.lockInfoToolbar.menu, modelAdapter)
  }

  override fun onToolbarNavigationClicked(onClick: () -> Unit) {
    binding.lockInfoToolbar.setNavigationOnClickListener(
        DebouncedOnClickListener.create { onClick() }
    )
  }

  override fun onToolbarMenuItemClicked(onClick: (id: Int) -> Unit) {
    binding.lockInfoToolbar.setOnMenuItemClickListener {
      onClick(it.itemId)
      return@setOnMenuItemClickListener true
    }
  }

  private fun setupPackageInfo() {
    binding.apply {
      lockInfoPackageName.text = packageName
      lockInfoSystem.text = if (isSystem) "YES" else "NO"
    }
  }

  private fun showRecycler() {
    binding.apply {
      lockInfoEmpty.visibility = View.GONE
      lockInfoRecycler.visibility = View.VISIBLE
    }
  }

  private fun hideRecycler() {
    binding.apply {
      lockInfoRecycler.visibility = View.GONE
      lockInfoEmpty.visibility = View.VISIBLE
    }
  }

  override fun onListPopulateBegin() {
    startRefreshing()
  }

  override fun onListPopulated() {
    doneRefreshing()
  }

  override fun onListLoaded(list: List<ActivityEntry>) {
    modelAdapter.set(list)
  }

  override fun onListPopulateError(onAction: () -> Unit) {
    Snackbreak.bindTo(owner)
        .long(root(), "Failed to load list for $applicationName")
        .setAction("Retry") { onAction() }
        .show()
  }

  override fun onModifyEntryError(onAction: () -> Unit) {
    Snackbreak.bindTo(owner)
        .long(root(), "Failed to modify list for $applicationName")
        .setAction("Retry") { onAction() }
        .show()
  }

  override fun onDatabaseChangeError(onAction: () -> Unit) {
    Snackbreak.bindTo(owner)
        .long(root(), "Failed realtime monitoring for $applicationName")
        .setAction("Retry") { onAction() }
        .show()
  }

  override fun onDatabaseChangeReceived(
    index: Int,
    entry: ActivityEntry
  ) {
    modelAdapter.set(index, entry)
  }

}
