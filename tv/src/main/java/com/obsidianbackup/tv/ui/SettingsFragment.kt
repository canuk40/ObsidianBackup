package com.obsidianbackup.tv.ui

import android.os.Bundle
import androidx.leanback.preference.LeanbackPreferenceFragmentCompat
import androidx.leanback.preference.LeanbackSettingsFragmentCompat
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceScreen
import com.obsidianbackup.tv.R

class SettingsFragment : LeanbackSettingsFragmentCompat() {
    override fun onPreferenceStartInitialScreen() {
        startPreferenceFragment(PrefsFragment())
    }

    override fun onPreferenceStartFragment(
        caller: PreferenceFragmentCompat,
        pref: Preference
    ): Boolean {
        return false
    }

    override fun onPreferenceStartScreen(
        caller: PreferenceFragmentCompat,
        pref: PreferenceScreen
    ): Boolean {
        val fragment = PrefsFragment()
        fragment.arguments = Bundle().apply {
            putString(PreferenceFragmentCompat.ARG_PREFERENCE_ROOT, pref.key)
        }
        startPreferenceFragment(fragment)
        return true
    }

    class PrefsFragment : LeanbackPreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.tv_preferences, rootKey)
        }
    }
}
