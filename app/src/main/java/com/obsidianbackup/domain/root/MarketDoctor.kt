package com.obsidianbackup.domain.root

import com.obsidianbackup.rootcore.shell.ShellExecutor
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Market Doctor — repair Play Store app associations and fix common issues.
 *
 * Inspired by Titanium Backup's "Market Doctor" feature.
 * Fixes missing Play Store links, clears bad app states, and re-registers packages.
 */
@Singleton
class MarketDoctor @Inject constructor(
    private val shellExecutor: ShellExecutor
) {
    companion object {
        private const val TAG = "[MarketDoctor]"
        private const val PLAY_STORE_PKG = "com.android.vending"
        private const val PLAY_SERVICES_PKG = "com.google.android.gms"
    }

    data class DiagnosticResult(
        val packageName: String,
        val issues: List<String>,
        val fixed: List<String>
    )

    /**
     * Fix Play Store association for a restored app.
     * After restore, apps may lose their Play Store link. This re-establishes it.
     */
    suspend fun fixPlayStoreLink(packageName: String): DiagnosticResult {
        val issues = mutableListOf<String>()
        val fixed = mutableListOf<String>()

        // Check if the installer is set
        val installerResult = shellExecutor.executeRoot(
            "pm get-install-location $packageName 2>/dev/null || dumpsys package $packageName | grep installerPackageName"
        )

        if (!installerResult.stdout.contains(PLAY_STORE_PKG)) {
            issues.add("Missing Play Store installer association")

            // Set the installer to Play Store
            val setResult = shellExecutor.executeRoot(
                "pm set-installer $packageName $PLAY_STORE_PKG"
            )
            if (setResult.success) {
                fixed.add("Set installer to $PLAY_STORE_PKG")
            }
        }

        // Clear Play Store cache to force refresh
        val clearResult = shellExecutor.executeRoot("pm clear $PLAY_STORE_PKG")
        if (clearResult.success) {
            fixed.add("Cleared Play Store cache")
        }

        Timber.d("$TAG Fixed ${fixed.size} issues for $packageName")
        return DiagnosticResult(packageName, issues, fixed)
    }

    /**
     * Batch fix Play Store links for multiple packages.
     */
    suspend fun batchFixPlayStoreLinks(packageNames: List<String>): List<DiagnosticResult> {
        return packageNames.map { fixPlayStoreLink(it) }
    }

    /**
     * Fix package verification state (useful after restoring from a different device).
     */
    suspend fun fixVerificationState(packageName: String): Boolean {
        val result = shellExecutor.executeRoot(
            "pm set-installer $packageName $PLAY_STORE_PKG && " +
                "am broadcast -a android.intent.action.PACKAGE_CHANGED -d package:$packageName"
        )
        return result.success
    }

    /**
     * Clear bad app states that can prevent updates.
     * Resets stopped state, clears default handler, fixes permissions.
     */
    suspend fun clearBadState(packageName: String): DiagnosticResult {
        val issues = mutableListOf<String>()
        val fixed = mutableListOf<String>()

        // Clear stopped state
        val unstop = shellExecutor.executeRoot("am unstop $packageName 2>/dev/null || cmd package unstop $packageName 2>/dev/null")
        if (unstop.success) {
            fixed.add("Cleared stopped state")
        }

        // Clear default handler if set
        val clearDefaults = shellExecutor.executeRoot("pm clear-default-browser $packageName 2>/dev/null")
        if (clearDefaults.success) {
            fixed.add("Cleared default handler")
        }

        // Re-enable if disabled
        val enable = shellExecutor.executeRoot("pm enable $packageName")
        if (enable.success) {
            fixed.add("Enabled package")
        }

        // Grant missing permissions silently
        val permsResult = shellExecutor.executeRoot("dumpsys package $packageName | grep 'requested permissions' -A 100 | grep 'android.permission' | head -20")
        if (permsResult.success) {
            for (line in permsResult.stdout.lines()) {
                val perm = line.trim().removePrefix("android.permission.")
                if (perm.isNotBlank() && !perm.contains(":")) {
                    shellExecutor.executeRoot("pm grant $packageName android.permission.${perm.trim()}")
                }
            }
            fixed.add("Attempted permission grants")
        }

        return DiagnosticResult(packageName, issues, fixed)
    }

    /**
     * Full diagnostic scan — checks Play Store, GMS, and package health.
     */
    suspend fun fullDiagnostic(packageName: String): DiagnosticResult {
        val issues = mutableListOf<String>()
        val fixed = mutableListOf<String>()

        // Check Play Store is installed
        val storeCheck = shellExecutor.executeRoot("pm path $PLAY_STORE_PKG")
        if (!storeCheck.success) {
            issues.add("Play Store not installed")
        }

        // Check GMS
        val gmsCheck = shellExecutor.executeRoot("pm path $PLAY_SERVICES_PKG")
        if (!gmsCheck.success) {
            issues.add("Google Play Services not installed")
        }

        // Check package exists
        val pkgCheck = shellExecutor.executeRoot("pm path $packageName")
        if (!pkgCheck.success) {
            issues.add("Package not installed")
            return DiagnosticResult(packageName, issues, fixed)
        }

        // Check for installer
        val dumpResult = shellExecutor.executeRoot("dumpsys package $packageName | grep -E 'installer|firstInstall|lastUpdate'")
        if (dumpResult.success) {
            if (!dumpResult.stdout.contains("com.android.vending")) {
                issues.add("Not installed via Play Store")
            }
        }

        // Fix whatever we can
        if (issues.isNotEmpty()) {
            val fixResult = fixPlayStoreLink(packageName)
            fixed.addAll(fixResult.fixed)
        }

        return DiagnosticResult(packageName, issues, fixed)
    }
}
