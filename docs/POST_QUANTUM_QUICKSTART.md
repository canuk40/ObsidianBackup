# Post-Quantum Cryptography - Quick Start Guide

This guide shows how to integrate and use ObsidianBackup's post-quantum cryptography implementation.

## Table of Contents

1. [Setup](#setup)
2. [Basic Usage](#basic-usage)
3. [Hybrid Mode](#hybrid-mode)
4. [Performance Testing](#performance-testing)
5. [Migration](#migration)
6. [Troubleshooting](#troubleshooting)

---

## Setup

### Step 1: Add Dependencies (Future)

When ready for production, add Bouncy Castle to `app/build.gradle.kts`:

```kotlin
dependencies {
    // Bouncy Castle for Post-Quantum Cryptography
    implementation("org.bouncycastle:bcprov-jdk18on:1.79")
    implementation("org.bouncycastle:bcpqc-jdk18on:1.79")
}
```

### Step 2: Register Provider (Future)

In your Application class or MainActivity:

```kotlin
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.pqc.jcajce.provider.BouncyCastlePQCProvider
import java.security.Security

class ObsidianBackupApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Register Bouncy Castle providers
        Security.addProvider(BouncyCastleProvider())
        Security.addProvider(BouncyCastlePQCProvider())
    }
}
```

### Step 3: Current Setup (Development)

For now, use the mock provider for testing:

```kotlin
import com.obsidianbackup.crypto.PostQuantumCrypto

// Use mock provider for development/testing
val pqcProvider = PostQuantumCrypto.MockPQCProvider()

// For production, use:
// val pqcProvider = BouncyCastlePQCProvider()
```

---

## Basic Usage

### Example 1: Encrypting a Backup with ML-KEM

```kotlin
import com.obsidianbackup.crypto.PostQuantumCrypto
import com.obsidianbackup.crypto.PostQuantumCrypto.KEMAlgorithm
import kotlinx.coroutines.runBlocking

fun encryptBackup(backupData: ByteArray): EncryptedPackage {
    return runBlocking {
        // 1. Initialize provider
        val provider = PostQuantumCrypto.MockPQCProvider()
        val algorithm = KEMAlgorithm.ML_KEM_768 // Recommended default
        
        // 2. Generate or load recipient's key pair
        val recipientKeyPair = provider.generateKEMKeyPair(algorithm)
        
        // Store public key for future use
        savePublicKey(recipientKeyPair.public.encoded)
        
        // 3. Encapsulate shared secret
        val encapResult = provider.encapsulate(recipientKeyPair.public, algorithm)
        
        // 4. Derive AES-256-GCM key from shared secret
        val aesKey = deriveAESKey(encapResult.sharedSecret)
        
        // 5. Encrypt backup data with AES-GCM
        val encryptedData = encryptAESGCM(backupData, aesKey)
        
        // 6. Package for transmission/storage
        EncryptedPackage(
            kemCiphertext = encapResult.ciphertext,
            encryptedData = encryptedData,
            algorithm = algorithm.algorithmName
        )
    }
}

fun decryptBackup(package: EncryptedPackage, privateKey: ByteArray): ByteArray {
    return runBlocking {
        val provider = PostQuantumCrypto.MockPQCProvider()
        val algorithm = KEMAlgorithm.valueOf(package.algorithm)
        
        // 1. Reconstruct private key
        val privKey = reconstructPrivateKey(privateKey, algorithm)
        
        // 2. Decapsulate shared secret
        val sharedSecret = provider.decapsulate(
            package.kemCiphertext,
            privKey,
            algorithm
        )
        
        // 3. Derive AES key
        val aesKey = deriveAESKey(sharedSecret)
        
        // 4. Decrypt backup data
        decryptAESGCM(package.encryptedData, aesKey)
    }
}

data class EncryptedPackage(
    val kemCiphertext: ByteArray,
    val encryptedData: ByteArray,
    val algorithm: String
)
```

### Example 2: Signing Backup Metadata

```kotlin
import com.obsidianbackup.crypto.PostQuantumCrypto.SignatureAlgorithm

fun signBackupMetadata(metadata: BackupMetadata): SignedMetadata {
    return runBlocking {
        val provider = PostQuantumCrypto.MockPQCProvider()
        val algorithm = SignatureAlgorithm.ML_DSA_65 // Recommended default
        
        // 1. Generate or load signing key pair
        val signingKeyPair = provider.generateSignatureKeyPair(algorithm)
        
        // 2. Serialize metadata
        val metadataBytes = serializeMetadata(metadata)
        
        // 3. Sign
        val signature = provider.sign(metadataBytes, signingKeyPair.private, algorithm)
        
        SignedMetadata(
            metadata = metadata,
            signature = signature,
            publicKey = signingKeyPair.public.encoded,
            algorithm = algorithm.algorithmName
        )
    }
}

fun verifyBackupMetadata(signed: SignedMetadata): Boolean {
    return runBlocking {
        val provider = PostQuantumCrypto.MockPQCProvider()
        val algorithm = SignatureAlgorithm.valueOf(signed.algorithm)
        
        // 1. Reconstruct public key
        val publicKey = reconstructPublicKey(signed.publicKey, algorithm)
        
        // 2. Serialize metadata
        val metadataBytes = serializeMetadata(signed.metadata)
        
        // 3. Verify signature
        provider.verify(metadataBytes, signed.signature, publicKey, algorithm)
    }
}

data class BackupMetadata(
    val backupId: String,
    val timestamp: Long,
    val fileCount: Int,
    val totalSize: Long
)

data class SignedMetadata(
    val metadata: BackupMetadata,
    val signature: ByteArray,
    val publicKey: ByteArray,
    val algorithm: String
)
```

---

## Hybrid Mode

### Why Use Hybrid Mode?

Hybrid mode combines classical (ECDH/ECDSA) with post-quantum algorithms:

- ✅ **Defense in Depth**: Secure if either algorithm remains unbroken
- ✅ **Migration Safety**: Gradual transition from classical to PQC
- ✅ **Industry Standard**: Recommended by NIST, BSI, ANSSI

### Example 3: Hybrid Key Encapsulation

```kotlin
fun encryptWithHybridMode(data: ByteArray): HybridEncryptedData {
    return runBlocking {
        // 1. Create hybrid provider
        val classicalProvider = PostQuantumCrypto.ClassicalCryptoProvider()
        val pqcProvider = PostQuantumCrypto.MockPQCProvider()
        val hybridProvider = PostQuantumCrypto.HybridCryptoProvider(
            classicalProvider,
            pqcProvider
        )
        
        val algorithm = KEMAlgorithm.HYBRID_ECDH_ML_KEM_768
        
        // 2. Generate hybrid key pair (ECDH + ML-KEM)
        val keyPair = hybridProvider.generateKEMKeyPair(algorithm)
        
        // 3. Encapsulate with both algorithms
        val result = hybridProvider.encapsulate(keyPair.public, algorithm)
        
        // result.sharedSecret = SHA-256(ECDH_secret || ML_KEM_secret)
        // Secure if EITHER ECDH OR ML-KEM remains secure
        
        // 4. Use combined shared secret for encryption
        val aesKey = deriveAESKey(result.sharedSecret)
        val encrypted = encryptAESGCM(data, aesKey)
        
        HybridEncryptedData(
            kemCiphertext = result.ciphertext,
            encryptedData = encrypted,
            privateKey = keyPair.private.encoded
        )
    }
}

data class HybridEncryptedData(
    val kemCiphertext: ByteArray,
    val encryptedData: ByteArray,
    val privateKey: ByteArray
)
```

### Example 4: Hybrid Digital Signatures

```kotlin
fun signWithHybridMode(data: ByteArray): HybridSignature {
    return runBlocking {
        val classicalProvider = PostQuantumCrypto.ClassicalCryptoProvider()
        val pqcProvider = PostQuantumCrypto.MockPQCProvider()
        val hybridProvider = PostQuantumCrypto.HybridCryptoProvider(
            classicalProvider,
            pqcProvider
        )
        
        val algorithm = SignatureAlgorithm.HYBRID_ECDSA_ML_DSA_65
        
        // Generate hybrid signing key (ECDSA + ML-DSA)
        val keyPair = hybridProvider.generateSignatureKeyPair(algorithm)
        
        // Sign with both algorithms
        val signature = hybridProvider.sign(data, keyPair.private, algorithm)
        
        // signature = ECDSA_signature || ML_DSA_signature
        // Both must verify for signature to be valid
        
        HybridSignature(
            data = data,
            signature = signature,
            publicKey = keyPair.public.encoded
        )
    }
}

fun verifyHybridSignature(hybrid: HybridSignature): Boolean {
    return runBlocking {
        val classicalProvider = PostQuantumCrypto.ClassicalCryptoProvider()
        val pqcProvider = PostQuantumCrypto.MockPQCProvider()
        val hybridProvider = PostQuantumCrypto.HybridCryptoProvider(
            classicalProvider,
            pqcProvider
        )
        
        val algorithm = SignatureAlgorithm.HYBRID_ECDSA_ML_DSA_65
        val publicKey = reconstructPublicKey(hybrid.publicKey, algorithm)
        
        // Verifies both ECDSA and ML-DSA signatures
        hybridProvider.verify(hybrid.data, hybrid.signature, publicKey, algorithm)
    }
}

data class HybridSignature(
    val data: ByteArray,
    val signature: ByteArray,
    val publicKey: ByteArray
)
```

---

## Performance Testing

### Example 5: Running Benchmarks

```kotlin
import com.obsidianbackup.crypto.PQCBenchmark

suspend fun runPerformanceBenchmarks() {
    val provider = PostQuantumCrypto.MockPQCProvider()
    val benchmark = PQCBenchmark(provider)
    
    // 1. Benchmark single algorithm
    val mlKem768Result = benchmark.benchmarkKEMEncapsulation(
        algorithm = KEMAlgorithm.ML_KEM_768,
        iterations = 100
    )
    
    println("ML-KEM-768 Encapsulation:")
    println("  Average time: ${"%.2f".format(mlKem768Result.avgTimeMs)}ms")
    println("  Throughput: ${"%.2f".format(mlKem768Result.opsPerSecond)} ops/sec")
    println("  Memory: ${mlKem768Result.memoryUsage.totalBytes} bytes")
    
    // 2. Comprehensive KEM benchmark
    val kemSuite = benchmark.runKEMBenchmarkSuite(
        algorithms = listOf(
            KEMAlgorithm.ECDH_P256,
            KEMAlgorithm.ML_KEM_512,
            KEMAlgorithm.ML_KEM_768,
            KEMAlgorithm.ML_KEM_1024,
            KEMAlgorithm.HYBRID_ECDH_ML_KEM_768
        ),
        iterations = 100
    )
    
    kemSuite.printSummary()
    
    // 3. Comprehensive signature benchmark
    val sigSuite = benchmark.runSignatureBenchmarkSuite(
        algorithms = listOf(
            SignatureAlgorithm.ECDSA_P256,
            SignatureAlgorithm.ML_DSA_44,
            SignatureAlgorithm.ML_DSA_65,
            SignatureAlgorithm.ML_DSA_87
        ),
        dataSizes = listOf(1024, 10240), // 1KB and 10KB
        iterations = 100
    )
    
    sigSuite.printSummary()
    
    // 4. Export results as markdown
    val markdown = kemSuite.toMarkdown()
    saveToFile("/sdcard/Download/pqc_benchmark.md", markdown)
    
    // 5. Compare classical vs PQC
    val comparison = benchmark.compareClassicalVsPQC(iterations = 50)
    println(comparison)
}
```

### Example 6: Benchmark Specific Operations

```kotlin
suspend fun benchmarkSpecificOperations() {
    val provider = PostQuantumCrypto.MockPQCProvider()
    val benchmark = PQCBenchmark(provider)
    
    // Benchmark key generation
    val keyGenResult = benchmark.benchmarkKEMKeyGen(
        algorithm = KEMAlgorithm.ML_KEM_768,
        iterations = 50
    )
    println("Key generation: ${"%.2f".format(keyGenResult.avgTimeMs)}ms avg")
    
    // Benchmark signing with different data sizes
    val signResult1KB = benchmark.benchmarkSigning(
        algorithm = SignatureAlgorithm.ML_DSA_65,
        dataSize = 1024,
        iterations = 50
    )
    
    val signResult10KB = benchmark.benchmarkSigning(
        algorithm = SignatureAlgorithm.ML_DSA_65,
        dataSize = 10240,
        iterations = 50
    )
    
    println("Signing 1KB: ${"%.2f".format(signResult1KB.avgTimeMs)}ms")
    println("Signing 10KB: ${"%.2f".format(signResult10KB.avgTimeMs)}ms")
}
```

---

## Migration

### Example 7: Detecting and Migrating Keys

```kotlin
import com.obsidianbackup.crypto.PostQuantumCrypto.KeyStorageFormat
import com.obsidianbackup.crypto.PostQuantumCrypto.MigrationHelper

fun migrateBackupEncryption(backupId: String) {
    // 1. Load existing encrypted backup
    val encryptedData = loadBackup(backupId)
    
    // 2. Detect encryption format
    val format = MigrationHelper.detectFormat(encryptedData)
    println("Detected format: $format")
    
    // 3. Check if migration needed
    if (MigrationHelper.shouldMigrate(format, KEMAlgorithm.ML_KEM_768)) {
        println("Migration needed: $format → ML-KEM-768")
        
        // 4. Decrypt with old key
        val classicalProvider = PostQuantumCrypto.ClassicalCryptoProvider()
        val oldKeyPair = loadLegacyKey(backupId)
        val decrypted = decryptWithClassical(encryptedData, oldKeyPair)
        
        // 5. Re-encrypt with new PQC key
        val pqcProvider = PostQuantumCrypto.MockPQCProvider()
        val newKeyPair = runBlocking {
            pqcProvider.generateKEMKeyPair(KEMAlgorithm.ML_KEM_768)
        }
        val reencrypted = encryptWithPQC(decrypted, newKeyPair)
        
        // 6. Save new encrypted backup
        saveBackup(backupId, reencrypted)
        
        // 7. Create migration metadata
        val metadata = MigrationHelper.createMigrationMetadata(
            sourceAlgorithm = "ECDH-P256",
            targetAlgorithm = "ML-KEM-768",
            migrationDate = java.time.Instant.now().toString()
        )
        saveMigrationMetadata(backupId, metadata)
        
        println("Migration complete!")
    } else {
        println("No migration needed")
    }
}
```

### Example 8: Storing Keys with Metadata

```kotlin
fun storeKeyWithMetadata(keyPair: KeyPair, algorithm: String) {
    val metadata = mapOf(
        "algorithm" to algorithm,
        "createdDate" to java.time.Instant.now().toString(),
        "securityLevel" to getSecurityLevel(algorithm),
        "deviceId" to android.os.Build.MODEL,
        "appVersion" to BuildConfig.VERSION_NAME
    )
    
    val keyStorage = KeyStorageFormat(
        version = 1,
        algorithmId = algorithm,
        keyMaterial = keyPair.private.encoded,
        metadata = metadata
    )
    
    val serialized = keyStorage.serialize()
    
    // Save to Android Keystore or encrypted SharedPreferences
    val encryptedStorage = EncryptedSharedPreferences.create(
        context,
        "pqc_keys",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    
    encryptedStorage.edit()
        .putString("key_$algorithm", Base64.encodeToString(serialized, Base64.NO_WRAP))
        .apply()
}

fun loadKeyWithMetadata(algorithm: String): KeyStorageFormat? {
    val encryptedStorage = EncryptedSharedPreferences.create(
        context,
        "pqc_keys",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    
    val serialized = encryptedStorage.getString("key_$algorithm", null) ?: return null
    val bytes = Base64.decode(serialized, Base64.NO_WRAP)
    
    return KeyStorageFormat.deserialize(bytes)
}
```

---

## Troubleshooting

### Issue 1: "Algorithm not supported by provider"

**Problem:**
```kotlin
java.lang.UnsupportedOperationException: Algorithm ML-KEM-768 not supported by classical provider
```

**Solution:**
```kotlin
// Check provider support before using
val provider = PostQuantumCrypto.ClassicalCryptoProvider()
if (!provider.supportsKEM(KEMAlgorithm.ML_KEM_768)) {
    // Use PQC provider instead
    val pqcProvider = PostQuantumCrypto.MockPQCProvider()
    // ... use pqcProvider
}
```

### Issue 2: Large signature sizes

**Problem:** ML-DSA or SLH-DSA signatures are too large (2-50 KB).

**Solution:**
```kotlin
// For general use, prefer ML-DSA-44 (smallest PQC signature)
val algorithm = SignatureAlgorithm.ML_DSA_44 // 2,420 bytes

// For very size-sensitive applications, use classical temporarily
val algorithm = SignatureAlgorithm.ECDSA_P256 // 64 bytes

// For firmware/critical signing, use SLH-DSA (conservative)
val algorithm = SignatureAlgorithm.SLH_DSA_SHA2_128S // 7,856 bytes
```

### Issue 3: Performance concerns

**Problem:** PQC operations slower than classical.

**Solution:**
```kotlin
// 1. Cache key pairs (expensive to generate)
class KeyCache {
    private val cache = mutableMapOf<String, KeyPair>()
    
    suspend fun getOrGenerate(algorithm: KEMAlgorithm): KeyPair {
        return cache.getOrPut(algorithm.algorithmName) {
            provider.generateKEMKeyPair(algorithm)
        }
    }
}

// 2. Use smaller parameter sets
// ML-KEM-512 instead of ML-KEM-1024
val algorithm = KEMAlgorithm.ML_KEM_512 // Faster, still secure

// 3. Benchmark to identify bottlenecks
val benchmark = PQCBenchmark(provider)
val result = benchmark.benchmarkKEMEncapsulation(algorithm, iterations = 10)
println("Performance: ${result.avgTimeMs}ms avg")
```

### Issue 4: Key storage format version mismatch

**Problem:** Cannot deserialize key stored with old format.

**Solution:**
```kotlin
fun loadKeyWithVersionCheck(data: ByteArray): KeyStorageFormat {
    val version = data[0].toInt()
    
    return when (version) {
        1 -> KeyStorageFormat.deserialize(data)
        0 -> migrateLegacyFormat(data)
        else -> throw IllegalStateException("Unsupported version: $version")
    }
}

fun migrateLegacyFormat(oldData: ByteArray): KeyStorageFormat {
    // Convert old format to new format
    // ... migration logic
}
```

---

## Next Steps

1. **Development:** Continue using `MockPQCProvider` for testing
2. **Integration:** Add Bouncy Castle dependencies when ready
3. **Testing:** Run comprehensive benchmarks on target devices
4. **Migration:** Plan gradual rollout strategy
5. **Production:** Deploy hybrid mode with monitoring

For more details, see [POST_QUANTUM_CRYPTO.md](POST_QUANTUM_CRYPTO.md).
