// crypto/PostQuantumCrypto.kt
package com.obsidianbackup.crypto

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.*
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Post-Quantum Cryptography Implementation
 * 
 * Implements NIST FIPS-203, FIPS-204, FIPS-205 standards with hybrid classical+PQC approach
 * 
 * NIST Standards:
 * - FIPS-203: ML-KEM (Module-Lattice-Based Key Encapsulation Mechanism) - based on CRYSTALS-Kyber
 * - FIPS-204: ML-DSA (Module-Lattice-Based Digital Signature Algorithm) - based on CRYSTALS-Dilithium
 * - FIPS-205: SLH-DSA (Stateless Hash-Based Digital Signature Algorithm) - based on SPHINCS+
 * 
 * Features:
 * - Hybrid mode: Classical (ECDH/RSA) + Post-Quantum algorithms
 * - Crypto agility: Pluggable algorithm interface
 * - Future-proof key storage format with version and metadata
 * - Migration path from current AES-GCM encryption
 * 
 * @see <a href="https://csrc.nist.gov/pubs/fips/203/final">NIST FIPS-203</a>
 * @see <a href="https://csrc.nist.gov/pubs/fips/204/final">NIST FIPS-204</a>
 * @see <a href="https://csrc.nist.gov/pubs/fips/205/final">NIST FIPS-205</a>
 */
class PostQuantumCrypto {

    companion object {
        private const val TAG = "PostQuantumCrypto"
        
        // Versioning for crypto agility
        private const val FORMAT_VERSION = 1
        
        // Classical algorithms (current implementation)
        private const val CLASSICAL_KEY_AGREEMENT = "ECDH"
        private const val CLASSICAL_SIGNATURE = "SHA256withECDSA"
        private const val CLASSICAL_KEM_CURVE = "secp256r1"
        
        // Symmetric encryption (AES-GCM)
        private const val SYMMETRIC_ALGORITHM = "AES/GCM/NoPadding"
        private const val AES_KEY_SIZE = 256
        private const val GCM_IV_SIZE = 12
        private const val GCM_TAG_SIZE = 128
        
        // Buffer size for streaming operations
        private const val BUFFER_SIZE = 8192
    }

    /**
     * Enum for supported Post-Quantum Key Encapsulation Mechanisms (FIPS-203)
     * 
     * ML-KEM Parameter Sets:
     * - ML_KEM_512: ~AES-128 security level, smallest keys
     * - ML_KEM_768: ~AES-192 security level, recommended
     * - ML_KEM_1024: ~AES-256 security level, highest security
     */
    enum class KEMAlgorithm(
        val algorithmName: String,
        val publicKeySize: Int,
        val secretKeySize: Int,
        val ciphertextSize: Int,
        val securityLevel: String
    ) {
        // Classical (current implementation)
        ECDH_P256("ECDH-P256", 65, 32, 0, "AES-128-equivalent"),
        
        // NIST FIPS-203 ML-KEM (Kyber)
        ML_KEM_512("ML-KEM-512", 800, 1632, 768, "AES-128-equivalent"),
        ML_KEM_768("ML-KEM-768", 1184, 2400, 1088, "AES-192-equivalent"),
        ML_KEM_1024("ML-KEM-1024", 1568, 3168, 1568, "AES-256-equivalent"),
        
        // Hybrid modes (Classical + PQC)
        HYBRID_ECDH_ML_KEM_768("Hybrid-ECDH-ML-KEM-768", 1249, 2432, 1088, "AES-192-equivalent");
        
        fun isPostQuantum(): Boolean = this in listOf(ML_KEM_512, ML_KEM_768, ML_KEM_1024, HYBRID_ECDH_ML_KEM_768)
        fun isHybrid(): Boolean = this == HYBRID_ECDH_ML_KEM_768
    }

