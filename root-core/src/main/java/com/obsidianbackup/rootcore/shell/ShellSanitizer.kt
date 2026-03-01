package com.obsidianbackup.rootcore.shell

/**
 * Utility for sanitizing user input before passing to shell commands.
 * Prevents shell injection attacks in root command execution.
 *
 * Ported from ObsidianBox v31 production code.
 */
object ShellSanitizer {

    private val SHELL_METACHARACTERS = setOf(
        '`', '$', '\\', '"', '\'', '|', ';', '&', '>', '<',
        '(', ')', '{', '}', '[', ']', '!', '#', '~', '*', '?', '\n', '\r'
    )

    /**
     * Escapes shell metacharacters by wrapping in single quotes.
     * Single quotes within the input are handled by ending the quote,
     * adding an escaped single quote, and re-opening the quote.
     */
    fun sanitizeShellArg(input: String): String {
        if (input.isEmpty()) return "''"
        val escaped = input.replace("'", "'\\''")
        return "'$escaped'"
    }

    /**
     * Validates an Android package name (e.g., com.example.app).
     */
    fun validatePackageName(pkg: String): Boolean {
        if (pkg.isBlank() || pkg.length > 256) return false
        return pkg.matches(Regex("^[a-zA-Z][a-zA-Z0-9_.]*$"))
    }

    /**
     * Validates a filename for safe use in shell commands.
     * Blocks path traversal (..), absolute paths, null bytes, and shell metacharacters.
     */
    fun validateFilename(name: String): Boolean {
        if (name.isBlank() || name.length > 255) return false
        if (name.contains("/") || name.contains("..")) return false
        if (name.contains("\u0000")) return false
        return name.none { it in SHELL_METACHARACTERS }
    }

    /**
     * Validates a sysctl-style key (e.g., vm.swappiness).
     */
    fun validateSysctlKey(key: String): Boolean {
        if (key.isBlank() || key.length > 128) return false
        return key.matches(Regex("^[a-z][a-z0-9_.]*$"))
    }

    /**
     * Validates a build.prop key (e.g., ro.build.display.id).
     */
    fun validateBuildPropKey(key: String): Boolean {
        if (key.isBlank() || key.length > 256) return false
        return key.matches(Regex("^[a-zA-Z][a-zA-Z0-9_.\\-]*$"))
    }

    /**
     * Validates a Magisk module ID (alphanumeric, hyphens, underscores).
     */
    fun validateModuleId(moduleId: String): Boolean {
        if (moduleId.isBlank() || moduleId.length > 128) return false
        return moduleId.matches(Regex("^[a-zA-Z0-9][a-zA-Z0-9_\\-]*$"))
    }

    /**
     * Checks if a string contains any shell metacharacters.
     */
    fun containsShellMetachars(input: String): Boolean {
        return input.any { it in SHELL_METACHARACTERS }
    }
}
