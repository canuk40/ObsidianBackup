package com.obsidianbackup.benchmarks

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import kotlin.system.measureTimeMillis

@DisplayName("Backup Performance Benchmarks")
class BackupPerformanceBenchmark {
    
    @Test
    @DisplayName("Benchmark: Small file backup")
    fun benchmarkSmallFileBackup() {
        val iterations = 10
        val times = mutableListOf<Long>()
        
        repeat(iterations) {
            val time = measureTimeMillis {
                runBlocking {
                    performMockBackup(fileCount = 100, fileSize = 1024)
                }
            }
            times.add(time)
        }
        
        val avgTime = times.average()
        println("Small File Backup Average: ${avgTime}ms")
    }
    
    @Test
    @DisplayName("Benchmark: Large file backup")
    fun benchmarkLargeFileBackup() {
        val iterations = 5
        val times = mutableListOf<Long>()
        
        repeat(iterations) {
            val time = measureTimeMillis {
                runBlocking {
                    performMockBackup(fileCount = 10, fileSize = 1024 * 1024 * 10)
                }
            }
            times.add(time)
        }
        
        val avgTime = times.average()
        println("Large File Backup Average: ${avgTime}ms")
    }
    
    private suspend fun performMockBackup(fileCount: Int, fileSize: Long) {
        repeat(fileCount) {
            val data = ByteArray(fileSize.toInt()) { it.toByte() }
        }
    }
}
