package com.obsidianbackup.tv.ui

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.leanback.app.BrowseSupportFragment
import androidx.leanback.widget.*
import com.obsidianbackup.tv.R
import com.obsidianbackup.tv.backup.TVBackupManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainFragment : BrowseSupportFragment() {

    @Inject
    lateinit var backupManager: TVBackupManager

    private lateinit var rowsAdapter: ArrayObjectAdapter

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        setupUI()
        loadRows()
        setupEventListeners()
    }

    private fun setupUI() {
        title = getString(R.string.app_name)
        headersState = HEADERS_ENABLED
        isHeadersTransitionOnBackEnabled = true

        // Set brand color
        brandColor = ContextCompat.getColor(requireContext(), R.color.tv_primary)
        searchAffordanceColor = ContextCompat.getColor(requireContext(), R.color.tv_accent)
    }

    private fun loadRows() {
        rowsAdapter = ArrayObjectAdapter(ListRowPresenter())

        // Dashboard Row
        val dashboardHeader = HeaderItem(0, "Dashboard")
        val dashboardAdapter = ArrayObjectAdapter(DashboardCardPresenter())
        dashboardAdapter.add(DashboardCard("Backup Now", "Start a new backup", R.drawable.ic_backup))
        dashboardAdapter.add(DashboardCard("Recent Backups", "View backup history", R.drawable.ic_history))
        dashboardAdapter.add(DashboardCard("Storage Status", "Check storage usage", R.drawable.ic_storage))
        rowsAdapter.add(ListRow(dashboardHeader, dashboardAdapter))

        // TV Apps Row
        val appsHeader = HeaderItem(1, "TV Apps")
        val appsAdapter = ArrayObjectAdapter(TVAppCardPresenter())
        loadTVApps(appsAdapter)
        rowsAdapter.add(ListRow(appsHeader, appsAdapter))

        // Streaming Apps Row
        val streamingHeader = HeaderItem(2, "Streaming Apps")
        val streamingAdapter = ArrayObjectAdapter(TVAppCardPresenter())
        loadStreamingApps(streamingAdapter)
        rowsAdapter.add(ListRow(streamingHeader, streamingAdapter))

        // Games Row
        val gamesHeader = HeaderItem(3, "TV Games")
        val gamesAdapter = ArrayObjectAdapter(TVAppCardPresenter())
        loadTVGames(gamesAdapter)
        rowsAdapter.add(ListRow(gamesHeader, gamesAdapter))

        // Settings Row
        val settingsHeader = HeaderItem(4, "Settings")
        val settingsAdapter = ArrayObjectAdapter(SettingsCardPresenter())
        settingsAdapter.add(SettingsItem("Backup Settings", "Configure backup options", R.drawable.ic_settings))
        settingsAdapter.add(SettingsItem("Cloud Sync", "Connect cloud storage", R.drawable.ic_cloud))
        settingsAdapter.add(SettingsItem("Schedule", "Set automatic backups", R.drawable.ic_schedule))
        settingsAdapter.add(SettingsItem("About", "App information", R.drawable.ic_info))
        rowsAdapter.add(ListRow(settingsHeader, settingsAdapter))

        adapter = rowsAdapter
    }

    private fun loadTVApps(adapter: ArrayObjectAdapter) {
        val apps = backupManager.getTVApps()
        apps.forEach { app ->
            adapter.add(TVAppCard(
                name = app.name,
                packageName = app.packageName,
                icon = app.icon,
                size = app.size,
                isBackedUp = app.isBackedUp
            ))
        }
        if (apps.isEmpty()) {
            adapter.add(TVAppCard("No TV apps found", "", null, 0, false))
        }
    }

    private fun loadStreamingApps(adapter: ArrayObjectAdapter) {
        val streamingApps = backupManager.getStreamingApps()
        streamingApps.forEach { app ->
            adapter.add(TVAppCard(
                name = app.name,
                packageName = app.packageName,
                icon = app.icon,
                size = app.size,
                isBackedUp = app.isBackedUp
            ))
        }
        if (streamingApps.isEmpty()) {
            adapter.add(TVAppCard("No streaming apps found", "", null, 0, false))
        }
    }

    private fun loadTVGames(adapter: ArrayObjectAdapter) {
        val games = backupManager.getTVGames()
        games.forEach { game ->
            adapter.add(TVAppCard(
                name = game.name,
                packageName = game.packageName,
                icon = game.icon,
                size = game.size,
                isBackedUp = game.isBackedUp
            ))
        }
        if (games.isEmpty()) {
            adapter.add(TVAppCard("No TV games found", "", null, 0, false))
        }
    }

    private fun setupEventListeners() {
        onItemViewClickedListener = ItemViewClickedListener()
    }

    inner class ItemViewClickedListener : OnItemViewClickedListener {
        override fun onItemClicked(
            itemViewHolder: Presenter.ViewHolder,
            item: Any,
            rowViewHolder: RowPresenter.ViewHolder,
            row: Row
        ) {
            when (item) {
                is DashboardCard -> handleDashboardClick(item)
                is TVAppCard -> handleAppClick(item)
                is SettingsItem -> handleSettingsClick(item)
            }
        }
    }

    private fun handleDashboardClick(card: DashboardCard) {
        when (card.title) {
            "Backup Now" -> {
                Toast.makeText(context, "Starting backup...", Toast.LENGTH_SHORT).show()
                backupManager.startBackup()
            }
            "Recent Backups" -> {
                val intent = Intent(context, BackupDetailsActivity::class.java)
                startActivity(intent)
            }
            "Storage Status" -> {
                Toast.makeText(context, "Storage: ${backupManager.getStorageStatus()}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun handleAppClick(app: TVAppCard) {
        if (app.packageName.isNotEmpty()) {
            val intent = Intent(context, BackupDetailsActivity::class.java).apply {
                putExtra("package_name", app.packageName)
                putExtra("app_name", app.name)
            }
            startActivity(intent)
        }
    }

    private fun handleSettingsClick(item: SettingsItem) {
        when (item.title) {
            "Backup Settings", "Cloud Sync", "Schedule", "About" -> {
                val intent = Intent(context, SettingsActivity::class.java).apply {
                    putExtra("setting_type", item.title)
                }
                startActivity(intent)
            }
        }
    }

    fun onBackPressed(): Boolean {
        // Return true if we handled the back press, false otherwise
        return false
    }

    // Data classes for cards
    data class DashboardCard(
        val title: String,
        val description: String,
        val iconRes: Int
    )

    data class TVAppCard(
        val name: String,
        val packageName: String,
        val icon: Drawable?,
        val size: Long,
        val isBackedUp: Boolean
    )

    data class SettingsItem(
        val title: String,
        val description: String,
        val iconRes: Int
    )
}
