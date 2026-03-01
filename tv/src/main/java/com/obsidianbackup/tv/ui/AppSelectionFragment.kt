package com.obsidianbackup.tv.ui

import android.os.Bundle
import androidx.leanback.app.VerticalGridSupportFragment
import androidx.leanback.widget.*
import com.obsidianbackup.tv.backup.TVBackupManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AppSelectionFragment : VerticalGridSupportFragment() {

    @Inject
    lateinit var backupManager: TVBackupManager

    private lateinit var gridAdapter: ArrayObjectAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        title = "Select Apps to Backup"
        
        setupGridPresenter()
        loadApps()
        setupClickListener()
    }

    private fun setupGridPresenter() {
        val gridPresenter = VerticalGridPresenter()
        gridPresenter.numberOfColumns = 4
        setGridPresenter(gridPresenter)

        gridAdapter = ArrayObjectAdapter(TVAppCardPresenter())
        adapter = gridAdapter
    }

    private fun loadApps() {
        val allApps = backupManager.getAllTVApps()
        allApps.forEach { app ->
            gridAdapter.add(MainFragment.TVAppCard(
                name = app.name,
                packageName = app.packageName,
                icon = app.icon,
                size = app.size,
                isBackedUp = app.isBackedUp
            ))
        }
    }

    private fun setupClickListener() {
        onItemViewClickedListener = OnItemViewClickedListener { _, item, _, _ ->
            val app = item as? MainFragment.TVAppCard
            app?.let {
                backupManager.toggleAppSelection(it.packageName)
                // Refresh the grid
                gridAdapter.notifyArrayItemRangeChanged(0, gridAdapter.size())
            }
        }
    }
}
