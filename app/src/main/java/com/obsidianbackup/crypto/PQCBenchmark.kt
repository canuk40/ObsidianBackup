// crypto/PQCBenchmark.kt
package com.obsidianbackup.crypto

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.SecureRandom
import kotlin.system.measureTimeMillis

/**
 * Performance Benchmarking for Post-Quantum Cryptography
 * 
 * Measures and compares performance of:
 * - Classical algorithms (ECDH, ECDSA, RSA)
 * - Post-quantum algorithms (ML-KEM, ML-DSA, SLH-DSA)
 * - Hybrid algorithms
 * 
 * Metrics:
 * - Key generation time
 * - Encapsulation/Decapsulation time
 * - Signing/Verification time
 * - Throughput (operations per second)
 * - Memory usage (key sizes, signature sizes)
 */
class PQCBenchmark(
    private val provider: PostQuantumCrypto.CryptoProvider
) {
    companion object {
        private const val TAG = "PQCBenchmark"
        private const val DEFAULT_ITERATIONS = 100
        private const val WARMUP_ITERATIONS = 10
    }

    /**
     * Benchmark result for a single operation
     */
    data class BenchmarkResult(
        val algorithmName: String,
        val operationType: String,
        val iterations: Int,
        val totalTimeMs: Long,
        val avgTimeMs: Double,
        val minTimeMs: Long,
        val maxTimeMs: Long,
        val opsPerSecond: Double,
        val memoryUsage: MemoryUsage
    ) {
        override fun toString(): String {
            return """
                |Algorithm: $algorithmName
                |Operation: $operationType
                |Iterations: $iterations
                |Total Time: ${totalTimeMs}ms
                |Average Time: ${"%.2f".format(avgTimeMs)}ms
                |Min Time: ${minTimeMs}ms
                |Max Time: ${maxTimeMs}ms
                |Throughput: ${"%.2f".format(opsPerSecond)} ops/sec
                |Memory: ${memoryUsage.totalBytes} bytes (pub: ${memoryUsage.publicKeySize}, priv: ${memoryUsage.privateKeySize}, sig/ct: ${memoryUsage.signatureOrCiphertextSize})
            """.trimMargin()
        }
    }

    /**
     * Memory usage metrics
     */
    data class MemoryUsage(
        val publicKeySize: Int,
        val privateKeySize: Int,
        val signatureOrCiphertextSize: Int
    ) {
        val totalBytes = publicKeySize + privateKeySize + signatureOrCiphertextSize
    }

    /**
     * Comprehensive benchmark suite result
     */
    data class BenchmarkSuite(
        val results: List<BenchmarkResult>,
        val timestamp: Long = System.currentTimeMillis()
    ) {
        fun printSummary() {
            println("\n" + "=".repeat(80))
            println("Post-Quantum Cryptography Benchmark Results")
            println("Timestamp: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(timestamp)}")
            println("=".repeat(80))
            
            results.forEach { result ->
                println("\n${result}")
                println("-".repeat(80))
            }
            
            println("\n" + "=".repeat(80))
            println("Comparison Summary")
            println("=".repeat(80))
            
            // Group by operation type
            results.groupBy { it.operationType }.forEach { (opType, results) ->
                println("\n$opType:")
                results.sortedBy { it.avgTimeMs }.forEach { result ->
                    println("  ${result.algorithmName.padEnd(30)} - ${"%.2f".format(result.avgTimeMs)}ms avg, ${"%.2f".format(result.opsPerSecond)} ops/sec")
                }
            }
        }

        fun toMarkdown(): String {
            val sb = StringBuilder()
            sb.appendLine("# Post-Quantum Cryptography Benchmark Results")
            sb.appendLine()
            sb.appendLine("**Timestamp:** ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(timestamp)}")
            sb.appendLine()
            
            // Group by operation type
            results.groupBy { it.operationType }.forEach { (opType, results) ->
                sb.appendLine("## $opType")
                sb.appendLine()
                sb.appendLine("| Algorithm | Avg Time (ms) | Min (ms) | Max (ms) | Ops/sec | Memory (bytes) |")
                sb.appendLine("|-----------|---------------|----------|----------|---------|----------------|")
                
                results.sortedBy { it.avgTimeMs }.forEach { result ->
                    sb.appendLine("| ${result.algorithmName} | ${"%.2f".format(result.avgTimeMs)} | ${result.minTimeMs} | ${result.maxTimeMs} | ${"%.2f".format(result.opsPerSecond)} | ${result.memoryUsage.totalBytes} |")
                }
                sb.appendLine()
            }
            
            return sb.toString()
        }
    }

    /**
     * Benchmark KEM key generation
     */
    suspend fun benchmarkKEMKeyGen(
        algorithm: PostQuantumCrypto.KEMAlgorithm,
        iterations: Int = DEFAULT_ITERATIONS
    ): BenchmarkResult = withContext(Dispatchers.Default) {
        Log.i(TAG, "Benchmarking ${algorithm.algorithmName} key generation ($iterations iterations)")
        
        // Warmup
        repeat(WARMUP_ITERATIONS) {
            provider.generateKEMKeyPair(algorithm)
        }
        
        val times = mutableListOf<Long>()
        var lastKeyPair: java.security.KeyPair? = null
        
        val totalTime = measureTimeMillis {
            repeat(iterations) {
                val time = measureTimeMillis {
                    lastKeyPair = provider.generateKEMKeyPair(algorithm)
                }
                times.add(time)
            }
        }
        
        val memoryUsage = MemoryUsage(
            publicKeySize = lastKeyPair?.public?.encoded?.size ?: algorithm.publicKeySize,
            privateKeySize = lastKeyPair?.private?.encoded?.size ?: algorithm.secretKeySize,
            signatureOrCiphertextSize = algorithm.ciphertextSize
        )
        
        BenchmarkResult(
            algorithmName = algorithm.algorithmName,
            operationType = "Key Generation",
            iterations = iterations,
            totalTimeMs = totalTime,
            avgTimeMs = times.average(),
            minTimeMs = times.minOrNull() ?: 0,
            maxTimeMs = times.maxOrNull() ?: 0,
            opsPerSecond = iterations / (totalTime / 1000.0),
            memoryUsage = memoryUsage
        )
    }

    /**
     * Benchmark KEM encapsulation
     */
    suspend fun benchmarkKEMEncapsulation(
        algorithm: PostQuantumCrypto.KEMAlgorithm,
        iterations: Int = DEFAULT_ITERATIONS
    ): BenchmarkResult = withContext(Dispatchers.Default) {
        Log.i(TAG, "Benchmarking ${algorithm.algorithmName} encapsulation ($iterations iterations)")
        
        // Generate key pair once
        val keyPair = provider.generateKEMKeyPair(algorithm)
        
        // Warmup
        repeat(WARMUP_ITERATIONS) {
            provider.encapsulate(keyPair.public, algorithm)
        }
        
        val times = mutableListOf<Long>()
        var lastResult: PostQuantumCrypto.EncapsulationResult? = null
        
        val totalTime = measureTimeMillis {
            repeat(iterations) {
                val time = measureTimeMillis {
                    lastResult = provider.encapsulate(keyPair.public, algorithm)
                }
                times.add(time)
            }
        }
        
        val memoryUsage = MemoryUsage(
            publicKeySize = keyPair.public.encoded.size,
            privateKeySize = keyPair.private.encoded.size,
            signatureOrCiphertextSize = lastResult?.ciphertext?.size ?: algorithm.ciphertextSize
        )
        
        BenchmarkResult(
            algorithmName = algorithm.algorithmName,
            operationType = "Encapsulation",
            iterations = iterations,
            totalTimeMs = totalTime,
            avgTimeMs = times.average(),
            minTimeMs = times.minOrNull() ?: 0,
            maxTimeMs = times.maxOrNull() ?: 0,
            opsPerSecond = iterations / (totalTime / 1000.0),
            memoryUsage = memoryUsage
        )
    }

    /**
     * Benchmark KEM decapsulation
     */
    suspend fun benchmarkKEMDecapsulation(
        algorithm: PostQuantumCrypto.KEMAlgorithm,
        iterations: Int = DEFAULT_ITERATIONS
    ): BenchmarkResult = withContext(Dispatchers.Default) {
        Log.i(TAG, "Benchmarking ${algorithm.algorithmName} decapsulation ($iterations iterations)")
        
        // Generate key pair and encapsulation once
        val keyPair = provider.generateKEMKeyPair(algorithm)
        val encapResult = provider.encapsulate(keyPair.public, algorithm)
        
        // Warmup
        repeat(WARMUP_ITERATIONS) {
            provider.decapsulate(encapResult.ciphertext, keyPair.private, algorithm)
        }
        
        val times = mutableListOf<Long>()
        
        val totalTime = measureTimeMillis {
            repeat(iterations) {
                val time = measureTimeMillis {
                    provider.decapsulate(encapResult.ciphertext, keyPair.private, algorithm)
                }
                times.add(time)
            }
        }
        
        val memoryUsage = MemoryUsage(
            publicKeySize = keyPair.public.encoded.size,
            privateKeySize = keyPair.private.encoded.size,
            signatureOrCiphertextSize = encapResult.ciphertext.size
        )
        
        BenchmarkResult(
            algorithmName = algorithm.algorithmName,
            operationType = "Decapsulation",
            iterations = iterations,
            totalTimeMs = totalTime,
            avgTimeMs = times.average(),
            minTimeMs = times.minOrNull() ?: 0,
            maxTimeMs = times.maxOrNull() ?: 0,
            opsPerSecond = iterations / (totalTime / 1000.0),
            memoryUsage = memoryUsage
        )
    }

    /**
     * Benchmark signature key generation
     */
    suspend fun benchmarkSignatureKeyGen(
        algorithm: PostQuantumCrypto.SignatureAlgorithm,
        iterations: Int = DEFAULT_ITERATIONS
    ): BenchmarkResult = withContext(Dispatchers.Default) {
        Log.i(TAG, "Benchmarking ${algorithm.algorithmName} signature key generation ($iterations iterations)")
        
        // Warmup
        repeat(WARMUP_ITERATIONS) {
            provider.generateSignatureKeyPair(algorithm)
        }
        
        val times = mutableListOf<Long>()
        var lastKeyPair: java.security.KeyPair? = null
        
        val totalTime = measureTimeMillis {
            repeat(iterations) {
                val time = measureTimeMillis {
                    lastKeyPair = provider.generateSignatureKeyPair(algorithm)
                }
                times.add(time)
            }
        }
        
        val memoryUsage = MemoryUsage(
            publicKeySize = lastKeyPair?.public?.encoded?.size ?: algorithm.publicKeySize,
            privateKeySize = lastKeyPair?.private?.encoded?.size ?: algorithm.secretKeySize,
            signatureOrCiphertextSize = algorithm.signatureSize
        )
        
        BenchmarkResult(
            algorithmName = algorithm.algorithmName,
            operationType = "Signature Key Generation",
            iterations = iterations,
            totalTimeMs = totalTime,
            avgTimeMs = times.average(),
            minTimeMs = times.minOrNull() ?: 0,
            maxTimeMs = times.maxOrNull() ?: 0,
            opsPerSecond = iterations / (totalTime / 1000.0),
            memoryUsage = memoryUsage
        )
    }

    /**
     * Benchmark signing operation
     */
    suspend fun benchmarkSigning(
        algorithm: PostQuantumCrypto.SignatureAlgorithm,
        dataSize: Int = 1024,
        iterations: Int = DEFAULT_ITERATIONS
    ): BenchmarkResult = withContext(Dispatchers.Default) {
        Log.i(TAG, "Benchmarking ${algorithm.algorithmName} signing ($iterations iterations, $dataSize bytes)")
        
        // Generate key pair and test data
        val keyPair = provider.generateSignatureKeyPair(algorithm)
        val data = ByteArray(dataSize)
        SecureRandom().nextBytes(data)
        
        // Warmup
        repeat(WARMUP_ITERATIONS) {
            provider.sign(data, keyPair.private, algorithm)
        }
        
        val times = mutableListOf<Long>()
        var lastSignature: ByteArray? = null
        
        val totalTime = measureTimeMillis {
            repeat(iterations) {
                val time = measureTimeMillis {
                    lastSignature = provider.sign(data, keyPair.private, algorithm)
                }
                times.add(time)
            }
        }
        
        val memoryUsage = MemoryUsage(
            publicKeySize = keyPair.public.encoded.size,
            privateKeySize = keyPair.private.encoded.size,
            signatureOrCiphertextSize = lastSignature?.size ?: algorithm.signatureSize
        )
        
        BenchmarkResult(
            algorithmName = algorithm.algorithmName,
            operationType = "Signing (${dataSize}B data)",
            iterations = iterations,
            totalTimeMs = totalTime,
            avgTimeMs = times.average(),
            minTimeMs = times.minOrNull() ?: 0,
            maxTimeMs = times.maxOrNull() ?: 0,
            opsPerSecond = iterations / (totalTime / 1000.0),
            memoryUsage = memoryUsage
        )
    }

    /**
     * Benchmark signature verification
     */
    suspend fun benchmarkVerification(
        algorithm: PostQuantumCrypto.SignatureAlgorithm,
        dataSize: Int = 1024,
        iterations: Int = DEFAULT_ITERATIONS
    ): BenchmarkResult = withContext(Dispatchers.Default) {
        Log.i(TAG, "Benchmarking ${algorithm.algorithmName} verification ($iterations iterations, $dataSize bytes)")
        
        // Generate key pair, test data, and signature
        val keyPair = provider.generateSignatureKeyPair(algorithm)
        val data = ByteArray(dataSize)
        SecureRandom().nextBytes(data)
        val signature = provider.sign(data, keyPair.private, algorithm)
        
        // Warmup
        repeat(WARMUP_ITERATIONS) {
            provider.verify(data, signature, keyPair.public, algorithm)
        }
        
        val times = mutableListOf<Long>()
        
        val totalTime = measureTimeMillis {
            repeat(iterations) {
                val time = measureTimeMillis {
                    provider.verify(data, signature, keyPair.public, algorithm)
                }
                times.add(time)
            }
        }
        
        val memoryUsage = MemoryUsage(
            publicKeySize = keyPair.public.encoded.size,
            privateKeySize = keyPair.private.encoded.size,
            signatureOrCiphertextSize = signature.size
        )
        
        BenchmarkResult(
            algorithmName = algorithm.algorithmName,
            operationType = "Verification (${dataSize}B data)",
            iterations = iterations,
            totalTimeMs = totalTime,
            avgTimeMs = times.average(),
            minTimeMs = times.minOrNull() ?: 0,
            maxTimeMs = times.maxOrNull() ?: 0,
            opsPerSecond = iterations / (totalTime / 1000.0),
            memoryUsage = memoryUsage
        )
    }

    /**
     * Run comprehensive benchmark suite for KEM algorithms
     */
    suspend fun runKEMBenchmarkSuite(
        algorithms: List<PostQuantumCrypto.KEMAlgorithm>,
        iterations: Int = DEFAULT_ITERATIONS
    ): BenchmarkSuite = withContext(Dispatchers.Default) {
        val results = mutableListOf<BenchmarkResult>()
        
        algorithms.forEach { algorithm ->
            if (provider.supportsKEM(algorithm)) {
                try {
                    results.add(benchmarkKEMKeyGen(algorithm, iterations))
                    results.add(benchmarkKEMEncapsulation(algorithm, iterations))
                    results.add(benchmarkKEMDecapsulation(algorithm, iterations))
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to benchmark ${algorithm.algorithmName}", e)
                }
            } else {
                Log.w(TAG, "Algorithm ${algorithm.algorithmName} not supported by provider ${provider.getName()}")
            }
        }
        
        BenchmarkSuite(results)
    }

    /**
     * Run comprehensive benchmark suite for signature algorithms
     */
    suspend fun runSignatureBenchmarkSuite(
        algorithms: List<PostQuantumCrypto.SignatureAlgorithm>,
        dataSizes: List<Int> = listOf(1024),
        iterations: Int = DEFAULT_ITERATIONS
    ): BenchmarkSuite = withContext(Dispatchers.Default) {
        val results = mutableListOf<BenchmarkResult>()
        
        algorithms.forEach { algorithm ->
            if (provider.supportsSignature(algorithm)) {
                try {
                    results.add(benchmarkSignatureKeyGen(algorithm, iterations))
                    dataSizes.forEach { dataSize ->
                        results.add(benchmarkSigning(algorithm, dataSize, iterations))
                        results.add(benchmarkVerification(algorithm, dataSize, iterations))
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to benchmark ${algorithm.algorithmName}", e)
                }
            } else {
                Log.w(TAG, "Algorithm ${algorithm.algorithmName} not supported by provider ${provider.getName()}")
            }
        }
        
        BenchmarkSuite(results)
    }

    /**
     * Compare classical vs post-quantum performance
     */
    suspend fun compareClassicalVsPQC(iterations: Int = DEFAULT_ITERATIONS): String = withContext(Dispatchers.Default) {
        val sb = StringBuilder()
        sb.appendLine("Classical vs Post-Quantum Cryptography Performance Comparison")
        sb.appendLine("=".repeat(80))
        sb.appendLine()
        
        // KEM comparison
        val kemAlgorithms = listOf(
            PostQuantumCrypto.KEMAlgorithm.ECDH_P256,
            PostQuantumCrypto.KEMAlgorithm.ML_KEM_512,
            PostQuantumCrypto.KEMAlgorithm.ML_KEM_768,
            PostQuantumCrypto.KEMAlgorithm.ML_KEM_1024
        )
        
        sb.appendLine("Key Encapsulation Mechanisms (KEM)")
        sb.appendLine("-".repeat(80))
        
        val kemSuite = runKEMBenchmarkSuite(kemAlgorithms.filter { provider.supportsKEM(it) }, iterations)
        kemSuite.results.forEach { result ->
            sb.appendLine("${result.algorithmName.padEnd(20)} | ${result.operationType.padEnd(20)} | ${"%.2f".format(result.avgTimeMs)}ms")
        }
        sb.appendLine()
        
        // Signature comparison
        val sigAlgorithms = listOf(
            PostQuantumCrypto.SignatureAlgorithm.ECDSA_P256,
            PostQuantumCrypto.SignatureAlgorithm.ML_DSA_44,
            PostQuantumCrypto.SignatureAlgorithm.ML_DSA_65,
            PostQuantumCrypto.SignatureAlgorithm.ML_DSA_87
        )
        
        sb.appendLine("Digital Signatures")
        sb.appendLine("-".repeat(80))
        
        val sigSuite = runSignatureBenchmarkSuite(sigAlgorithms.filter { provider.supportsSignature(it) }, listOf(1024), iterations)
        sigSuite.results.forEach { result ->
            sb.appendLine("${result.algorithmName.padEnd(20)} | ${result.operationType.padEnd(30)} | ${"%.2f".format(result.avgTimeMs)}ms")
        }
        
        sb.toString()
    }
}
