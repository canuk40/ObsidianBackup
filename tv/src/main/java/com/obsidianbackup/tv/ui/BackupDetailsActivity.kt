package com.obsidianbackup.tv.ui

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import com.obsidianbackup.tv.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BackupDetailsActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_backup_details)

        val packageName = intent.getStringExtra("package_name")
        val appName = intent.getStringExtra("app_name")

        if (savedInstanceState == null) {
            val fragment = BackupDetailsFragment().apply {
                arguments = Bundle().apply {
                    putString("package_name", packageName)
                    putString("app_name", appName)
                }
            }
            supportFragmentManager.beginTransaction()
                .replace(R.id.details_fragment, fragment)
                .commitNow()
        }
    }
}