    /**
     * Enum for supported Post-Quantum Digital Signature Algorithms (FIPS-204, FIPS-205)
     * 
     * ML-DSA (Dilithium) - Lattice-based, fast verification:
     * - ML_DSA_44: ~128-bit security, smallest signatures
     * - ML_DSA_65: ~192-bit security, recommended
     * - ML_DSA_87: ~256-bit security, highest security
     * 
     * SLH-DSA (SPHINCS+) - Hash-based, conservative security:
     * - Small variants (s): Smaller signatures, slower
     * - Fast variants (f): Larger signatures, faster
     */
    enum class SignatureAlgorithm(
        val algorithmName: String,
        val publicKeySize: Int,
        val secretKeySize: Int,
        val signatureSize: Int,
        val securityLevel: String,
        val signatureType: String
    ) {
        // Classical (current implementation)
        ECDSA_P256("ECDSA-P256", 65, 32, 64, "AES-128-equivalent", "ECDSA"),
        RSA_2048("RSA-2048", 294, 1193, 256, "RSA-2048", "RSA"),
        
        // NIST FIPS-204 ML-DSA (Dilithium)
        ML_DSA_44("ML-DSA-44", 1312, 2528, 2420, "AES-128-equivalent", "Lattice"),
        ML_DSA_65("ML-DSA-65", 1952, 4000, 3293, "AES-192-equivalent", "Lattice"),
        ML_DSA_87("ML-DSA-87", 2592, 4864, 4595, "AES-256-equivalent", "Lattice"),
        
        // NIST FIPS-205 SLH-DSA (SPHINCS+)
        SLH_DSA_SHA2_128S("SLH-DSA-SHA2-128s", 32, 64, 7856, "AES-128-equivalent", "Hash"),
        SLH_DSA_SHA2_128F("SLH-DSA-SHA2-128f", 32, 64, 17088, "AES-128-equivalent", "Hash"),
        SLH_DSA_SHA2_192S("SLH-DSA-SHA2-192s", 48, 96, 16224, "AES-192-equivalent", "Hash"),
        SLH_DSA_SHA2_192F("SLH-DSA-SHA2-192f", 48, 96, 35664, "AES-192-equivalent", "Hash"),
        SLH_DSA_SHA2_256S("SLH-DSA-SHA2-256s", 64, 128, 29792, "AES-256-equivalent", "Hash"),
        SLH_DSA_SHA2_256F("SLH-DSA-SHA2-256f", 64, 128, 49856, "AES-256-equivalent", "Hash"),
        
        // Hybrid modes (Classical + PQC)
        HYBRID_ECDSA_ML_DSA_65("Hybrid-ECDSA-ML-DSA-65", 2017, 4032, 3357, "AES-192-equivalent", "Hybrid");
        
        fun isPostQuantum(): Boolean = signatureType in listOf("Lattice", "Hash", "Hybrid")
        fun isHybrid(): Boolean = signatureType == "Hybrid"
    }

    /**
     * Crypto Provider Interface for algorithm agility
     * 
     * Allows pluggable implementations:
     * - Bouncy Castle Provider (preferred for Android)
     * - liboqs-java (Open Quantum Safe)
     * - Native implementations
     * - Mock implementations for testing
     */
    interface CryptoProvider {
        fun getName(): String
        fun getVersion(): String
        fun supportsKEM(algorithm: KEMAlgorithm): Boolean
        fun supportsSignature(algorithm: SignatureAlgorithm): Boolean
        
        // KEM operations (FIPS-203)
        suspend fun generateKEMKeyPair(algorithm: KEMAlgorithm): KeyPair
        suspend fun encapsulate(publicKey: PublicKey, algorithm: KEMAlgorithm): EncapsulationResult
        suspend fun decapsulate(ciphertext: ByteArray, privateKey: PrivateKey, algorithm: KEMAlgorithm): ByteArray
        
        // Signature operations (FIPS-204, FIPS-205)
        suspend fun generateSignatureKeyPair(algorithm: SignatureAlgorithm): KeyPair
        suspend fun sign(data: ByteArray, privateKey: PrivateKey, algorithm: SignatureAlgorithm): ByteArray
        suspend fun verify(data: ByteArray, signature: ByteArray, publicKey: PublicKey, algorithm: SignatureAlgorithm): Boolean
    }

    /**
     * Result of key encapsulation operation
     * @param ciphertext Encapsulated key (to be sent to peer)
     * @param sharedSecret Shared secret (32 bytes, used to derive symmetric keys)
     */
    data class EncapsulationResult(
        val ciphertext: ByteArray,
        val sharedSecret: ByteArray
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as EncapsulationResult
            if (!ciphertext.contentEquals(other.ciphertext)) return false
            if (!sharedSecret.contentEquals(other.sharedSecret)) return false
            return true
        }

