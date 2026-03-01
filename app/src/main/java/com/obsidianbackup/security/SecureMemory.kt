package com.obsidianbackup.security

import java.util.Arrays

/**
 * Utilities for secure memory wiping of sensitive data.
 * Prevents sensitive data from remaining in memory after use.
 */
object SecureMemory {
    /**
     * Securely wipe a ByteArray by overwriting with zeros.
     */
    fun wipe(data: ByteArray) {
        Arrays.fill(data, 0.toByte())
    }
    
    /**
     * Securely wipe a CharArray by overwriting with null chars.
     */
    fun wipe(data: CharArray) {
        Arrays.fill(data, '\u0000')
    }
    
    /**
     * Execute block with secure ByteArray, wiping after use.
     */
    inline fun <R> withSecureBytes(size: Int, block: (ByteArray) -> R): R {
        val data = ByteArray(size)
        try {
            return block(data)
        } finally {
            wipe(data)
        }
    }
    
    /**
     * Execute block with secure CharArray, wiping after use.
     */
    inline fun <R> withSecureChars(size: Int, block: (CharArray) -> R): R {
        val data = CharArray(size)
        try {
            return block(data)
        } finally {
            wipe(data)
        }
    }
}
