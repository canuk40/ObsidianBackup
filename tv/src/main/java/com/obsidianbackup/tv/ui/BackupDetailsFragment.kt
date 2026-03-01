package com.obsidianbackup.tv.ui

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.leanback.app.DetailsSupportFragment
import androidx.leanback.widget.*
import com.obsidianbackup.tv.R
import com.obsidianbackup.tv.backup.TVBackupManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class BackupDetailsFragment : DetailsSupportFragment() {

    @Inject
    lateinit var backupManager: TVBackupManager

    private lateinit var detailsAdapter: ArrayObjectAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val packageName = arguments?.getString("package_name") ?: ""
        val appName = arguments?.getString("app_name") ?: ""

        setupAdapter()
        setupDetailsOverviewRow(packageName, appName)
        setupRelatedContent()
    }

    private fun setupAdapter() {
        val presenterSelector = ClassPresenterSelector()
        presenterSelector.addClassPresenter(
            DetailsOverviewRow::class.java,
            DetailsOverviewRowPresenter(DescriptionPresenter())
        )
        presenterSelector.addClassPresenter(
            ListRow::class.java,
            ListRowPresenter()
        )

        detailsAdapter = ArrayObjectAdapter(presenterSelector)
        adapter = detailsAdapter
    }

    private fun setupDetailsOverviewRow(packageName: String, appName: String) {
        val row = DetailsOverviewRow(AppDetails(packageName, appName))
        
        val icon = try {
            context?.packageManager?.getApplicationIcon(packageName)
        } catch (e: Exception) {
            ContextCompat.getDrawable(requireContext(), R.drawable.ic_app_default)
        }
        
        row.imageDrawable = icon

        val actionsAdapter = ArrayObjectAdapter()
        actionsAdapter.add(Action(ACTION_BACKUP, "Backup Now"))
        actionsAdapter.add(Action(ACTION_RESTORE, "Restore"))
        actionsAdapter.add(Action(ACTION_DELETE, "Delete Backup"))
        row.actionsAdapter = actionsAdapter

        detailsAdapter.add(row)
    }

    private fun setupRelatedContent() {
        val header = HeaderItem(0, "Backup History")
        val listRowAdapter = ArrayObjectAdapter(BackupItemPresenter())
        
        // Add backup history items
        listRowAdapter.add(BackupItem("Full Backup", "2024-01-15 14:30", "125 MB"))
        listRowAdapter.add(BackupItem("Full Backup", "2024-01-10 10:15", "120 MB"))
        
        detailsAdapter.add(ListRow(header, listRowAdapter))
    }

    inner class DescriptionPresenter : AbstractDetailsDescriptionPresenter() {
        override fun onBindDescription(vh: ViewHolder, item: Any) {
            val details = item as AppDetails
            vh.title.text = details.name
            vh.subtitle.text = details.packageName
            vh.body.text = "Backup and restore this TV app's data, settings, and configuration."
        }
    }

    class BackupItemPresenter : Presenter() {
        override fun onCreateViewHolder(parent: android.view.ViewGroup): ViewHolder {
            val view = android.view.LayoutInflater.from(parent.context)
                .inflate(R.layout.item_backup_history, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(viewHolder: ViewHolder, item: Any) {
            val backup = item as BackupItem
            val titleView = viewHolder.view.findViewById<android.widget.TextView>(R.id.backup_type)
            val dateView = viewHolder.view.findViewById<android.widget.TextView>(R.id.backup_date)
            val sizeView = viewHolder.view.findViewById<android.widget.TextView>(R.id.backup_size)
            
            titleView.text = backup.type
            dateView.text = backup.date
            sizeView.text = backup.size
        }

        override fun onUnbindViewHolder(viewHolder: ViewHolder) {}
    }

    data class AppDetails(val packageName: String, val name: String)
    data class BackupItem(val type: String, val date: String, val size: String)

    companion object {
        private const val ACTION_BACKUP = 1L
        private const val ACTION_RESTORE = 2L
        private const val ACTION_DELETE = 3L
    }
}
