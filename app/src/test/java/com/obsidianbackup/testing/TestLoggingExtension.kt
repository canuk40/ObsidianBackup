package com.obsidianbackup.testing

import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

/**
 * JUnit extension for test logging.
 */
class TestLoggingExtension : BeforeEachCallback, AfterEachCallback {
    
    override fun beforeEach(context: ExtensionContext) {
        val testName = context.displayName
        println("Starting test: $testName")
    }
    
    override fun afterEach(context: ExtensionContext) {
        val testName = context.displayName
        val result = if (context.executionException.isPresent) "FAILED" else "PASSED"
        println("Finished test: $testName - $result")
    }
}
