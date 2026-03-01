package com.obsidianbackup.domain.apps

import com.obsidianbackup.logging.ObsidianLogger
import com.obsidianbackup.rootcore.shell.ShellExecutor
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Freeze/Defrost apps — disable packages without uninstalling.
 * Ported from ObsidianBox v31's AppFreezerRepository pattern.
 *
 * Uses two-tier execution: tries non-root `pm disable-user` first,
 * escalates to root `pm disable` only if needed.
 *
 * Protected packages (system critical) are blocked from freezing.
 */
@Singleton
class AppFreezer @Inject constructor(
    private val shellExecutor: ShellExecutor,
    private val logger: ObsidianLogger
) {
    companion object {
        private const val TAG = "AppFreezer"

        private val PROTECTED_PACKAGES = setOf(
            "android",
            "com.android.systemui",
            "com.android.settings",
            "com.android.providers.settings",
            "com.android.providers.contacts",
            "com.android.providers.telephony",
            "com.android.phone",
            "com.android.server.telecom",
            "com.android.launcher3",
            "com.google.android.gms",
            "com.google.android.gsf",
            "com.android.inputmethod.latin",
            "com.android.packageinstaller",
            "com.android.shell"
        )
    }

    suspend fun freeze(packageName: String): FreezeResult {
        if (packageName in PROTECTED_PACKAGES) {
            return FreezeResult.Blocked("$packageName is a protected system package")
        }

        // Tier 1: Try without root
        val nonRootResult = shellExecutor.executeShell("pm disable-user --user 0 $packageName")
        if (nonRootResult.success) {
            Timber.i("$TAG Frozen (non-root): $packageName")
            return FreezeResult.Success(packageName, usedRoot = false)
        }

        // Tier 2: Escalate to root
        val rootResult = shellExecutor.executeRoot("pm disable $packageName")
        return if (rootResult.success) {
            Timber.i("$TAG Frozen (root): $packageName")
            FreezeResult.Success(packageName, usedRoot = true)
        } else {
            FreezeResult.Failed(packageName, rootResult.stderr)
        }
    }

    suspend fun defrost(packageName: String): FreezeResult {
        val nonRootResult = shellExecutor.executeShell("pm enable --user 0 $packageName")
        if (nonRootResult.success) {
            return FreezeResult.Success(packageName, usedRoot = false)
        }

        val rootResult = shellExecutor.executeRoot("pm enable $packageName")
        return if (rootResult.success) {
            FreezeResult.Success(packageName, usedRoot = true)
        } else {
            FreezeResult.Failed(packageName, rootResult.stderr)
        }
    }

    suspend fun isFrozen(packageName: String): Boolean {
        val result = shellExecutor.executeShell("pm list packages -d")
        return result.success && result.stdout.contains("package:$packageName")
    }

    suspend fun freezeBatch(packageNames: List<String>): List<FreezeResult> {
        return packageNames.map { freeze(it) }
    }

    suspend fun defrostBatch(packageNames: List<String>): List<FreezeResult> {
        return packageNames.map { defrost(it) }
    }

    suspend fun listFrozen(): List<String> {
        val result = shellExecutor.executeShell("pm list packages -d")
        if (!result.success) return emptyList()
        return result.stdout.lines()
            .filter { it.startsWith("package:") }
            .map { it.removePrefix("package:").trim() }
    }

    fun isProtected(packageName: String): Boolean = packageName in PROTECTED_PACKAGES

    sealed class FreezeResult {
        data class Success(val packageName: String, val usedRoot: Boolean) : FreezeResult()
        data class Failed(val packageName: String, val error: String) : FreezeResult()
        data class Blocked(val reason: String) : FreezeResult()
    }
}
