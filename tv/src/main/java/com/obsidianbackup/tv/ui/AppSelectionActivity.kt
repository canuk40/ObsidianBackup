package com.obsidianbackup.tv.ui

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import com.obsidianbackup.tv.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AppSelectionActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_selection)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.app_selection_fragment, AppSelectionFragment())
                .commitNow()
        }
    }
}
