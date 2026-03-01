package com.obsidianbackup.tv.ui

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import com.obsidianbackup.tv.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingsActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val settingType = intent.getStringExtra("setting_type")

        if (savedInstanceState == null) {
            val fragment = SettingsFragment().apply {
                arguments = Bundle().apply {
                    putString("setting_type", settingType)
                }
            }
            supportFragmentManager.beginTransaction()
                .replace(R.id.settings_fragment, fragment)
                .commitNow()
        }
    }
}
