package com.obsidianbackup.testing

import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode

/**
 * Base test class with common configuration.
 */
@ExtendWith(TestLoggingExtension::class)
@Execution(ExecutionMode.CONCURRENT)
abstract class BaseTest {
    
    companion object {
        init {
            // Global test configuration
            System.setProperty("junit.jupiter.execution.parallel.enabled", "true")
            System.setProperty("junit.jupiter.execution.parallel.mode.default", "concurrent")
        }
    }
}
