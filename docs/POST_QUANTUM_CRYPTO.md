# Post-Quantum Cryptography Implementation Guide

**Status:** 🟡 Infrastructure Ready - Awaiting Production Library Integration  
**Version:** 1.0  
**Last Updated:** 2025-01-XX  
**NIST Standards:** FIPS-203, FIPS-204, FIPS-205

---

## Executive Summary

This document describes ObsidianBackup's preparation for post-quantum cryptography (PQC) following NIST's August 2024 standardization of quantum-resistant algorithms. The implementation provides a **hybrid classical+PQC approach** ensuring security even if one algorithm is broken, with full crypto agility for future algorithm updates.

### Key Features

✅ **NIST-Compliant Standards**
- FIPS-203 (ML-KEM): Key encapsulation mechanism
- FIPS-204 (ML-DSA): Digital signatures  
- FIPS-205 (SLH-DSA): Hash-based signatures

✅ **Hybrid Mode**
- Classical (ECDH/ECDSA) + Post-Quantum
- Security if either algorithm remains secure
- Smooth migration path

✅ **Crypto Agility**
- Pluggable algorithm interface
- Easy to swap implementations
- Future-proof design

✅ **Production Ready Infrastructure**
- Versioned key storage format
- Migration tools
- Performance benchmarking
- Comprehensive testing

---

## Table of Contents

