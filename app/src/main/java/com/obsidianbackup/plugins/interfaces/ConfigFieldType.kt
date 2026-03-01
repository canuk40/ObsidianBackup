// plugins/interfaces/ConfigFieldType.kt
package com.obsidianbackup.plugins.interfaces

/**
 * Configuration field types for plugin configuration schemas
 */
enum class ConfigFieldType {
    STRING,
    PASSWORD,
    NUMBER,
    INTEGER,
    BOOLEAN,
    SELECT,
    MULTI_SELECT,
    STRING_LIST,
    URL
}
