// security/SecureDatabaseHelper.kt
package com.obsidianbackup.security

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.obsidianbackup.logging.ObsidianLogger
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

/**
 * Secure Database Helper for SQL Injection Prevention
 * Implements OWASP MASVS-STORAGE and MASVS-CODE requirements
 * 
 * Features:
 * - SQL injection prevention through parameterized queries
 * - Database encryption with SQLCipher
 * - Prepared statement validation
 * - Query sanitization
 */
class SecureDatabaseHelper(
    private val context: Context,
    private val secureStorage: SecureStorageManager,
    private val logger: ObsidianLogger
) {
    companion object {
        private const val TAG = "SecureDatabase"
        private const val DB_PASSPHRASE_KEY = "db_passphrase"
        
        // SQL injection patterns to detect and block
        private val SQL_INJECTION_PATTERNS = listOf(
            Regex("('.+--)|(--)|(\\|\\|)", RegexOption.IGNORE_CASE),
            Regex("('.+(\\/\\*|\\*\\/))|((\\*\\/))", RegexOption.IGNORE_CASE),
            Regex("('.+\\bexec(\\s|\\+)+(s|x)p\\w+)", RegexOption.IGNORE_CASE),
            Regex("\\bselect\\b.+\\bfrom\\b", RegexOption.IGNORE_CASE),
            Regex("\\bunion\\b.+\\bselect\\b", RegexOption.IGNORE_CASE),
            Regex("\\binsert\\b.+\\binto\\b", RegexOption.IGNORE_CASE),
            Regex("\\bupdate\\b.+\\bset\\b", RegexOption.IGNORE_CASE),
            Regex("\\bdelete\\b.+\\bfrom\\b", RegexOption.IGNORE_CASE),
            Regex("\\bdrop\\b.+\\btable\\b", RegexOption.IGNORE_CASE),
            Regex("\\balter\\b.+\\btable\\b", RegexOption.IGNORE_CASE),
            Regex("\\btruncate\\b", RegexOption.IGNORE_CASE),
            Regex("\\bexec(ute)?\\b", RegexOption.IGNORE_CASE),
            Regex("\\bscript\\b", RegexOption.IGNORE_CASE),
            Regex("<script", RegexOption.IGNORE_CASE),
            Regex("javascript:", RegexOption.IGNORE_CASE),
            Regex("onerror\\s*=", RegexOption.IGNORE_CASE),
            Regex("onload\\s*=", RegexOption.IGNORE_CASE)
        )
    }
    
    /**
     * Create an encrypted Room database instance
     */
    fun <T : RoomDatabase> createEncryptedDatabase(
        databaseClass: Class<T>,
        databaseName: String
    ): T {
        val passphrase = getOrCreateDatabasePassphrase()
        try {
            val factory = SupportOpenHelperFactory(String(passphrase).toByteArray(Charsets.UTF_8))
            
            return Room.databaseBuilder(
                context,
                databaseClass,
                databaseName
            )
                .openHelperFactory(factory)
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        logger.i(TAG, "Encrypted database created: $databaseName")
                    }
                    
                    override fun onOpen(db: SupportSQLiteDatabase) {
                        super.onOpen(db)
                        // Enable foreign keys
                        db.execSQL("PRAGMA foreign_keys=ON;")
                    }
                })
                .build()
        } finally {
            SecureMemory.wipe(passphrase)
        }
    }
    
    /**
     * Get or create database passphrase from secure storage
     */
    private fun getOrCreateDatabasePassphrase(): CharArray {
        var passphrase = secureStorage.getString(DB_PASSPHRASE_KEY)
        
        if (passphrase == null) {
            // Generate a new random passphrase
            passphrase = generateSecurePassphrase()
            secureStorage.putString(DB_PASSPHRASE_KEY, passphrase)
            logger.i(TAG, "Generated new database passphrase")
        }
        
        val passphraseChars = passphrase.toCharArray()
        try {
            return passphraseChars
        } finally {
            // Note: Caller is responsible for wiping the returned CharArray
            // This wipe only clears the intermediate conversion
        }
    }
    
    /**
     * Generate a cryptographically secure passphrase
     */
    private fun generateSecurePassphrase(): String {
        val random = java.security.SecureRandom()
        val bytes = ByteArray(32)
        try {
            random.nextBytes(bytes)
            return android.util.Base64.encodeToString(
                bytes,
                android.util.Base64.NO_WRAP or android.util.Base64.NO_PADDING
            )
        } finally {
            SecureMemory.wipe(bytes)
        }
    }
    
    /**
     * Validate user input for SQL injection attempts
     * Returns sanitized input or throws exception
     */
    fun validateAndSanitizeInput(input: String, fieldName: String = "input"): String {
        // Check for SQL injection patterns
        for (pattern in SQL_INJECTION_PATTERNS) {
            if (pattern.containsMatchIn(input)) {
                logger.w(TAG, "SQL injection attempt detected in $fieldName: $input")
                throw SecurityException(
                    "Invalid input detected in $fieldName. Special SQL characters are not allowed."
                )
            }
        }
        
        // Additional sanitization
        return sanitizeString(input)
    }
    
    /**
     * Sanitize string input to prevent SQL injection
     */
    private fun sanitizeString(input: String): String {
        // Escape single quotes (though parameterized queries should handle this)
        return input.replace("'", "''")
    }
    
    /**
     * Validate search query input
     */
    fun validateSearchQuery(query: String): String {
        if (query.length > 500) {
            throw SecurityException("Search query too long")
        }
        
        // Allow alphanumeric, spaces, and basic punctuation
        val allowedPattern = Regex("[a-zA-Z0-9\\s._@-]+")
        if (!allowedPattern.matches(query)) {
            throw SecurityException("Search query contains invalid characters")
        }
        
        return query
    }
    
    /**
     * Validate package name (used in backup queries)
     */
    fun validatePackageName(packageName: String): String {
        // Package names follow a strict format
        val packagePattern = Regex("^[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)+\$")
        if (!packagePattern.matches(packageName)) {
            throw SecurityException("Invalid package name format: $packageName")
        }
        
        return packageName
    }
    
    /**
     * Validate snapshot ID (UUID format)
     */
    fun validateSnapshotId(snapshotId: String): String {
        val uuidPattern = Regex(
            "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\$",
            RegexOption.IGNORE_CASE
        )
        if (!uuidPattern.matches(snapshotId)) {
            throw SecurityException("Invalid snapshot ID format: $snapshotId")
        }
        
        return snapshotId
    }
    
    /**
     * Validate file path to prevent directory traversal
     */
    fun validateFilePath(path: String): String {
        if (path.contains("..") || path.contains("~")) {
            throw SecurityException("Path traversal attempt detected: $path")
        }
        
        // Ensure path doesn't try to escape app directories
        val canonicalPath = java.io.File(path).canonicalPath
        val appDir = context.filesDir.canonicalPath
        val cacheDir = context.cacheDir.canonicalPath
        val externalDir = context.getExternalFilesDir(null)?.canonicalPath
        
        val isValid = canonicalPath.startsWith(appDir) ||
                     canonicalPath.startsWith(cacheDir) ||
                     (externalDir != null && canonicalPath.startsWith(externalDir))
        
        if (!isValid) {
            throw SecurityException("Invalid file path - outside app directories: $path")
        }
        
        return path
    }
    
    /**
     * Create a safe LIKE query pattern
     * Escapes special SQL LIKE characters
     */
    fun createSafeLikePattern(input: String): String {
        // Escape SQL LIKE special characters
        return input
            .replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_")
            .replace("[", "\\[")
    }
    
    /**
     * Validate URL input
     */
    fun validateUrl(url: String): String {
        try {
            val uri = java.net.URI(url)
            
            // Only allow HTTPS (except for localhost in debug)
            if (uri.scheme != "https") {
                if (uri.host != "localhost" && uri.host != "127.0.0.1") {
                    throw SecurityException("Only HTTPS URLs are allowed")
                }
            }
            
            return url
        } catch (e: Exception) {
            throw SecurityException("Invalid URL format: $url", e)
        }
    }
    
    /**
     * Validate email address
     */
    fun validateEmail(email: String): String {
        val emailPattern = Regex(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\$"
        )
        if (!emailPattern.matches(email)) {
            throw SecurityException("Invalid email format: $email")
        }
        
        return email
    }
    
    /**
     * Safe query builder for common queries
     */
    class SafeQueryBuilder {
        private val params = mutableListOf<Any>()
        private val whereClauses = mutableListOf<String>()
        
        fun addWhereClause(column: String, value: Any): SafeQueryBuilder {
            // Validate column name (no special characters)
            if (!Regex("^[a-zA-Z_][a-zA-Z0-9_]*\$").matches(column)) {
                throw SecurityException("Invalid column name: $column")
            }
            
            whereClauses.add("$column = ?")
            params.add(value)
            return this
        }
        
        fun addLikeClause(column: String, pattern: String): SafeQueryBuilder {
            if (!Regex("^[a-zA-Z_][a-zA-Z0-9_]*\$").matches(column)) {
                throw SecurityException("Invalid column name: $column")
            }
            
            whereClauses.add("$column LIKE ? ESCAPE '\\'")
            params.add(pattern)
            return this
        }
        
        fun build(): Pair<String, Array<String>> {
            val whereClause = if (whereClauses.isEmpty()) {
                ""
            } else {
                "WHERE " + whereClauses.joinToString(" AND ")
            }
            
            return Pair(whereClause, params.map { it.toString() }.toTypedArray())
        }
    }
    
    /**
     * Log potentially suspicious database operations
     */
    fun logSuspiciousOperation(operation: String, details: String) {
        logger.w(TAG, "Suspicious database operation: $operation - $details")
        
        // In production, you might want to:
        // - Send to security monitoring system
        // - Alert administrators
        // - Rate limit or block the source
    }
}

/**
 * Extension functions for Room DAOs to add validation
 */
fun String.validateAsPackageName(): String {
    val packagePattern = Regex("^[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)+\$")
    if (!packagePattern.matches(this)) {
        throw SecurityException("Invalid package name format")
    }
    return this
}

fun String.validateAsSnapshotId(): String {
    val uuidPattern = Regex(
        "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\$",
        RegexOption.IGNORE_CASE
    )
    if (!uuidPattern.matches(this)) {
        throw SecurityException("Invalid snapshot ID format")
    }
    return this
}