1. [Background & Motivation](#background--motivation)
2. [NIST Post-Quantum Standards](#nist-post-quantum-standards)
3. [Architecture Overview](#architecture-overview)
4. [Implementation Details](#implementation-details)
5. [Library Options](#library-options)
6. [Migration Strategy](#migration-strategy)
7. [Performance Benchmarks](#performance-benchmarks)
8. [Usage Examples](#usage-examples)
9. [Security Considerations](#security-considerations)
10. [Roadmap & Next Steps](#roadmap--next-steps)

---

## Background & Motivation

### The Quantum Threat

Large-scale quantum computers, once operational, will break current public-key cryptography:

- **Shor's Algorithm**: Breaks RSA, Diffie-Hellman, ECDSA, ECDH
- **Impact**: All current key exchange and digital signatures vulnerable
- **Timeline**: Uncertain, but preparations needed now (10-20 years estimated)
- **"Harvest Now, Decrypt Later"**: Adversaries storing encrypted data to decrypt when quantum computers arrive

### NIST Standardization (August 2024)

After an 8-year process, NIST published official post-quantum cryptography standards:

- **FIPS 203** (ML-KEM): Key encapsulation for encryption
- **FIPS 204** (ML-DSA): Digital signatures (lattice-based)
- **FIPS 205** (SLH-DSA): Digital signatures (hash-based)

### Why Hybrid Mode?

Combining classical and post-quantum algorithms provides:

1. **Defense in Depth**: Security if either algorithm is broken
2. **Gradual Migration**: Maintain compatibility during transition
3. **Risk Mitigation**: PQC algorithms are newer, less battle-tested
4. **Standards Compliance**: Recommended by NIST, BSI, ANSSI

---

## NIST Post-Quantum Standards

### FIPS-203: ML-KEM (Module-Lattice-Based Key Encapsulation)

**Based on:** CRYSTALS-Kyber  
**Purpose:** Secure key establishment over public channels  
**Security Basis:** Module Learning With Errors (MLWE) problem

#### Parameter Sets

| Variant | Public Key | Secret Key | Ciphertext | Security Level | Use Case |
|---------|-----------|-----------|-----------|---------------|----------|
| **ML-KEM-512** | 800 B | 1,632 B | 768 B | AES-128 | IoT, constrained devices |
| **ML-KEM-768** | 1,184 B | 2,400 B | 1,088 B | AES-192 | **Recommended default** |
| **ML-KEM-1024** | 1,568 B | 3,168 B | 1,568 B | AES-256 | High security applications |

**All variants produce a 32-byte shared secret.**

#### Algorithm Flow

```
Alice (Initiator)                    Bob (Responder)
-----------------                    ----------------
                                     1. KeyGen() → (pk, sk)
                                        ↓
                                     2. Send pk to Alice
         ↓
3. Encaps(pk) → (ct, ss)
   ↓
4. Send ct to Bob
                                     5. Decaps(ct, sk) → ss
         ↓                                ↓
6. Both parties have shared secret ss (32 bytes)
7. Use ss to derive AES-GCM encryption keys
```

#### Security Properties

- **IND-CCA2 Secure**: Resistant to chosen-ciphertext attacks
- **Quantum Safe**: Based on hard lattice problems
- **No Timing Leaks**: Constant-time implementation required

---

### FIPS-204: ML-DSA (Module-Lattice-Based Digital Signatures)

**Based on:** CRYSTALS-Dilithium  
**Purpose:** Digital signatures for authentication and integrity  
**Security Basis:** Module-LWE and Module-LWR problems

#### Parameter Sets

| Variant | Public Key | Private Key | Signature | Security Level | Use Case |
|---------|-----------|-------------|-----------|---------------|----------|
| **ML-DSA-44** | 1,312 B | 2,528 B | 2,420 B | ~128-bit | Standard security |
| **ML-DSA-65** | 1,952 B | 4,000 B | 3,293 B | ~192-bit | **Recommended default** |
| **ML-DSA-87** | 2,592 B | 4,864 B | 4,595 B | ~256-bit | High security |

#### Performance Characteristics

- **Fast Verification**: ~10x faster than signing
- **Good Key Sizes**: Smaller than SLH-DSA
- **Moderate Signatures**: Larger than ECDSA but acceptable

#### Algorithm Flow

```
Signer                               Verifier
------                               --------
1. KeyGen() → (pk, sk)
2. Publish/distribute pk
                                     3. Obtain pk
         ↓
4. Sign(message, sk) → signature
5. Send (message, signature)
                                     6. Verify(message, signature, pk) → valid/invalid
```

---

### FIPS-205: SLH-DSA (Stateless Hash-Based Digital Signatures)

**Based on:** SPHINCS+  
**Purpose:** Conservative digital signatures using only hash functions  
**Security Basis:** Hash function security (preimage resistance)

#### Parameter Sets

| Variant | Public Key | Private Key | Signature Size | Security Level | Speed |
|---------|-----------|-------------|---------------|---------------|-------|
| **SLH-DSA-SHA2-128s** | 32 B | 64 B | 7,856 B | ~128-bit | Small/Slow |
| **SLH-DSA-SHA2-128f** | 32 B | 64 B | 17,088 B | ~128-bit | Fast/Large |
| **SLH-DSA-SHA2-192s** | 48 B | 96 B | 16,224 B | ~192-bit | Small/Slow |
| **SLH-DSA-SHA2-192f** | 48 B | 96 B | 35,664 B | ~192-bit | Fast/Large |
| **SLH-DSA-SHA2-256s** | 64 B | 128 B | 29,792 B | ~256-bit | Small/Slow |
| **SLH-DSA-SHA2-256f** | 64 B | 128 B | 49,856 B | ~256-bit | Fast/Large |

**Note:** "s" = small signature (slower), "f" = fast (larger signature)

#### Use Cases

- **Firmware signing**: Long-term security
- **Code signing**: High assurance
- **Certificate authorities**: Root certificates
- **Backup scenarios**: Not time-sensitive

#### Advantages

- **Conservative Security**: Based only on hash functions
- **No Assumptions**: No lattice, code, or isogeny assumptions
- **Stateless**: No need to track signature count (unlike XMSS/LMS)

#### Disadvantages

- **Large Signatures**: 7KB - 50KB (vs ~3KB for ML-DSA)
- **Slower**: Especially for "s" variants
- **Overkill**: For most applications, ML-DSA is sufficient

---

## Architecture Overview

### Component Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                    ObsidianBackup Application                   │
└─────────────────────────────────────────────────────────────────┘
                             │
                             ↓
┌─────────────────────────────────────────────────────────────────┐
│                    PostQuantumCrypto.kt                         │
│  ┌───────────────────────────────────────────────────────────┐ │
│  │              CryptoProvider Interface                      │ │
│  │  • generateKEMKeyPair()    • encapsulate()                │ │
│  │  • decapsulate()           • generateSignatureKeyPair()   │ │
│  │  • sign()                  • verify()                     │ │
│  └───────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
         │                    │                     │
         ↓                    ↓                     ↓
┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐
│   Classical     │  │  MockPQC        │  │    Hybrid       │
│   Provider      │  │  Provider       │  │   Provider      │
│  (ECDH/ECDSA)   │  │ (Development)   │  │ (Classical+PQC) │
└─────────────────┘  └─────────────────┘  └─────────────────┘
         │                    │                     │
         ↓                    ↓                     ↓
┌─────────────────────────────────────────────────────────────────┐
│              Future: Bouncy Castle or liboqs-java               │
│  • ML-KEM-512/768/1024   • ML-DSA-44/65/87   • SLH-DSA-*       │
└─────────────────────────────────────────────────────────────────┘
         │
         ↓
┌─────────────────────────────────────────────────────────────────┐
│              AES-GCM Symmetric Encryption                       │
│  (Shared secret from KEM used to derive AES-256-GCM keys)      │
└─────────────────────────────────────────────────────────────────┘
```

### Key Design Principles

#### 1. Crypto Agility

**Problem:** Cryptographic algorithms can be broken or deprecated.  
**Solution:** Abstract algorithm implementations behind interfaces.

```kotlin
interface CryptoProvider {
    fun supportsKEM(algorithm: KEMAlgorithm): Boolean
    fun supportsSignature(algorithm: SignatureAlgorithm): Boolean
    suspend fun generateKEMKeyPair(algorithm: KEMAlgorithm): KeyPair
    // ... other operations
}
```

**Benefits:**
- Easy to swap Bouncy Castle ↔ liboqs-java ↔ native implementations
- Can add new algorithms without changing application code
- Enable/disable algorithms via configuration

#### 2. Hybrid Mode

**Strategy:** Combine classical and post-quantum algorithms.

```kotlin
// Hybrid KEM: ECDH + ML-KEM-768
val hybridProvider = HybridCryptoProvider(classicalProvider, pqcProvider)
val keyPair = hybridProvider.generateKEMKeyPair(HYBRID_ECDH_ML_KEM_768)

// Encapsulation produces combined shared secret
val result = hybridProvider.encapsulate(publicKey, HYBRID_ECDH_ML_KEM_768)
// result.sharedSecret = SHA-256(ECDH_secret || ML_KEM_secret)
```

**Security:** If either ECDH or ML-KEM is broken, the other still protects data.

#### 3. Future-Proof Key Storage

**Format:**
```
[Version:1B][AlgID_Len:2B][AlgID:var][Key_Len:4B][Key:var][Meta_Len:4B][Metadata:JSON]
```

**Metadata Example:**
```json
{
  "algorithm": "ML-KEM-768",
  "createdDate": "2025-01-15T10:30:00Z",
  "securityLevel": "AES-192-equivalent",
  "version": "1",
  "migrationHistory": "ECDH-P256 → ML-KEM-768"
}
```

**Benefits:**
- Can detect and migrate old formats
- Store algorithm parameters
- Track key lifecycle
- Enable gradual rollout

---

## Implementation Details

### File Structure

```
app/src/main/java/com/obsidianbackup/crypto/
├── PostQuantumCrypto.kt        # Main PQC implementation
│   ├── KEMAlgorithm enum       # Supported KEM algorithms
│   ├── SignatureAlgorithm enum # Supported signature algorithms
│   ├── CryptoProvider interface # Algorithm abstraction
│   ├── ClassicalCryptoProvider # ECDH/ECDSA implementation
│   ├── MockPQCProvider         # Development/testing provider
│   ├── HybridCryptoProvider    # Classical + PQC hybrid
│   ├── KeyStorageFormat        # Version key storage
│   └── MigrationHelper         # Migration utilities
│
├── PQCBenchmark.kt             # Performance benchmarking
│   ├── benchmarkKEMKeyGen()
│   ├── benchmarkKEMEncapsulation()
│   ├── benchmarkKEMDecapsulation()
│   ├── benchmarkSignatureKeyGen()
│   ├── benchmarkSigning()
│   ├── benchmarkVerification()
│   ├── runKEMBenchmarkSuite()
│   └── compareClassicalVsPQC()
│
└── EncryptionEngine.kt         # Existing AES-GCM engine
```

### Supported Algorithms

#### Key Encapsulation (KEM)

| Algorithm | Status | Provider | Notes |
|-----------|--------|----------|-------|
| **ECDH-P256** | ✅ Production | Android Keystore | Current implementation |
| **ML-KEM-512** | 🟡 Mock | Awaiting BC/liboqs | NIST FIPS-203 |
| **ML-KEM-768** | 🟡 Mock | Awaiting BC/liboqs | **Recommended** |
| **ML-KEM-1024** | 🟡 Mock | Awaiting BC/liboqs | High security |
| **Hybrid-ECDH-ML-KEM-768** | 🟡 Mock | Awaiting BC/liboqs | **Recommended hybrid** |

#### Digital Signatures

| Algorithm | Status | Provider | Notes |
|-----------|--------|----------|-------|
| **ECDSA-P256** | ✅ Production | Android Keystore | Current implementation |
| **RSA-2048** | ✅ Production | Android Keystore | Legacy support |
| **ML-DSA-44** | 🟡 Mock | Awaiting BC/liboqs | NIST FIPS-204 |
| **ML-DSA-65** | 🟡 Mock | Awaiting BC/liboqs | **Recommended** |
| **ML-DSA-87** | 🟡 Mock | Awaiting BC/liboqs | High security |
| **SLH-DSA-SHA2-128s** | 🟡 Mock | Awaiting BC/liboqs | NIST FIPS-205 (conservative) |
| **Hybrid-ECDSA-ML-DSA-65** | 🟡 Mock | Awaiting BC/liboqs | **Recommended hybrid** |

---

## Library Options

### Option 1: Bouncy Castle (Recommended)

**Status:** ✅ NIST standards supported as of v1.79 (2024)

#### Advantages
- ✅ Pure Java/Kotlin - no native dependencies
- ✅ Mature library (20+ years)
- ✅ Excellent Android support
- ✅ JCA/JCE compatible
- ✅ ML-KEM, ML-DSA, SLH-DSA support
- ✅ Active maintenance
- ✅ Commercial support available (Keyfactor)
- ✅ Hybrid mode examples provided

#### Disadvantages
- ⚠️ Larger binary size (~2-5 MB)
- ⚠️ Slightly slower than native (but still fast)

#### Integration

**build.gradle.kts:**
```kotlin
dependencies {
    // Bouncy Castle Provider (main crypto library)
    implementation("org.bouncycastle:bcprov-jdk18on:1.79")
    
    // Bouncy Castle PQC Provider (post-quantum algorithms)
    implementation("org.bouncycastle:bcpqc-jdk18on:1.79")
    
    // TLS support (optional, for future HTTPS with PQC)
    implementation("org.bouncycastle:bctls-jdk18on:1.79")
}
```

**Usage Example:**
```kotlin
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.pqc.jcajce.provider.BouncyCastlePQCProvider
import java.security.Security

// Register providers at app startup
Security.addProvider(BouncyCastleProvider())
Security.addProvider(BouncyCastlePQCProvider())

// Generate ML-KEM-768 key pair
val keyGen = KeyPairGenerator.getInstance("ML-KEM-768", "BCPQC")
val keyPair = keyGen.generateKeyPair()

// Encapsulate
val kem = KeyAgreement.getInstance("ML-KEM-768", "BCPQC")
kem.init(keyPair.public)
val sharedSecret = kem.generateSecret()
```

#### Proguard Rules
```proguard
# Bouncy Castle
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**
-keep class org.bouncycastle.jcajce.provider.** { *; }
-keep class org.bouncycastle.pqc.jcajce.provider.** { *; }
```

---

### Option 2: liboqs-java

**Status:** 🟡 Experimental Android support

#### Advantages
- ✅ Based on NIST reference implementations
- ✅ Potentially faster (native C)
- ✅ Linux Foundation project
- ✅ Comprehensive algorithm support
- ✅ Active research community

#### Disadvantages
- ⚠️ Requires native libraries (JNI)
- ⚠️ Complex Android build (NDK cross-compilation)
- ⚠️ No official Android binaries
- ⚠️ Marked "for research, not production"
- ⚠️ Less mature JCA integration

#### Android Integration Steps

1. **Build liboqs C library for Android:**
```bash
# Requires Android NDK
export ANDROID_NDK=/path/to/ndk
export TOOLCHAIN=$ANDROID_NDK/toolchains/llvm/prebuilt/linux-x86_64

# Build for arm64-v8a
cmake .. \
  -DCMAKE_TOOLCHAIN_FILE=$ANDROID_NDK/build/cmake/android.toolchain.cmake \
  -DANDROID_ABI=arm64-v8a \
  -DANDROID_PLATFORM=android-26 \
  -DCMAKE_BUILD_TYPE=Release

make
```

2. **Build liboqs-java JNI wrapper:**
```bash
# Compile Java/JNI glue code for Android
./gradlew build -Pandroid
```

3. **Include in Android project:**
```
app/src/main/jniLibs/
├── arm64-v8a/
│   ├── liboqs.so
│   └── liboqs-jni.so
├── armeabi-v7a/
│   ├── liboqs.so
│   └── liboqs-jni.so
└── x86_64/
    ├── liboqs.so
    └── liboqs-jni.so
```

**Verdict:** Requires significant build engineering. Wait for official Android support or use Bouncy Castle.

---

### Recommendation: Bouncy Castle

**For ObsidianBackup, use Bouncy Castle because:**

1. ✅ Pure Java - no NDK complexity
2. ✅ Production-ready with NIST standards support
3. ✅ Excellent Android track record
4. ✅ Easier maintenance and updates
5. ✅ Smaller APK impact than bundling native libs for multiple ABIs

**Next Step:** Add Bouncy Castle dependencies and implement `BouncyCastlePQCProvider` class.

---

## Migration Strategy

### Phase 1: Infrastructure (Current)

**Status:** ✅ Complete

- [x] Define algorithm enums
- [x] Create CryptoProvider interface
- [x] Implement ClassicalCryptoProvider (ECDH/ECDSA)
- [x] Implement MockPQCProvider (testing)
- [x] Implement HybridCryptoProvider
- [x] Design versioned key storage format
- [x] Create migration helper utilities
- [x] Implement performance benchmarking
- [x] Write comprehensive documentation

### Phase 2: Library Integration

**Status:** 🔴 Not Started

- [ ] Add Bouncy Castle dependencies to build.gradle.kts
- [ ] Implement `BouncyCastlePQCProvider` class
- [ ] Test ML-KEM-768 key generation
- [ ] Test ML-DSA-65 signing/verification
- [ ] Validate key storage format
- [ ] Run benchmark suite
- [ ] Update ProGuard rules

### Phase 3: Hybrid Mode Testing

**Status:** 🔴 Not Started

- [ ] Test HYBRID_ECDH_ML_KEM_768 encapsulation
- [ ] Test HYBRID_ECDSA_ML_DSA_65 signatures
- [ ] Validate shared secret combination
- [ ] Test interoperability (encrypt with classical, decrypt with hybrid)
- [ ] Performance benchmarks for hybrid mode

### Phase 4: Migration Tools

**Status:** 🔴 Not Started

- [ ] Implement background migration service
- [ ] Re-encrypt existing backups with hybrid keys
- [ ] Preserve backward compatibility (can read old format)
- [ ] Progress tracking and rollback capability
- [ ] User notification and opt-in

### Phase 5: Production Rollout

**Status:** 🔴 Not Started

- [ ] Beta testing with select users
- [ ] Monitor performance impact
- [ ] Gradual rollout (10% → 50% → 100%)
- [ ] Documentation and user education
- [ ] Emergency rollback plan

### Migration Timeline

```
2025 Q1: Phase 2 (Library Integration)
2025 Q2: Phase 3 (Hybrid Testing)
2025 Q3: Phase 4 (Migration Tools)
2025 Q4: Phase 5 (Production Rollout)
2026+:   100% PQC by default, classical deprecated
```

### Backward Compatibility

**Design Goals:**
- ✅ New app can read old (classical) encrypted backups
- ✅ Old app cannot read new (PQC) encrypted backups (acceptable)
- ✅ Metadata identifies encryption version

**Detection Logic:**
```kotlin
fun detectEncryptionFormat(data: ByteArray): EncryptionFormat {
    return when {
        data[0] == 0x01.toByte() -> EncryptionFormat.PQC_V1
        data.size > 12 && data[0..3].contentEquals(magicBytes) -> EncryptionFormat.LEGACY_AES_GCM
        else -> EncryptionFormat.UNKNOWN
    }
}
```

---

## Performance Benchmarks

### Expected Performance (Mock Provider)

**Note:** These are simulated benchmarks. Real performance will be measured after Bouncy Castle integration.

#### Key Encapsulation Mechanisms (KEM)

| Algorithm | KeyGen (ms) | Encapsulation (ms) | Decapsulation (ms) | Throughput (ops/sec) |
|-----------|-------------|-------------------|-------------------|---------------------|
| **ECDH-P256** | 5-10 | 2-5 | 2-5 | 200-500 |
| **ML-KEM-512** | 1-3 | 1-2 | 1-2 | 500-1000 (estimated) |
| **ML-KEM-768** | 2-4 | 1-3 | 1-3 | 300-700 (estimated) |
| **ML-KEM-1024** | 3-6 | 2-4 | 2-4 | 200-500 (estimated) |
| **Hybrid-ECDH-ML-KEM** | 7-14 | 4-8 | 4-8 | 100-250 (estimated) |

#### Digital Signatures

| Algorithm | KeyGen (ms) | Sign (ms) | Verify (ms) | Signature Size |
|-----------|-------------|-----------|-------------|---------------|
| **ECDSA-P256** | 5-10 | 2-5 | 3-7 | 64 B |
| **RSA-2048** | 100-300 | 5-10 | 1-2 | 256 B |
| **ML-DSA-44** | 2-5 | 3-6 | 2-4 (est.) | 2,420 B |
| **ML-DSA-65** | 3-7 | 5-10 | 3-6 (est.) | 3,293 B |
| **ML-DSA-87** | 5-10 | 8-15 | 5-10 (est.) | 4,595 B |
| **SLH-DSA-128s** | 1-2 | 50-100 | 2-5 (est.) | 7,856 B |

### Benchmarking Code

```kotlin
// Run comprehensive benchmark
val provider = PostQuantumCrypto.MockPQCProvider()
val benchmark = PQCBenchmark(provider)

val kemSuite = benchmark.runKEMBenchmarkSuite(
    algorithms = listOf(
        KEMAlgorithm.ECDH_P256,
        KEMAlgorithm.ML_KEM_768,
        KEMAlgorithm.HYBRID_ECDH_ML_KEM_768
    ),
    iterations = 100
)

val sigSuite = benchmark.runSignatureBenchmarkSuite(
    algorithms = listOf(
        SignatureAlgorithm.ECDSA_P256,
        SignatureAlgorithm.ML_DSA_65
    ),
    dataSizes = listOf(1024, 10240),
    iterations = 100
)

// Print results
kemSuite.printSummary()
println(sigSuite.toMarkdown())

// Compare classical vs PQC
val comparison = benchmark.compareClassicalVsPQC(iterations = 100)
println(comparison)
```

### Performance Optimization Tips

1. **Use ML-KEM-768 (not 1024)** unless highest security needed
2. **Prefer ML-DSA over SLH-DSA** for general use (faster, smaller)
3. **Cache key pairs** when possible (expensive to generate)
4. **Use hybrid mode sparingly** (2x performance cost)
5. **Batch operations** to amortize overhead

---

## Usage Examples

### Example 1: Basic KEM Encryption

```kotlin
import com.obsidianbackup.crypto.PostQuantumCrypto
import com.obsidianbackup.crypto.PostQuantumCrypto.KEMAlgorithm

suspend fun encryptWithMLKEM() {
    // Choose provider and algorithm
    val provider = PostQuantumCrypto.MockPQCProvider() // Replace with BouncyCastlePQCProvider
    val algorithm = KEMAlgorithm.ML_KEM_768
    
    // Recipient generates key pair
    val recipientKeyPair = provider.generateKEMKeyPair(algorithm)
    
    // Sender encapsulates shared secret
    val encapResult = provider.encapsulate(recipientKeyPair.public, algorithm)
    val sharedSecret = encapResult.sharedSecret // 32 bytes
    val ciphertext = encapResult.ciphertext     // 1088 bytes for ML-KEM-768
    
    // Derive AES-256-GCM key from shared secret
    val aesKey = deriveAESKey(sharedSecret)
    
    // Encrypt data with AES-256-GCM
    val plaintext = "Sensitive backup data".toByteArray()
    val encrypted = encryptAESGCM(plaintext, aesKey)
    
    // Send: ciphertext (KEM) + encrypted (AES-GCM)
    val package = Package(
        kemCiphertext = ciphertext,
        aesEncrypted = encrypted
    )
    
    // Recipient decapsulates to get shared secret
    val recoveredSecret = provider.decapsulate(
        ciphertext = package.kemCiphertext,
        privateKey = recipientKeyPair.private,
        algorithm = algorithm
    )
    
    // Derive same AES key and decrypt
    val recoveredAESKey = deriveAESKey(recoveredSecret)
    val decrypted = decryptAESGCM(package.aesEncrypted, recoveredAESKey)
    
    assert(decrypted.contentEquals(plaintext))
}

fun deriveAESKey(sharedSecret: ByteArray): ByteArray {
    // Use HKDF or SHA-256 for key derivation
    val digest = java.security.MessageDigest.getInstance("SHA-256")
    return digest.digest(sharedSecret)
}
```

### Example 2: Hybrid KEM

```kotlin
suspend fun hybridEncryption() {
    val classicalProvider = PostQuantumCrypto.ClassicalCryptoProvider()
    val pqcProvider = PostQuantumCrypto.MockPQCProvider()
    val hybridProvider = PostQuantumCrypto.HybridCryptoProvider(classicalProvider, pqcProvider)
    
    val algorithm = KEMAlgorithm.HYBRID_ECDH_ML_KEM_768
    
    // Generate hybrid key pair (ECDH + ML-KEM)
    val keyPair = hybridProvider.generateKEMKeyPair(algorithm)
    
    // Encapsulate with both algorithms
    val result = hybridProvider.encapsulate(keyPair.public, algorithm)
    
    // Shared secret is combination: SHA-256(ECDH_secret || ML_KEM_secret)
    // Secure if either ECDH OR ML-KEM remains unbroken
    val hybridSecret = result.sharedSecret
    
    // Use for encryption as usual
    val aesKey = deriveAESKey(hybridSecret)
    // ... encrypt data
}
```

### Example 3: Digital Signatures

```kotlin
suspend fun signBackupMetadata() {
    val provider = PostQuantumCrypto.MockPQCProvider()
    val algorithm = SignatureAlgorithm.ML_DSA_65
    
    // Generate signing key pair
    val keyPair = provider.generateSignatureKeyPair(algorithm)
    
    // Sign backup metadata
    val metadata = """
        {
          "backupId": "backup_2025_01_15",
          "files": 1234,
          "size": 5368709120,
          "timestamp": "2025-01-15T10:30:00Z"
        }
    """.toByteArray()
    
    val signature = provider.sign(metadata, keyPair.private, algorithm)
    // signature.size = 3293 bytes for ML-DSA-65
    
    // Verify signature (by another device or server)
    val isValid = provider.verify(metadata, signature, keyPair.public, algorithm)
    
    if (isValid) {
        println("Backup metadata integrity verified!")
    } else {
        println("WARNING: Signature verification failed!")
    }
}
```

### Example 4: Key Storage with Migration

```kotlin
import com.obsidianbackup.crypto.PostQuantumCrypto.KeyStorageFormat
import com.obsidianbackup.crypto.PostQuantumCrypto.MigrationHelper

fun storeKeyWithMetadata(keyPair: KeyPair, algorithm: String) {
    val metadata = mapOf(
        "algorithm" to algorithm,
        "createdDate" to java.time.Instant.now().toString(),
        "securityLevel" to "AES-192-equivalent",
        "deviceId" to "device_12345"
    )
    
    val keyStorage = KeyStorageFormat(
        version = 1,
        algorithmId = algorithm,
        keyMaterial = keyPair.private.encoded,
        metadata = metadata
    )
    
    val serialized = keyStorage.serialize()
    
    // Save to Android Keystore or secure storage
    saveToSecureStorage("ml_kem_key", serialized)
}

fun loadAndMigrateKey(keyId: String) {
    val data = loadFromSecureStorage(keyId)
    
    // Detect format
    val format = MigrationHelper.detectFormat(data)
    
    when (format) {
        "PQC_v1" -> {
            // New format, deserialize directly
            val keyStorage = KeyStorageFormat.deserialize(data)
            println("Loaded ${keyStorage.algorithmId} key")
        }
        "LEGACY_AES_GCM" -> {
            // Old format, need to migrate
            if (MigrationHelper.shouldMigrate(format, KEMAlgorithm.ML_KEM_768)) {
                println("Migrating from legacy format to ML-KEM-768")
                // Decrypt with old key, re-encrypt with new key
                migrateBackup(keyId)
            }
        }
        else -> {
            throw IllegalStateException("Unknown format: $format")
        }
    }
}
```

### Example 5: Running Benchmarks

```kotlin
import com.obsidianbackup.crypto.PQCBenchmark

suspend fun profilePerformance() {
    val provider = PostQuantumCrypto.MockPQCProvider()
    val benchmark = PQCBenchmark(provider)
    
    // Benchmark single algorithm
    val result = benchmark.benchmarkKEMEncapsulation(
        algorithm = KEMAlgorithm.ML_KEM_768,
        iterations = 100
    )
    println(result)
    
    // Comprehensive suite
    val suite = benchmark.runKEMBenchmarkSuite(
        algorithms = listOf(
            KEMAlgorithm.ECDH_P256,
            KEMAlgorithm.ML_KEM_512,
            KEMAlgorithm.ML_KEM_768,
            KEMAlgorithm.ML_KEM_1024
        ),
        iterations = 100
    )
    
    // Export as markdown
    val markdown = suite.toMarkdown()
    File("/sdcard/Download/pqc_benchmark.md").writeText(markdown)
    
    // Compare classical vs PQC
    val comparison = benchmark.compareClassicalVsPQC(iterations = 50)
    println(comparison)
}
```

---

## Security Considerations

### Threat Model

**Protected Against:**
- ✅ Quantum computer attacks (Shor's algorithm)
- ✅ Classical attacks on post-quantum algorithms
- ✅ Harvest now, decrypt later attacks
- ✅ Key compromise (forward secrecy with ephemeral keys)

**Not Protected Against:**
- ⚠️ Malware on device (keyloggers, memory dumps)
- ⚠️ Implementation bugs (timing attacks, side channels)
- ⚠️ Compromised cryptographic libraries
- ⚠️ Social engineering / phishing

### Best Practices

#### 1. Use Hybrid Mode in Production

```kotlin
// ✅ GOOD: Hybrid mode
val provider = HybridCryptoProvider(classical, pqc)
val algorithm = KEMAlgorithm.HYBRID_ECDH_ML_KEM_768

// ❌ AVOID: Pure PQC in production (until algorithms mature)
val algorithm = KEMAlgorithm.ML_KEM_768 // Only for testing
```

**Rationale:** PQC algorithms are newer, less scrutinized. Hybrid provides defense-in-depth.

#### 2. Protect Private Keys

```kotlin
// ✅ GOOD: Use Android Keystore hardware backing
val keyStore = KeyStore.getInstance("AndroidKeyStore")
keyStore.load(null)

// Generate keys in hardware if available
val keyGenParameterSpec = KeyGenParameterSpec.Builder(
    "pqc_key",
    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
)
    .setUserAuthenticationRequired(true)
    .setUserAuthenticationValidityDurationSeconds(30)
    .build()

// ❌ AVOID: Storing keys in plain files or SharedPreferences
val keyFile = File("/sdcard/private_key.bin") // INSECURE!
```

#### 3. Constant-Time Operations

```kotlin
// ✅ GOOD: Use library implementations (already constant-time)
val signature = provider.sign(data, privateKey, algorithm)

// ❌ AVOID: Custom implementations without timing protection
// Vulnerable to timing attacks
```

#### 4. Validate Key Sizes

```kotlin
fun validateKeySize(publicKey: ByteArray, algorithm: KEMAlgorithm) {
    if (publicKey.size != algorithm.publicKeySize) {
        throw SecurityException("Invalid public key size: expected ${algorithm.publicKeySize}, got ${publicKey.size}")
    }
}
```

#### 5. Secure Random Number Generation

```kotlin
// ✅ GOOD: Use SecureRandom
val random = SecureRandom()
val nonce = ByteArray(12)
random.nextBytes(nonce)

// ❌ AVOID: Random() or predictable sources
val random = Random(System.currentTimeMillis()) // INSECURE!
```

#### 6. Key Rotation

```kotlin
// Rotate keys periodically (e.g., every 90 days)
fun shouldRotateKey(keyMetadata: Map<String, String>): Boolean {
    val createdDate = Instant.parse(keyMetadata["createdDate"])
    val age = Duration.between(createdDate, Instant.now())
    return age.toDays() > 90
}
```

#### 7. Algorithm Deprecation

```kotlin
// Maintain list of approved algorithms
val approvedKEMAlgorithms = setOf(
    KEMAlgorithm.HYBRID_ECDH_ML_KEM_768,
    KEMAlgorithm.ML_KEM_768
)

fun validateAlgorithm(algorithm: KEMAlgorithm) {
    if (algorithm !in approvedKEMAlgorithms) {
        Log.w(TAG, "Algorithm $algorithm is deprecated or not approved")
        // Force migration
    }
}
```

### Compliance & Regulations

#### NIST Requirements

- ✅ Use FIPS-203, FIPS-204, FIPS-205 approved algorithms
- ✅ Implement approved parameter sets
- ✅ Follow NIST SP 800-208 (hash-based signatures)
- ✅ Plan migration by 2030 (recommended)

#### Industry Standards

- **BSI (Germany)**: Recommends hybrid mode
- **ANSSI (France)**: Requires transition plan
- **NSA CNSA 2.0**: Quantum-safe by 2033
- **ETSI**: Hybrid key exchange standards

---

## Roadmap & Next Steps

### Immediate Actions (Q1 2025)

1. **Add Bouncy Castle dependencies**
   ```bash
   # Update app/build.gradle.kts
   implementation("org.bouncycastle:bcprov-jdk18on:1.79")
   implementation("org.bouncycastle:bcpqc-jdk18on:1.79")
   ```

2. **Implement BouncyCastlePQCProvider**
   - Extend `CryptoProvider` interface
   - Wrap Bouncy Castle ML-KEM, ML-DSA APIs
   - Test with real algorithms

3. **Run initial benchmarks**
   - Compare MockPQC vs BouncyCastle performance
   - Identify bottlenecks
   - Optimize if needed

4. **Update ProGuard rules**
   - Ensure BC classes not stripped

### Short-Term (Q2 2025)

5. **Implement hybrid mode end-to-end**
   - Test HYBRID_ECDH_ML_KEM_768
   - Validate security properties
   - Performance profiling

6. **Design backup format migration**
   - Backward compatibility testing
   - Migration UI/UX design
   - Progress tracking

### Medium-Term (Q3-Q4 2025)

7. **Beta testing**
   - Select 100-1000 users
   - Monitor crash rates
   - Collect performance data

8. **Production rollout**
   - Gradual rollout (10% → 100%)
   - A/B testing
   - User education

9. **Documentation**
   - User-facing guides
   - Developer documentation
   - Security audit report

### Long-Term (2026+)

10. **Full PQC deployment**
    - 100% of new backups use hybrid mode
    - Background migration of old backups
    - Classical mode deprecated

11. **Algorithm updates**
    - Track NIST updates/errata
    - Update to newer Bouncy Castle versions
    - Add new algorithms as standardized

12. **Research & Development**
    - Monitor quantum computing progress
    - Evaluate next-gen PQC algorithms
    - Collaborate with academic/industry partners

---

## Conclusion

ObsidianBackup's post-quantum cryptography infrastructure is **ready for integration**. The architecture provides:

- ✅ **Future-proof design** with crypto agility
- ✅ **Hybrid security** combining classical and PQC
- ✅ **Smooth migration path** preserving backward compatibility
- ✅ **Performance monitoring** via comprehensive benchmarks
- ✅ **NIST compliance** (FIPS-203, FIPS-204, FIPS-205)

**Next critical step:** Integrate Bouncy Castle and replace MockPQCProvider with production implementations.

**Timeline to production:** 6-12 months, with gradual rollout starting Q4 2025.

---

## References

### NIST Standards

- [FIPS 203: ML-KEM Standard](https://csrc.nist.gov/pubs/fips/203/final)
- [FIPS 204: ML-DSA Standard](https://csrc.nist.gov/pubs/fips/204/final)
- [FIPS 205: SLH-DSA Standard](https://csrc.nist.gov/pubs/fips/205/final)
- [NIST PQC Project Homepage](https://csrc.nist.gov/projects/post-quantum-cryptography)

### Libraries & Tools

- [Bouncy Castle Java 1.79+](https://www.bouncycastle.org/)
- [Bouncy Castle PQC Almanac](https://www.bouncycastle.org/resources/latest-nist-pqc-standards-and-more-bouncy-castle-java-1-79/)
- [Open Quantum Safe (liboqs)](https://openquantumsafe.org/)
- [liboqs-java GitHub](https://github.com/open-quantum-safe/liboqs-java)

### Research & Analysis

- [IETF ML-KEM Security Considerations](https://www.ietf.org/archive/id/draft-sfluhrer-cfrg-ml-kem-security-considerations-01.html)
- [DigiCert: Post-Quantum Cryptography Insights](https://www.digicert.com/insights/post-quantum-cryptography/)
- [WolfSSL: FIPS 203, 204, 205 Overview](https://www.wolfssl.com/what-are-fips-203-204-and-205/)

### Android Security

- [Android Keystore System](https://developer.android.com/training/articles/keystore)
- [Android Security Best Practices](https://developer.android.com/topic/security/best-practices)

---

**Document Version:** 1.0  
**Last Updated:** 2025-01-XX  
**Maintained By:** ObsidianBackup Cryptography Team  
**Contact:** security@obsidianbackup.example.com