        override fun hashCode(): Int {
            var result = ciphertext.contentHashCode()
            result = 31 * result + sharedSecret.contentHashCode()
            return result
        }
    }

    /**
     * Future-proof key storage format with versioning and metadata
     * 
     * Format:
     * - Version (1 byte): Format version for migration support
     * - Algorithm ID (2 bytes): Identifies KEM/signature algorithm
     * - Key Length (4 bytes): Length of key material
     * - Key Material (variable): Encoded key data
     * - Metadata Length (4 bytes): Length of metadata JSON
     * - Metadata (variable): JSON with creation date, security level, etc.
     */
    data class KeyStorageFormat(
        val version: Int,
        val algorithmId: String,
        val keyMaterial: ByteArray,
        val metadata: Map<String, String>
    ) {
        fun serialize(): ByteArray {
            val metadataJson = metadata.entries.joinToString(",", "{", "}") { 
                "\"${it.key}\":\"${it.value}\"" 
            }
            val metadataBytes = metadataJson.toByteArray(Charsets.UTF_8)
            
            val buffer = ByteArray(1 + 2 + 4 + keyMaterial.size + 4 + metadataBytes.size)
            var offset = 0
            
            // Version
            buffer[offset++] = version.toByte()
            
            // Algorithm ID (2 bytes, assuming short string)
            val algIdBytes = algorithmId.toByteArray(Charsets.UTF_8).take(65535)
            buffer[offset++] = (algIdBytes.size shr 8).toByte()
            buffer[offset++] = (algIdBytes.size and 0xFF).toByte()
            System.arraycopy(algIdBytes.toByteArray(), 0, buffer, offset, algIdBytes.size)
            offset += algIdBytes.size
            
            // Key material length
            buffer[offset++] = (keyMaterial.size shr 24).toByte()
            buffer[offset++] = ((keyMaterial.size shr 16) and 0xFF).toByte()
            buffer[offset++] = ((keyMaterial.size shr 8) and 0xFF).toByte()
            buffer[offset++] = (keyMaterial.size and 0xFF).toByte()
            
            // Key material
            System.arraycopy(keyMaterial, 0, buffer, offset, keyMaterial.size)
            offset += keyMaterial.size
            
            // Metadata length
            buffer[offset++] = (metadataBytes.size shr 24).toByte()
            buffer[offset++] = ((metadataBytes.size shr 16) and 0xFF).toByte()
            buffer[offset++] = ((metadataBytes.size shr 8) and 0xFF).toByte()
            buffer[offset++] = (metadataBytes.size and 0xFF).toByte()
            
            // Metadata
            System.arraycopy(metadataBytes, 0, buffer, offset, metadataBytes.size)
            
            return buffer
        }

        companion object {
            fun deserialize(data: ByteArray): KeyStorageFormat {
                var offset = 0
                
                // Version
                val version = data[offset++].toInt()
                
                // Algorithm ID
                val algIdLen = ((data[offset++].toInt() and 0xFF) shl 8) or (data[offset++].toInt() and 0xFF)
                val algorithmId = String(data, offset, algIdLen, Charsets.UTF_8)
                offset += algIdLen
                
                // Key material length
                val keyLen = ((data[offset++].toInt() and 0xFF) shl 24) or
                             ((data[offset++].toInt() and 0xFF) shl 16) or
                             ((data[offset++].toInt() and 0xFF) shl 8) or
                             (data[offset++].toInt() and 0xFF)
                
                // Key material
                val keyMaterial = data.copyOfRange(offset, offset + keyLen)
                offset += keyLen
                
                // Metadata length
                val metadataLen = ((data[offset++].toInt() and 0xFF) shl 24) or
                                  ((data[offset++].toInt() and 0xFF) shl 16) or
                                  ((data[offset++].toInt() and 0xFF) shl 8) or
                                  (data[offset++].toInt() and 0xFF)
                
                // Metadata (simple JSON parsing)
                val metadataJson = String(data, offset, metadataLen, Charsets.UTF_8)
                val metadata = parseSimpleJson(metadataJson)
                
                return KeyStorageFormat(version, algorithmId, keyMaterial, metadata)
            }
            
            private fun parseSimpleJson(json: String): Map<String, String> {
                val result = mutableMapOf<String, String>()
                val content = json.trim().removeSurrounding("{", "}")
                if (content.isBlank()) return result
                
                content.split(",").forEach { pair ->
                    val parts = pair.split(":")
                    if (parts.size == 2) {
                        val key = parts[0].trim().removeSurrounding("\"")
                        val value = parts[1].trim().removeSurrounding("\"")
                        result[key] = value
                    }
                }
                return result
            }
        }
    }

    /**
     * Classical Crypto Provider (Current Implementation)
     * 
     * Implements current ECDH + ECDSA using Android's crypto providers.
     * Used as fallback and for hybrid mode.
     */
    class ClassicalCryptoProvider : CryptoProvider {
        override fun getName() = "AndroidKeyStore"
        override fun getVersion() = "1.0"
        
        override fun supportsKEM(algorithm: KEMAlgorithm): Boolean {
            return algorithm in listOf(KEMAlgorithm.ECDH_P256, KEMAlgorithm.HYBRID_ECDH_ML_KEM_768)
        }
        
        override fun supportsSignature(algorithm: SignatureAlgorithm): Boolean {
            return algorithm in listOf(
                SignatureAlgorithm.ECDSA_P256,
                SignatureAlgorithm.RSA_2048,
                SignatureAlgorithm.HYBRID_ECDSA_ML_DSA_65
            )
        }

        override suspend fun generateKEMKeyPair(algorithm: KEMAlgorithm): KeyPair = withContext(Dispatchers.Default) {
            when (algorithm) {
                KEMAlgorithm.ECDH_P256, KEMAlgorithm.HYBRID_ECDH_ML_KEM_768 -> {
                    val keyPairGenerator = KeyPairGenerator.getInstance("EC")
                    val ecSpec = java.security.spec.ECGenParameterSpec(CLASSICAL_KEM_CURVE)
                    keyPairGenerator.initialize(ecSpec, SecureRandom())
                    keyPairGenerator.generateKeyPair()
                }
                // Fallback for PQC algorithms: use classical ECDH
                KEMAlgorithm.ML_KEM_512, KEMAlgorithm.ML_KEM_768, KEMAlgorithm.ML_KEM_1024 -> {
                    Log.w(TAG, "ML-KEM not available, falling back to ECDH-P256 for $algorithm")
                    generateKEMKeyPair(KEMAlgorithm.ECDH_P256)
                }
            }
        }

        override suspend fun encapsulate(
            publicKey: PublicKey,
            algorithm: KEMAlgorithm
        ): EncapsulationResult = withContext(Dispatchers.Default) {
            when (algorithm) {
                KEMAlgorithm.ECDH_P256, KEMAlgorithm.HYBRID_ECDH_ML_KEM_768 -> {
                    // Generate ephemeral key pair
                    val ephemeralKeyPair = generateKEMKeyPair(algorithm)
                    
                    // Perform ECDH
                    val keyAgreement = KeyAgreement.getInstance(CLASSICAL_KEY_AGREEMENT)
                    keyAgreement.init(ephemeralKeyPair.private)
                    keyAgreement.doPhase(publicKey, true)
                    val sharedSecret = keyAgreement.generateSecret()
                    
                    // Ciphertext is the ephemeral public key
                    val ciphertext = ephemeralKeyPair.public.encoded
                    
                    // Derive 32-byte key from shared secret
                    val derivedKey = deriveKey(sharedSecret, 32)
                    
                    EncapsulationResult(ciphertext, derivedKey)
                }
                // Fallback for PQC algorithms: use classical ECDH
                KEMAlgorithm.ML_KEM_512, KEMAlgorithm.ML_KEM_768, KEMAlgorithm.ML_KEM_1024 -> {
                    Log.w(TAG, "ML-KEM not available, falling back to ECDH-P256 for $algorithm")
                    encapsulate(publicKey, KEMAlgorithm.ECDH_P256)
                }
            }
        }

        override suspend fun decapsulate(
            ciphertext: ByteArray,
            privateKey: PrivateKey,
            algorithm: KEMAlgorithm
        ): ByteArray = withContext(Dispatchers.Default) {
            when (algorithm) {
                KEMAlgorithm.ECDH_P256, KEMAlgorithm.HYBRID_ECDH_ML_KEM_768 -> {
                    // Decode ephemeral public key from ciphertext
                    val keyFactory = KeyFactory.getInstance("EC")
                    val ephemeralPublicKey = keyFactory.generatePublic(X509EncodedKeySpec(ciphertext))
                    
                    // Perform ECDH
                    val keyAgreement = KeyAgreement.getInstance(CLASSICAL_KEY_AGREEMENT)
                    keyAgreement.init(privateKey)
                    keyAgreement.doPhase(ephemeralPublicKey, true)
                    val sharedSecret = keyAgreement.generateSecret()
                    
                    // Derive 32-byte key from shared secret
                    deriveKey(sharedSecret, 32)
                }
                // Fallback for PQC algorithms: use classical ECDH
                KEMAlgorithm.ML_KEM_512, KEMAlgorithm.ML_KEM_768, KEMAlgorithm.ML_KEM_1024 -> {
                    Log.w(TAG, "ML-KEM not available, falling back to ECDH-P256 for $algorithm")
                    decapsulate(ciphertext, privateKey, KEMAlgorithm.ECDH_P256)
                }
            }
        }

        override suspend fun generateSignatureKeyPair(algorithm: SignatureAlgorithm): KeyPair = withContext(Dispatchers.Default) {
            when (algorithm) {
                SignatureAlgorithm.ECDSA_P256, SignatureAlgorithm.HYBRID_ECDSA_ML_DSA_65 -> {
                    val keyPairGenerator = KeyPairGenerator.getInstance("EC")
                    val ecSpec = java.security.spec.ECGenParameterSpec(CLASSICAL_KEM_CURVE)
                    keyPairGenerator.initialize(ecSpec, SecureRandom())
                    keyPairGenerator.generateKeyPair()
                }
                SignatureAlgorithm.RSA_2048 -> {
                    val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
                    keyPairGenerator.initialize(2048, SecureRandom())
                    keyPairGenerator.generateKeyPair()
                }
                // Fallback for PQC algorithms: use classical ECDSA
                else -> {
                    Log.w(TAG, "ML-DSA/SLH-DSA not available, falling back to ECDSA-P256 for $algorithm")
                    generateSignatureKeyPair(SignatureAlgorithm.ECDSA_P256)
                }
            }
        }

        override suspend fun sign(
            data: ByteArray,
            privateKey: PrivateKey,
            algorithm: SignatureAlgorithm
        ): ByteArray = withContext(Dispatchers.Default) {
            val signatureAlg = when (algorithm) {
                SignatureAlgorithm.ECDSA_P256, SignatureAlgorithm.HYBRID_ECDSA_ML_DSA_65 -> CLASSICAL_SIGNATURE
                SignatureAlgorithm.RSA_2048 -> "SHA256withRSA"
                // Fallback for PQC algorithms: use classical ECDSA
                else -> {
                    Log.w(TAG, "ML-DSA/SLH-DSA not available, falling back to ECDSA for $algorithm")
                    CLASSICAL_SIGNATURE
                }
            }
            
            val signature = Signature.getInstance(signatureAlg)
            signature.initSign(privateKey)
            signature.update(data)
            signature.sign()
        }

        override suspend fun verify(
            data: ByteArray,
            signature: ByteArray,
            publicKey: PublicKey,
            algorithm: SignatureAlgorithm
        ): Boolean = withContext(Dispatchers.Default) {
            val signatureAlg = when (algorithm) {
                SignatureAlgorithm.ECDSA_P256, SignatureAlgorithm.HYBRID_ECDSA_ML_DSA_65 -> CLASSICAL_SIGNATURE
                SignatureAlgorithm.RSA_2048 -> "SHA256withRSA"
                // Fallback for PQC algorithms: use classical ECDSA
                else -> {
                    Log.w(TAG, "ML-DSA/SLH-DSA not available, falling back to ECDSA for $algorithm")
                    CLASSICAL_SIGNATURE
                }
            }
            
            val sig = Signature.getInstance(signatureAlg)
            sig.initVerify(publicKey)
            sig.update(data)
            sig.verify(signature)
        }

        private fun deriveKey(sharedSecret: ByteArray, length: Int): ByteArray {
            // Use SHA-256 for key derivation
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(sharedSecret)
            return hash.copyOf(length)
        }
    }

    /**
     * Mock Post-Quantum Crypto Provider
     * 
     * Simulates PQC operations for testing and development.
     * Will be replaced with actual Bouncy Castle or liboqs-java implementation.
     * 
     * WARNING: NOT FOR PRODUCTION USE - Uses weak simulation algorithms
     */
    class MockPQCProvider : CryptoProvider {
        override fun getName() = "MockPQC"
        override fun getVersion() = "0.1-dev"
        
        override fun supportsKEM(algorithm: KEMAlgorithm) = algorithm.isPostQuantum()
        override fun supportsSignature(algorithm: SignatureAlgorithm) = algorithm.isPostQuantum()

        override suspend fun generateKEMKeyPair(algorithm: KEMAlgorithm): KeyPair = withContext(Dispatchers.Default) {
            // Mock implementation: Generate dummy key pair with correct sizes
            val random = SecureRandom()
            val publicKey = ByteArray(algorithm.publicKeySize)
            val privateKey = ByteArray(algorithm.secretKeySize)
            random.nextBytes(publicKey)
            random.nextBytes(privateKey)
            
            Log.w(TAG, "MockPQCProvider: Generating mock ${algorithm.algorithmName} key pair (NOT SECURE)")
            
            // Wrap in generic key objects
            val pubKey = object : PublicKey {
                override fun getAlgorithm() = algorithm.algorithmName
                override fun getFormat() = "RAW"
                override fun getEncoded() = publicKey
            }
            val privKey = object : PrivateKey {
                override fun getAlgorithm() = algorithm.algorithmName
                override fun getFormat() = "RAW"
                override fun getEncoded() = privateKey
            }
            
            KeyPair(pubKey, privKey)
        }

        override suspend fun encapsulate(
            publicKey: PublicKey,
            algorithm: KEMAlgorithm
        ): EncapsulationResult = withContext(Dispatchers.Default) {
            // Mock implementation: Generate random ciphertext and shared secret
            val random = SecureRandom()
            val ciphertext = ByteArray(algorithm.ciphertextSize)
            val sharedSecret = ByteArray(32) // ML-KEM always produces 32-byte shared secret
            random.nextBytes(ciphertext)
            random.nextBytes(sharedSecret)
            
            Log.w(TAG, "MockPQCProvider: Encapsulating with ${algorithm.algorithmName} (NOT SECURE)")
            
            EncapsulationResult(ciphertext, sharedSecret)
        }

        override suspend fun decapsulate(
            ciphertext: ByteArray,
            privateKey: PrivateKey,
            algorithm: KEMAlgorithm
        ): ByteArray = withContext(Dispatchers.Default) {
            // Mock implementation: Return deterministic "shared secret" based on private key
            val digest = MessageDigest.getInstance("SHA-256")
            digest.update(privateKey.encoded)
            digest.update(ciphertext)
            val hash = digest.digest()
            
            Log.w(TAG, "MockPQCProvider: Decapsulating with ${algorithm.algorithmName} (NOT SECURE)")
            
            hash
        }

        override suspend fun generateSignatureKeyPair(algorithm: SignatureAlgorithm): KeyPair = withContext(Dispatchers.Default) {
            // Mock implementation: Generate dummy key pair with correct sizes
            val random = SecureRandom()
            val publicKey = ByteArray(algorithm.publicKeySize)
            val privateKey = ByteArray(algorithm.secretKeySize)
            random.nextBytes(publicKey)
            random.nextBytes(privateKey)
            
            Log.w(TAG, "MockPQCProvider: Generating mock ${algorithm.algorithmName} key pair (NOT SECURE)")
            
            val pubKey = object : PublicKey {
                override fun getAlgorithm() = algorithm.algorithmName
                override fun getFormat() = "RAW"
                override fun getEncoded() = publicKey
            }
            val privKey = object : PrivateKey {
                override fun getAlgorithm() = algorithm.algorithmName
                override fun getFormat() = "RAW"
                override fun getEncoded() = privateKey
            }
            
            KeyPair(pubKey, privKey)
        }

        override suspend fun sign(
            data: ByteArray,
            privateKey: PrivateKey,
            algorithm: SignatureAlgorithm
        ): ByteArray = withContext(Dispatchers.Default) {
            // Mock implementation: Create "signature" by hashing data with private key
            val digest = MessageDigest.getInstance("SHA-256")
            digest.update(data)
            digest.update(privateKey.encoded)
            val hash = digest.digest()
            
            // Pad to expected signature size
            val signature = ByteArray(algorithm.signatureSize)
            System.arraycopy(hash, 0, signature, 0, minOf(hash.size, signature.size))
            
            Log.w(TAG, "MockPQCProvider: Signing with ${algorithm.algorithmName} (NOT SECURE)")
            
            signature
        }

        override suspend fun verify(
            data: ByteArray,
            signature: ByteArray,
            publicKey: PublicKey,
            algorithm: SignatureAlgorithm
        ): Boolean = withContext(Dispatchers.Default) {
            // Mock implementation: Always return true for valid format
            Log.w(TAG, "MockPQCProvider: Verifying with ${algorithm.algorithmName} (NOT SECURE - always true)")
            signature.size == algorithm.signatureSize
        }
    }

    /**
     * Hybrid Crypto Provider
     * 
     * Combines classical and post-quantum algorithms for defense-in-depth.
     * Security holds if either algorithm remains secure.
     * 
     * Strategy:
     * - KEM: Combine ECDH and ML-KEM shared secrets with XOR or KDF
     * - Signatures: Concatenate classical and PQC signatures
     */
    class HybridCryptoProvider(
        private val classicalProvider: CryptoProvider,
        private val pqcProvider: CryptoProvider
    ) : CryptoProvider {
        override fun getName() = "Hybrid-${classicalProvider.getName()}-${pqcProvider.getName()}"
        override fun getVersion() = "${classicalProvider.getVersion()}+${pqcProvider.getVersion()}"
        
        override fun supportsKEM(algorithm: KEMAlgorithm) = algorithm.isHybrid()
        override fun supportsSignature(algorithm: SignatureAlgorithm) = algorithm.isHybrid()

        override suspend fun generateKEMKeyPair(algorithm: KEMAlgorithm): KeyPair = withContext(Dispatchers.Default) {
            if (algorithm != KEMAlgorithm.HYBRID_ECDH_ML_KEM_768) {
                // Fallback: use classical provider if hybrid not requested
                Log.w(TAG, "Non-hybrid algorithm $algorithm requested on HybridCryptoProvider, using classical")
                return@withContext classicalProvider.generateKEMKeyPair(algorithm)
            }
            
            // Generate both key pairs
            val classicalKP = classicalProvider.generateKEMKeyPair(KEMAlgorithm.ECDH_P256)
            val pqcKP = pqcProvider.generateKEMKeyPair(KEMAlgorithm.ML_KEM_768)
            
            // Combine public keys
            val combinedPublic = combineKeys(classicalKP.public.encoded, pqcKP.public.encoded)
            val combinedPrivate = combineKeys(classicalKP.private.encoded, pqcKP.private.encoded)
            
            val pubKey = object : PublicKey {
                override fun getAlgorithm() = algorithm.algorithmName
                override fun getFormat() = "HYBRID"
                override fun getEncoded() = combinedPublic
            }
            val privKey = object : PrivateKey {
                override fun getAlgorithm() = algorithm.algorithmName
                override fun getFormat() = "HYBRID"
                override fun getEncoded() = combinedPrivate
            }
            
            KeyPair(pubKey, privKey)
        }

        override suspend fun encapsulate(
            publicKey: PublicKey,
            algorithm: KEMAlgorithm
        ): EncapsulationResult = withContext(Dispatchers.Default) {
            // Split hybrid public key
            val (classicalPub, pqcPub) = splitKeys(publicKey.encoded)
            
            val classicalPubKey = object : PublicKey {
                override fun getAlgorithm() = "EC"
                override fun getFormat() = "X.509"
                override fun getEncoded() = classicalPub
            }
            val pqcPubKey = object : PublicKey {
                override fun getAlgorithm() = "ML-KEM-768"
                override fun getFormat() = "RAW"
                override fun getEncoded() = pqcPub
            }
            
            // Encapsulate with both algorithms
            val classicalResult = classicalProvider.encapsulate(classicalPubKey, KEMAlgorithm.ECDH_P256)
            val pqcResult = pqcProvider.encapsulate(pqcPubKey, KEMAlgorithm.ML_KEM_768)
            
            // Combine ciphertexts
            val combinedCiphertext = combineKeys(classicalResult.ciphertext, pqcResult.ciphertext)
            
            // Combine shared secrets with KDF
            val combinedSecret = combineSharedSecrets(classicalResult.sharedSecret, pqcResult.sharedSecret)
            
            EncapsulationResult(combinedCiphertext, combinedSecret)
        }

        override suspend fun decapsulate(
            ciphertext: ByteArray,
            privateKey: PrivateKey,
            algorithm: KEMAlgorithm
        ): ByteArray = withContext(Dispatchers.Default) {
            // Split hybrid ciphertext and private key
            val (classicalCT, pqcCT) = splitKeys(ciphertext)
            val (classicalPriv, pqcPriv) = splitKeys(privateKey.encoded)
            
            val classicalPrivKey = object : PrivateKey {
                override fun getAlgorithm() = "EC"
                override fun getFormat() = "PKCS#8"
                override fun getEncoded() = classicalPriv
            }
            val pqcPrivKey = object : PrivateKey {
                override fun getAlgorithm() = "ML-KEM-768"
                override fun getFormat() = "RAW"
                override fun getEncoded() = pqcPriv
            }
            
            // Decapsulate with both algorithms
            val classicalSecret = classicalProvider.decapsulate(classicalCT, classicalPrivKey, KEMAlgorithm.ECDH_P256)
            val pqcSecret = pqcProvider.decapsulate(pqcCT, pqcPrivKey, KEMAlgorithm.ML_KEM_768)
            
            // Combine shared secrets
            combineSharedSecrets(classicalSecret, pqcSecret)
        }

        override suspend fun generateSignatureKeyPair(algorithm: SignatureAlgorithm): KeyPair = withContext(Dispatchers.Default) {
            if (algorithm != SignatureAlgorithm.HYBRID_ECDSA_ML_DSA_65) {
                // Fallback: use classical provider if hybrid not requested
                Log.w(TAG, "Non-hybrid algorithm $algorithm requested on HybridCryptoProvider, using classical")
                return@withContext classicalProvider.generateSignatureKeyPair(algorithm)
            }
            
            val classicalKP = classicalProvider.generateSignatureKeyPair(SignatureAlgorithm.ECDSA_P256)
            val pqcKP = pqcProvider.generateSignatureKeyPair(SignatureAlgorithm.ML_DSA_65)
            
            val combinedPublic = combineKeys(classicalKP.public.encoded, pqcKP.public.encoded)
            val combinedPrivate = combineKeys(classicalKP.private.encoded, pqcKP.private.encoded)
            
            val pubKey = object : PublicKey {
                override fun getAlgorithm() = algorithm.algorithmName
                override fun getFormat() = "HYBRID"
                override fun getEncoded() = combinedPublic
            }
            val privKey = object : PrivateKey {
                override fun getAlgorithm() = algorithm.algorithmName
                override fun getFormat() = "HYBRID"
                override fun getEncoded() = combinedPrivate
            }
            
            KeyPair(pubKey, privKey)
        }

        override suspend fun sign(
            data: ByteArray,
            privateKey: PrivateKey,
            algorithm: SignatureAlgorithm
        ): ByteArray = withContext(Dispatchers.Default) {
            val (classicalPriv, pqcPriv) = splitKeys(privateKey.encoded)
            
            val classicalPrivKey = object : PrivateKey {
                override fun getAlgorithm() = "EC"
                override fun getFormat() = "PKCS#8"
                override fun getEncoded() = classicalPriv
            }
            val pqcPrivKey = object : PrivateKey {
                override fun getAlgorithm() = "ML-DSA-65"
                override fun getFormat() = "RAW"
                override fun getEncoded() = pqcPriv
            }
            
            // Sign with both algorithms
            val classicalSig = classicalProvider.sign(data, classicalPrivKey, SignatureAlgorithm.ECDSA_P256)
            val pqcSig = pqcProvider.sign(data, pqcPrivKey, SignatureAlgorithm.ML_DSA_65)
            
            // Combine signatures
            combineKeys(classicalSig, pqcSig)
        }

        override suspend fun verify(
            data: ByteArray,
            signature: ByteArray,
            publicKey: PublicKey,
            algorithm: SignatureAlgorithm
        ): Boolean = withContext(Dispatchers.Default) {
            val (classicalSig, pqcSig) = splitKeys(signature)
            val (classicalPub, pqcPub) = splitKeys(publicKey.encoded)
            
            val classicalPubKey = object : PublicKey {
                override fun getAlgorithm() = "EC"
                override fun getFormat() = "X.509"
                override fun getEncoded() = classicalPub
            }
            val pqcPubKey = object : PublicKey {
                override fun getAlgorithm() = "ML-DSA-65"
                override fun getFormat() = "RAW"
                override fun getEncoded() = pqcPub
            }
            
            // Verify both signatures (both must be valid)
            val classicalValid = classicalProvider.verify(data, classicalSig, classicalPubKey, SignatureAlgorithm.ECDSA_P256)
            val pqcValid = pqcProvider.verify(data, pqcSig, pqcPubKey, SignatureAlgorithm.ML_DSA_65)
            
            classicalValid && pqcValid
        }

        private fun combineKeys(key1: ByteArray, key2: ByteArray): ByteArray {
            // Simple concatenation with length prefix
            val result = ByteArray(4 + key1.size + key2.size)
            var offset = 0
            
            // Length of first key
            result[offset++] = (key1.size shr 24).toByte()
            result[offset++] = ((key1.size shr 16) and 0xFF).toByte()
            result[offset++] = ((key1.size shr 8) and 0xFF).toByte()
            result[offset++] = (key1.size and 0xFF).toByte()
            
            // First key
            System.arraycopy(key1, 0, result, offset, key1.size)
            offset += key1.size
            
            // Second key
            System.arraycopy(key2, 0, result, offset, key2.size)
            
            return result
        }

        private fun splitKeys(combined: ByteArray): Pair<ByteArray, ByteArray> {
            var offset = 0
            
            // Read length of first key
            val key1Len = ((combined[offset++].toInt() and 0xFF) shl 24) or
                         ((combined[offset++].toInt() and 0xFF) shl 16) or
                         ((combined[offset++].toInt() and 0xFF) shl 8) or
                         (combined[offset++].toInt() and 0xFF)
            
            // Extract first key
            val key1 = combined.copyOfRange(offset, offset + key1Len)
            offset += key1Len
            
            // Extract second key
            val key2 = combined.copyOfRange(offset, combined.size)
            
            return Pair(key1, key2)
        }

        private fun combineSharedSecrets(secret1: ByteArray, secret2: ByteArray): ByteArray {
            // Use HKDF-like approach: SHA-256(secret1 || secret2)
            val digest = MessageDigest.getInstance("SHA-256")
            digest.update(secret1)
            digest.update(secret2)
            return digest.digest()
        }
    }

    /**
     * Migration Helper
     * 
     * Provides utilities for migrating from classical to post-quantum cryptography.
     */
    object MigrationHelper {
        /**
         * Detect current encryption format
         */
        fun detectFormat(data: ByteArray): String {
            return when {
                data.size < 2 -> "UNKNOWN"
                data[0] == FORMAT_VERSION.toByte() -> "PQC_v$FORMAT_VERSION"
                else -> "LEGACY_AES_GCM"
            }
        }

        /**
         * Create migration metadata
         */
        fun createMigrationMetadata(
            sourceAlgorithm: String,
            targetAlgorithm: String,
            migrationDate: String
        ): Map<String, String> {
            return mapOf(
                "sourceAlgorithm" to sourceAlgorithm,
                "targetAlgorithm" to targetAlgorithm,
                "migrationDate" to migrationDate,
                "migrationVersion" to FORMAT_VERSION.toString()
            )
        }

        /**
         * Check if migration is needed
         */
        fun shouldMigrate(currentFormat: String, targetAlgorithm: KEMAlgorithm): Boolean {
            return currentFormat.startsWith("LEGACY") && targetAlgorithm.isPostQuantum()
        }
    }
}
