# GitHub Copilot Instructions for ObsidianBackup

## CONTEXT

You are working on **ObsidianBackup**, a production-grade Android backup application with root capabilities, multi-platform support (Wear OS, Android TV, Web), cloud integrations, and enterprise features. This is a root-focused Android backup engine (spiritual successor to Titanium Backup) that is 72% production-ready with specific gaps that need addressing.

**Version:** Kotlin 2.0.21 | compileSdk/targetSdk 35 | minSdk 26 | JDK 17

---

## CRITICAL PRINCIPLES

### 1. Security First
- **NEVER store sensitive data unencrypted** - Always use Android Keystore or EncryptedFile
- **ALWAYS validate external inputs** - Checksums for binaries, signature verification for plugins
- **NEVER trust user input in shell commands** - Use the existing ShellSanitizer
- **ALWAYS encrypt PHI/PII** - Health Connect data MUST use AES256_GCM encryption
- **Validate JWT secrets** - Never use defaults in production (Enterprise backend)

### 2. Billing & Monetization Protection
- **Enforce feature gates at repository/use-case level** - Never just UI-level
- **Validate usage limits in orchestrators** - Prevent deep link bypass
- **All premium features must check FeatureGateService** before execution
- Product IDs: `obsidian_backup_pro_monthly`, `obsidian_backup_pro_yearly`, `obsidian_backup_team_monthly`, `obsidian_backup_team_yearly`

### 3. Code Quality Standards
- **Clean Architecture** - Maintain layer separation (UI → ViewModel → UseCase → Repository → DataSource)
- **Hilt dependency injection** - Use constructor injection, proper scoping (@Singleton, @ViewModelScoped)
- **Sealed classes for results** - Use `Result.Success`, `Result.Error`, `Result.Loading`
- **No GlobalScope** - Use proper coroutine scopes (viewModelScope, lifecycleScope)
- **Null-safety patterns** - Leverage Kotlin's null-safety fully
- **No TODOs in production code** - Complete implementations only

### 4. Multi-Platform Consistency
- **Wear OS** - Full Material You Tiles, not skeletons
- **Android TV** - Leanback UI with proper D-pad navigation
- **Web Companion** - Socket.io with proper WebSocket auth
- All platforms must respect the same security and billing rules

---

## CRITICAL BLOCKERS TO FIX

### 🔴 BLOCKER 1: Health Connect HIPAA Compliance
**File:** `app/src/main/java/com/obsidianbackup/health/HealthDataExporter.kt`  
**Lines:** 65-66  
**Issue:** Storing PHI unencrypted violates HIPAA

**Current Code:**
```kotlin
val outputFile = File(outputDir, "health_data_$timestamp.json")
outputFile.writeText(healthDataJson)
```

**Required Fix:**
```kotlin
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKeys

val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
val outputFile = File(outputDir, "health_data_$timestamp.json.encrypted")

val encryptedFile = EncryptedFile.Builder(
    outputFile,
    context,
    masterKeyAlias,
    EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
).build()

encryptedFile.openFileOutput().use { outputStream ->
    outputStream.write(healthDataJson.toByteArray())
}
```

**Audit Logging Required:**
```kotlin
// Add after encryption
securityAuditLogger.logDataAccess(
    dataType = "PHI",
    action = "EXPORT_ENCRYPTED",
    userId = currentUser.id,
    timestamp = System.currentTimeMillis(),
    complianceFlag = "HIPAA"
)
```

**Time Estimate:** 3 days (includes audit logging + testing)

---

### 🔴 BLOCKER 2: ML Model Missing
**File:** `app/src/main/assets/backup_predictor.tflite` (MISSING)  
**Related:** `app/src/main/java/com/obsidianbackup/ml/BackupPredictor.kt`

**Issue:** Smart scheduling feature claims ML-based predictions but model file doesn't exist

**Required Actions:**
1. **Train TensorFlow Lite Model:**
   - Input features: hour_of_day, day_of_week, battery_level, wifi_connected, charging_state, app_usage_pattern
   - Output: probability of successful backup completion
   - Use existing usage data from `BackupHistoryRepository`

2. **Model Architecture:**
```python
# Training script (Python/TF Lite)
import tensorflow as tf

model = tf.keras.Sequential([
    tf.keras.layers.Dense(32, activation='relu', input_shape=(6,)),
    tf.keras.layers.Dropout(0.2),
    tf.keras.layers.Dense(16, activation='relu'),
    tf.keras.layers.Dense(1, activation='sigmoid')
])

# Convert to TFLite
converter = tf.lite.TFLiteConverter.from_keras_model(model)
converter.optimizations = [tf.lite.Optimize.DEFAULT]
tflite_model = converter.convert()

# Save to assets
with open('backup_predictor.tflite', 'wb') as f:
    f.write(tflite_model)
```

3. **Verify Interpreter Initialization:**
```kotlin
// In BackupPredictor.kt
private val interpreter: Interpreter by lazy {
    val model = loadModelFile() // This should not throw FileNotFoundException
    Interpreter(model, Interpreter.Options().apply {
        setNumThreads(2)
        setUseNNAPI(true)
    })
}
```

**Time Estimate:** 1 week (data preparation + training + integration)

---

### 🔴 BLOCKER 3: Enterprise Backend Implementation
**Location:** `enterprise/` directory  
**Issue:** Scaffold only - no service implementations, SAML SSO missing, admin console incomplete

**Required Implementations:**

#### A. Device Management Service
**File:** `enterprise/src/main/kotlin/com/obsidianbackup/enterprise/services/DeviceService.kt`

```kotlin
@Service
class DeviceService(
    private val deviceRepository: DeviceRepository,
    private val policyEngine: PolicyEngine,
    private val fcmPushService: FcmPushService
) {
    suspend fun enrollDevice(deviceId: String, userId: String, orgId: String): Result<Device> {
        // Validate organization policy
        val policy = policyEngine.getOrgPolicy(orgId) ?: return Result.Error("Invalid org")
        
        // Create device record
        val device = Device(
            id = deviceId,
            userId = userId,
            orgId = orgId,
            enrolledAt = Instant.now(),
            lastSeen = Instant.now(),
            complianceStatus = ComplianceStatus.PENDING
        )
        
        deviceRepository.save(device)
        
        // Send initial policy
        fcmPushService.sendPolicyUpdate(deviceId, policy)
        
        return Result.Success(device)
    }
    
    suspend fun sendRemoteWipe(deviceId: String, adminId: String): Result<Unit> {
        val device = deviceRepository.findById(deviceId) ?: return Result.Error("Device not found")
        
        // Audit log
        auditLogger.log(
            action = "REMOTE_WIPE_INITIATED",
            adminId = adminId,
            deviceId = deviceId,
            timestamp = Instant.now()
        )
        
        // Send FCM command
        fcmPushService.sendCommand(deviceId, RemoteCommand.WIPE)
        
        return Result.Success(Unit)
    }
}
```

#### B. SAML SSO Integration
**File:** `enterprise/src/main/kotlin/com/obsidianbackup/enterprise/auth/SamlAuthProvider.kt`

```kotlin
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal

@Component
class SamlAuthProvider(
    private val userRepository: UserRepository,
    private val orgRepository: OrgRepository
) {
    fun handleSamlAssertion(assertion: Saml2AuthenticatedPrincipal): AuthResult {
        val email = assertion.getAttribute("email")?.first() 
            ?: return AuthResult.Error("Email not in SAML assertion")
        
        val orgDomain = email.substringAfter("@")
        val org = orgRepository.findByDomain(orgDomain) 
            ?: return AuthResult.Error("Organization not enrolled")
        
        // Create or update user
        val user = userRepository.findByEmail(email) ?: User(
            email = email,
            orgId = org.id,
            role = assertion.getAttribute("role")?.first() ?: "USER",
            createdAt = Instant.now()
        )
        
        userRepository.save(user)
        
        return AuthResult.Success(generateJWT(user))
    }
}
```

**Configuration Required (docker-compose.yml):**
```yaml
environment:
  SAML_IDP_METADATA_URL: ${SAML_IDP_METADATA_URL}
  SAML_SP_ENTITY_ID: "https://enterprise.obsidianbackup.com"
  JWT_SECRET: ${JWT_SECRET}  # MUST override default
  JWT_EXPIRATION: 3600
```

#### C. Policy Engine
**File:** `enterprise/src/main/kotlin/com/obsidianbackup/enterprise/policy/PolicyEngine.kt`

```kotlin
@Service
class PolicyEngine(
    private val policyRepository: PolicyRepository
) {
    fun evaluateCompliance(device: Device, policy: BackupPolicy): ComplianceResult {
        val violations = mutableListOf<PolicyViolation>()
        
        // Check backup frequency
        val lastBackup = device.lastBackupTime
        val requiredInterval = policy.minimumBackupInterval
        if (lastBackup == null || 
            Duration.between(lastBackup, Instant.now()) > requiredInterval) {
            violations.add(PolicyViolation.BACKUP_OVERDUE)
        }
        
        // Check encryption
        if (policy.requireEncryption && !device.encryptionEnabled) {
            violations.add(PolicyViolation.ENCRYPTION_DISABLED)
        }
        
        // Check allowed cloud providers
        if (device.cloudProvider !in policy.allowedProviders) {
            violations.add(PolicyViolation.UNAUTHORIZED_CLOUD_PROVIDER)
        }
        
        return ComplianceResult(
            isCompliant = violations.isEmpty(),
            violations = violations,
            evaluatedAt = Instant.now()
        )
    }
}
```

**Time Estimate:** 6-8 weeks for full Enterprise backend

---

### 🔒 HIGH PRIORITY: Rclone Binary Validation
**File:** `app/src/main/java/com/obsidianbackup/cloud/rclone/RcloneBinaryManager.kt`  
**Line:** 247

**Issue:** Checksums detected but not enforced before execution

**Current Code:**
```kotlin
private val EXPECTED_CHECKSUMS = mapOf(
    "arm64-v8a" to "sha256:abc123...",
    "armeabi-v7a" to "sha256:def456...",
    "x86_64" to "sha256:ghi789..."
)
// But validation is never called!
```

**Required Fix:**
```kotlin
private fun validateBinaryChecksum(abi: String, binaryFile: File): Boolean {
    val expectedChecksum = EXPECTED_CHECKSUMS[abi] 
        ?: throw SecurityException("No checksum for ABI: $abi")
    
    val actualChecksum = MessageDigest.getInstance("SHA-256").run {
        update(binaryFile.readBytes())
        digest().joinToString("") { "%02x".format(it) }
    }
    
    return "sha256:$actualChecksum" == expectedChecksum
}

fun extractBinary(context: Context): File {
    val abi = Build.SUPPORTED_ABIS.first()
    val binaryFile = File(context.filesDir, "rclone_$abi")
    
    if (binaryFile.exists()) {
        // CRITICAL: Validate existing binary
        if (!validateBinaryChecksum(abi, binaryFile)) {
            binaryFile.delete()
            throw SecurityException("Rclone binary checksum mismatch - possible tampering")
        }
        return binaryFile
    }
    
    // Extract from assets
    context.assets.open("rclone/rclone_$abi").use { input ->
        binaryFile.outputStream().use { output ->
            input.copyTo(output)
        }
    }
    
    // Validate after extraction
    if (!validateBinaryChecksum(abi, binaryFile)) {
        binaryFile.delete()
        throw SecurityException("Extracted rclone binary failed checksum")
    }
    
    binaryFile.setExecutable(true)
    return binaryFile
}
```

**Time Estimate:** 1 day

---

## MEDIUM PRIORITY FIXES

### ⚠️ Database Migration Tests
**File:** `app/src/androidTest/java/com/obsidianbackup/data/migrations/MigrationTest.kt` (MISSING)

**Required Test Coverage:**
```kotlin
@RunWith(AndroidJUnit4::class)
class MigrationTest {
    private lateinit var testHelper: MigrationTestHelper
    
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java
    )
    
    @Test
    fun migrate29To30_preservesData() = runTest {
        // Create v29 database
        val dbV29 = helper.createDatabase(TEST_DB, 29).apply {
            execSQL("INSERT INTO backup_profiles VALUES (1, 'Test Profile', 0)")
            close()
        }
        
        // Migrate to v30
        val dbV30 = helper.runMigrationsAndValidate(TEST_DB, 30, true)
        
        // Verify data integrity
        dbV30.query("SELECT * FROM backup_profiles WHERE id = 1").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Test Profile", cursor.getString(cursor.getColumnIndex("name")))
        }
    }
    
    @Test
    fun migrate30To31_addsEncryptionColumn() = runTest {
        helper.createDatabase(TEST_DB, 30).close()
        val db = helper.runMigrationsAndValidate(TEST_DB, 31, true)
        
        // Verify schema
        db.query("PRAGMA table_info(backup_profiles)").use { cursor ->
            val columns = mutableListOf<String>()
            while (cursor.moveToNext()) {
                columns.add(cursor.getString(cursor.getColumnIndex("name")))
            }
            assertTrue("encryption_enabled" in columns)
        }
    }
}
```

**Time Estimate:** 3 days

---

### ⚠️ Web Companion WebSocket Authentication
**File:** `web-companion/src/main/kotlin/com/obsidianbackup/web/WebSocketHandler.kt`  
**Line:** Missing auth check in `onConnect()`

**Current Gap:**
```kotlin
override fun onConnect(session: SocketIOSession) {
    // No authentication check!
    connectedSessions[session.id] = session
}
```

**Required Fix:**
```kotlin
override fun onConnect(session: SocketIOSession) {
    val token = session.handshakeData.getHeader("Authorization")
        ?.removePrefix("Bearer ")
        ?: run {
            session.disconnect()
            return
        }
    
    val user = jwtAuthService.validateToken(token) ?: run {
        session.disconnect()
        return
    }
    
    // Rate limiting
    if (!rateLimiter.allowConnection(user.id)) {
        session.disconnect()
        return
    }
    
    connectedSessions[session.id] = AuthenticatedSession(session, user)
    
    logger.info("WebSocket connected: userId=${user.id}, sessionId=${session.id}")
}
```

**Time Estimate:** 30 minutes

---

### ⚠️ Wear OS Tile Intent Fix
**File:** `wear/src/main/java/com/obsidianbackup/wear/tiles/BackupTileService.kt`  
**Line:** 89

**Issue:** Hardcoded intent doesn't launch app correctly

**Current Code:**
```kotlin
val launchIntent = Intent(this, MainActivity::class.java)
```

**Required Fix:**
```kotlin
val launchIntent = Intent(this, MainActivity::class.java).apply {
    action = Intent.ACTION_MAIN
    addCategory(Intent.CATEGORY_LAUNCHER)
    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
    putExtra("source", "wear_tile")  // Analytics tracking
}

val pendingIntent = PendingIntent.getActivity(
    this,
    0,
    launchIntent,
    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
)
```

**Time Estimate:** 10 minutes

---

### ⚠️ TaskerSecurityValidator Debug Bypass
**File:** `app/src/main/java/com/obsidianbackup/tasker/TaskerSecurityValidator.kt`  
**Line:** 125

**Issue:** Debug mode bypasses validation completely

**Current Code:**
```kotlin
fun validateCaller(callingPackage: String): Boolean {
    if (BuildConfig.DEBUG) return true  // SECURITY RISK
    return callingPackage == TASKER_PACKAGE
}
```

**Required Fix:**
```kotlin
fun validateCaller(callingPackage: String): Boolean {
    // Debug mode should still validate, just log warnings
    val isValid = callingPackage in ALLOWED_AUTOMATION_PACKAGES
    
    if (BuildConfig.DEBUG && !isValid) {
        Log.w(TAG, "Debug mode: Allowing unauthorized caller $callingPackage")
        securityAuditLogger.logUnauthorizedAccess(
            caller = callingPackage,
            action = "TASKER_API_ACCESS",
            debugMode = true
        )
    }
    
    return isValid || BuildConfig.DEBUG
}

companion object {
    private val ALLOWED_AUTOMATION_PACKAGES = setOf(
        "net.dinglisch.android.taskerm",
        "com.llamalab.automate",
        "com.twofortyfouram.locale"
    )
}
```

**Time Estimate:** 5 minutes

---

### ⚠️ Widget Button Click Handler
**File:** `app/src/main/java/com/obsidianbackup/widgets/QuickBackupWidget.kt`  
**Line:** 134

**Issue:** PendingIntent flags missing IMMUTABLE flag (Android 12+ requirement)

**Current Code:**
```kotlin
val pendingIntent = PendingIntent.getBroadcast(
    context, 
    0, 
    intent, 
    PendingIntent.FLAG_UPDATE_CURRENT
)
```

**Required Fix:**
```kotlin
val pendingIntent = PendingIntent.getBroadcast(
    context,
    0,
    intent,
    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
)
```

**Time Estimate:** 5 minutes

---

## CLOUD PROVIDER OAUTH SETUP

### Google Drive OAuth Configuration
**File:** `app/src/main/res/values/oauth_credentials.xml`

**Required Steps:**
1. Go to https://console.cloud.google.com
2. Create OAuth 2.0 Client ID (Android)
3. Add SHA-1 fingerprint: `keytool -list -v -keystore ~/.android/debug.keystore`
4. Enable Google Drive API
5. Add credentials:

```xml
<resources>
    <string name="google_drive_client_id">YOUR_CLIENT_ID.apps.googleusercontent.com</string>
    <string name="google_drive_scopes">https://www.googleapis.com/auth/drive.file</string>
</resources>
```

**Verify in GoogleDriveProvider.kt:**
```kotlin
private val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
    .requestScopes(Scope(DriveScopes.DRIVE_FILE))
    .requestEmail()
    .requestIdToken(context.getString(R.string.google_drive_client_id))
    .build()
```

---

### Dropbox OAuth via Rclone
**File:** `app/src/main/assets/rclone/rclone.conf.template`

**Required:**
1. Create app at https://www.dropbox.com/developers/apps
2. Add OAuth redirect URL: `obsidianbackup://oauth2redirect`
3. Configure in app:

```kotlin
// In RcloneDropboxProvider.kt
override suspend fun authenticate(): Result<String> {
    val config = """
    [dropbox]
    type = dropbox
    client_id = ${BuildConfig.DROPBOX_CLIENT_ID}
    client_secret = ${BuildConfig.DROPBOX_CLIENT_SECRET}
    """.trimIndent()
    
    // Save encrypted config
    val configFile = keystoreManager.createEncryptedFile("rclone_dropbox.conf")
    configFile.writeText(config)
    
    // Launch OAuth flow
    return rcloneExecutor.executeAuth("dropbox", configFile)
}
```

---

## Build Commands

```bash
# Assemble specific variant (preferred for development)
./gradlew assembleFreeDebug
./gradlew assemblePremiumRelease

# Run a single unit test
./gradlew test --tests "com.obsidianbackup.SpecificTest"

# Run unit tests for a module/variant
./gradlew :app:testFreeDebugUnitTest

# Instrumentation tests (requires device/emulator)
./gradlew connectedAndroidTest
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.obsidianbackup.SpecificTest

# Static analysis
./gradlew detekt
./gradlew lintRelease

# Coverage
./gradlew jacocoTestReport

# Full test pipeline (script)
./scripts/run_tests.sh

# Build optimizations
./gradlew buildInfo                    # Show build configuration
./gradlew cleanAll                     # Clean all build outputs including cache
```

## Architecture

### Overview

Root-focused Android backup engine (spiritual successor to Titanium Backup). Kotlin 2.0.21, compileSdk/targetSdk 35, minSdk 26, JDK 17.

### Modules

- `:app` — Main Android app
- `:root-core` — Root detection library (Magisk/KernelSU/APatch), ShellExecutor, SELinux, BusyBox
- `:tv` — Android TV variant
- `:wear` — Wear OS variant

### Layers (Clean Architecture)

```
Presentation (Compose UI + ViewModels)
        ↓
Domain (UseCases + BackupOrchestrator)
        ↓
Data (CatalogRepository + CloudProviders)
        ↓
Storage (Room DB + BackupCatalog + File I/O)
```

### Backup Pipeline

```
AppsViewModel.backupApps()
  → BackupAppsUseCase
    → BackupOrchestrator.executeBackup()  # retry logic, incremental strategy
      → ObsidianBoxEngine.backupApps()    # root shell: cp APK, tar data
      → verifyAndCatalog()                # Merkle verify + Room DB + JSON save
```

### Key Interfaces

**BackupEngine** (`engine/BackupEngine.kt`): All methods are `suspend`. Uses `BackupId` (not `SnapshotId`). Returns sealed result types. Progress via `Flow<OperationProgress>`.

**CloudProvider** (`cloud/CloudProvider.kt`): Has `providerId`/`displayName` properties. Methods for upload/download/list/delete snapshots plus `getStorageQuota()` and `syncCatalog()`. Progress via `Flow<CloudTransferProgress>`.

**PluginRegistry** (`plugins/core/PluginRegistry.kt`): Thread-safe (Mutex). Plugin types: `BACKUP_ENGINE`, `CLOUD_PROVIDER`, `AUTOMATION`, `EXPORT`.

### Build Variants

| Flavor | Profiles | Flag |
|--------|----------|------|
| `free` | 3 max | `IS_PREMIUM=false` |
| `premium` | 999 max | `IS_PREMIUM=true` |

Build types: `debug`, `release` (R8 + ProGuard), `benchmark`.

## Key Conventions

### Dependency Injection (Hilt)

DI modules in `di/` package, named `[Feature]Module.kt`. Key modules: `AppModule` (core singletons), `CloudModule`, `SecurityModule`. All `@Singleton` scoped in `SingletonComponent`. ViewModels use `@HiltViewModel`.

### MVI Pattern

ViewModels expose `StateFlow<State>`, accept sealed `Intent` classes via `handleIntent()`. Compose screens split into `FeatureScreen` (stateful, gets ViewModel) and `FeatureContent` (stateless, receives state + callbacks).

### UseCases

One per business operation, named `[Verb][Noun]UseCase`. Implement `suspend operator fun invoke()`. Injected via Hilt constructor.

### Storage

- Room DB encrypted with SQLCipher (key from `security/crypto/DatabaseKeyProvider`)
- DAOs in `storage/`, named `[Entity]Dao`
- Entities in `storage/`, named `[Entity]Entity`
- `BackupCatalog` does dual persistence: Room DB + JSON files for portability
- Indexed columns for performance: `timestamp`, `baseSnapshotId`, `isIncremental`
- 8 schema versions — always add migrations in `storage/migrations/DatabaseMigrations.kt`

### Cloud Providers

To add a provider: create class in `cloud/providers/` implementing `CloudProvider`, add to `CloudModule` DI binding, add enum value to `CloudProviderType`, update `CloudProviderFactory`.

### Testing

- Unit tests: JUnit 5 with `@Nested`, `@DisplayName`, `@ParameterizedTest`. Extend `BaseTest` (enables parallel execution).
- Instrumentation tests: JUnit 4 with `@HiltAndroidTest` + `AndroidJUnit4`.
- Mocking: MockK (`mockk<T>()`, `coVerify` for suspend functions).
- Test naming: `methodName_scenario_expectedResult`
- Factories: `TestDataFactory`, `TestFixtures`, `TestConstants`

### Logging

Use Timber: `Timber.d()`, `Timber.e(throwable, "message")`. Log tag format: `[Module]` (e.g., `[BackupEngine]`).

### Feature Flags

`FeatureFlagManager` with `Feature` enum. Check via `featureFlagManager.isEnabled(Feature.GAMING_BACKUP)`. Firebase Remote Config backed.

### UI Theme (Neon Shield)

Dark cybersecurity aesthetic. Primary: Neon Cyan `#00E5FF`, Secondary: Neon Purple `#BB86FC`, Background: Deep Obsidian `#08080C`.

## Type System Pitfalls

**BackupId vs SnapshotId**: Both are value classes wrapping String. `BackupId` is used by engines/orchestrators, `SnapshotId` in results/UI. Convert explicitly: `BackupId(snapshotId.value)`.

**BackupMetadata**: Lives in `com.obsidianbackup.storage` (not `model`) because it's tied to catalog implementation.

**BackupRequest duality**: `model.BackupRequest` (engine-level) vs `domain.backup.BackupRequest` (orchestrator-level). Must convert between layers.

**BackupCatalog**: Takes domain types (`BackupMetadata`, `BackupId`). Does NOT expose DAO entities directly.

**Stub implementations**: Many exist. Always return proper sealed failure types (e.g., `BackupResult.Failure(reason = "Not implemented", appsFailed = emptyList())`), not exceptions.

**Type aliases**: Some model types are re-exported via `core.models.BridgeModels.kt` for backward compatibility (e.g., `AppId`, `SnapshotId`, `BackupId`).

**Migration paths**: Legacy `model.BackupEngine` is deprecated; use `engine.BackupEngine` instead.

## Common Build Issues

- **KSP errors**: `rm -rf app/build/generated/ksp/ && ./gradlew clean assembleDebug`
- **Hilt injection failures**: Verify module is in correct component, `@Provides` signature matches injection site
- **Room migrations**: Add migrations in `storage/migrations/DatabaseMigrations.kt`; `.fallbackToDestructiveMigration()` only in debug
- **Public inline functions**: Cannot call internal/private functions — make the inline function `internal` or the called function `public`
- **Keystore not found**: Release signing requires keystore at `/mnt/workspace/obsidianbox/keystore/obsidianbox.jks` with properties file

## Project Context

**Purpose**: Root-focused Android backup engine, spiritual successor to Titanium Backup. Production code ported from ObsidianBox (v31).

**Modules**:
- `:app` (main), `:root-core` (root detection/shell), `:tv`, `:wear`
- Enterprise backend in `enterprise/`, ML training in `ml_training/`, web companion in `web-companion/`

**Documentation**: 60+ docs in `docs/` (start with `00_START_HERE_TESTING.md`)

---

## FEATURE IMPLEMENTATION GUIDELINES

### When Implementing New Cloud Providers
1. **Extend CloudProvider interface** - Implement all methods (upload, download, delete, list)
2. **Add to CloudModule** - Register with `@Named("ProviderName")` qualifier
3. **Implement OAuth if needed** - Store tokens via KeystoreManager
4. **Add to FeatureGateService** - Gate behind appropriate subscription tier
5. **Write integration tests** - Upload/download roundtrip validation
6. **Update CloudProviderRegistry** - Add to available providers list

### When Adding Enterprise Features
1. **Check subscription tier** - Use FeatureGateService.checkAccess(FeatureId.ENTERPRISE_*)
2. **Add audit logging** - All admin actions must log to audit trail
3. **Implement policy enforcement** - Use PolicyEngine for compliance checks
4. **Add WebSocket command** - Enterprise features should support remote triggering
5. **Update admin console** - Add UI in React admin panel

### When Modifying Security-Critical Code
1. **Run security audit** - `./gradlew detekt`
2. **Add security test** - Verify attack vectors are blocked
3. **Update SecurityAuditLogger** - Log sensitive operations
4. **Review with ShellSanitizer** - If touching shell execution
5. **Check encryption** - Sensitive data must use Android Keystore

---

## CODE PATTERNS TO FOLLOW

### Repository Pattern
```kotlin
class BackupRepository @Inject constructor(
    private val localDataSource: BackupLocalDataSource,
    private val cloudDataSource: CloudDataSource,
    private val featureGate: FeatureGateService
) {
    suspend fun createBackup(profile: BackupProfile): Result<Backup> {
        // Check feature access FIRST
        if (!featureGate.hasAccess(FeatureId.CLOUD_BACKUP)) {
            return Result.Error("Upgrade to Pro for cloud backups")
        }
        
        // Business logic
        val backup = localDataSource.createBackup(profile)
        cloudDataSource.upload(backup)
        
        return Result.Success(backup)
    }
}
```

### UseCase Pattern
```kotlin
class CreateBackupUseCase @Inject constructor(
    private val backupRepository: BackupRepository,
    private val analyticsRepository: AnalyticsRepository
) {
    suspend operator fun invoke(profileId: String): Result<Backup> {
        analyticsRepository.logEvent("backup_started", mapOf("profile" to profileId))
        
        return when (val result = backupRepository.createBackup(profileId)) {
            is Result.Success -> {
                analyticsRepository.logEvent("backup_completed")
                result
            }
            is Result.Error -> {
                analyticsRepository.logEvent("backup_failed", mapOf("error" to result.message))
                result
            }
        }
    }
}
```

### ViewModel Pattern
```kotlin
@HiltViewModel
class BackupViewModel @Inject constructor(
    private val createBackupUseCase: CreateBackupUseCase,
    private val featureGateService: FeatureGateService
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<BackupUiState>(BackupUiState.Idle)
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()
    
    fun startBackup(profileId: String) {
        viewModelScope.launch {
            _uiState.value = BackupUiState.Loading
            
            when (val result = createBackupUseCase(profileId)) {
                is Result.Success -> {
                    _uiState.value = BackupUiState.Success(result.data)
                }
                is Result.Error -> {
                    _uiState.value = BackupUiState.Error(result.message)
                }
            }
        }
    }
}
```

---

## FORBIDDEN PATTERNS

### ❌ NEVER DO THIS
```kotlin
// DON'T: Check subscription in UI only
if (userTier == "PRO") {
    backupRepository.createCloudBackup()  // Can be bypassed!
}

// DON'T: Use GlobalScope
GlobalScope.launch {
    // Leaks, never cancelled
}

// DON'T: Store secrets in code
const val API_KEY = "sk_live_abc123"

// DON'T: Execute unsanitized shell commands
Runtime.getRuntime().exec("rm -rf $userInput")

// DON'T: Write sensitive data unencrypted
File("health.json").writeText(piiData)
```

### ✅ ALWAYS DO THIS
```kotlin
// DO: Check subscription in repository
suspend fun createCloudBackup(): Result<Backup> {
    if (!featureGate.hasAccess(FeatureId.CLOUD_BACKUP)) {
        return Result.Error("Premium feature")
    }
    // Proceed
}

// DO: Use proper coroutine scopes
viewModelScope.launch {
    // Cancelled automatically
}

// DO: Use BuildConfig or KeystoreManager
val apiKey = BuildConfig.API_KEY
val encryptedToken = keystoreManager.decrypt("oauth_token")

// DO: Sanitize all shell inputs
val sanitized = shellSanitizer.sanitize(userInput)
shellExecutor.execute("tar -czf backup.tar.gz $sanitized")

// DO: Encrypt sensitive data
val encrypted = encryptedFile.openFileOutput().use { it.write(piiData.toByteArray()) }
```

---

## TESTING REQUIREMENTS

### Before Committing Code
```bash
# Run all checks
./gradlew test                    # Unit tests
./gradlew connectedAndroidTest   # Instrumentation tests
./gradlew detekt                 # Static analysis
./gradlew lint                   # Android Lint
```

### Test Coverage Requirements
- **Critical paths:** 90%+ coverage (billing, backup engine, security)
- **Feature implementations:** 80%+ coverage
- **UI layers:** 60%+ coverage

### Security Testing Checklist
- [ ] SQL injection attempts blocked
- [ ] Shell injection attempts sanitized
- [ ] Path traversal attempts rejected
- [ ] Invalid APK signatures rejected
- [ ] Unauthenticated API calls denied
- [ ] Free tier users blocked from Pro features
- [ ] Encrypted data is actually encrypted (verify bytes)

---

## DEPENDENCIES & VERSIONS

### Critical Libraries (Already in build.gradle)
```kotlin
// Billing
implementation("com.android.billingclient:billing-ktx:6.0.1")

// Encryption
implementation("androidx.security:security-crypto:1.1.0-alpha06")
implementation("net.zetetic:android-database-sqlcipher:4.5.4")

// Cloud
implementation("com.google.android.gms:play-services-auth:20.7.0")
implementation("com.google.api-client:google-api-client-android:2.2.0")

// ML
implementation("org.tensorflow:tensorflow-lite:2.14.0")
implementation("org.tensorflow:tensorflow-lite-gpu:2.14.0")

// DI
implementation("com.google.dagger:hilt-android:2.48")
kapt("com.google.dagger:hilt-compiler:2.48")

// Testing
testImplementation("junit:junit:4.13.2")
androidTestImplementation("androidx.test.ext:junit:1.1.5")
androidTestImplementation("androidx.test:runner:1.5.2")
```

---

## FINAL CHECKLIST BEFORE LAUNCH

### Free Tier Launch Requirements
- [ ] Health Connect encryption implemented
- [ ] ML model trained and integrated
- [ ] Rclone checksum validation enforced
- [ ] Database migration tests passing
- [ ] Web companion WebSocket auth added
- [ ] All widgets working on Android 12+
- [ ] Tasker security hardened
- [ ] Google Drive OAuth configured
- [ ] Play Console products created
- [ ] Firebase Analytics configured

### Pro Tier Additional Requirements
- [ ] All cloud providers tested (upload/download)
- [ ] Feature gates enforced in repositories
- [ ] Usage limits tested (profile count, batch size)
- [ ] Wear OS/TV apps fully functional
- [ ] Plugin system tested with 3rd party plugins

### Enterprise Tier Additional Requirements
- [ ] Device management service implemented
- [ ] SAML SSO working with test IdP
- [ ] Policy engine compliance checks functional
- [ ] Remote wipe tested on test devices
- [ ] Admin console deployed and accessible
- [ ] FCM push commands working
- [ ] JWT secret rotated from default
- [ ] PostgreSQL configured with backups
- [ ] Audit logging to separate database
- [ ] HTTPS enforced with valid certificate

---

## WHEN TO ASK FOR HELP

You should flag these scenarios for human review:
1. **OAuth credentials needed** - Cannot implement without developer accounts
2. **Play Console setup** - Requires Google Play Developer account
3. **Enterprise customer configuration** - SAML metadata from customer IdP
4. **ML model training** - Requires historical backup data
5. **Production secrets** - JWT keys, database passwords, API tokens
6. **Legal compliance questions** - HIPAA, GDPR interpretations
7. **Architecture changes** - Major refactoring that affects multiple domains

---

## SUCCESS METRICS

### Code Quality Metrics
- Zero Detekt violations
- Zero Android Lint errors
- 80%+ test coverage
- All migrations tested
- Zero TODOs in production code

### Security Metrics
- All sensitive data encrypted at rest
- All external binaries checksum-validated
- All API calls authenticated
- All admin actions audit-logged
- Zero hardcoded secrets

### Business Metrics
- Billing flow tested end-to-end
- Feature gates enforced in ALL code paths
- Usage limits cannot be bypassed
- Free trial expiration works correctly
- Subscription upgrades/downgrades tested

---

## QUICK REFERENCE: FILE LOCATIONS

```
app/src/main/java/com/obsidianbackup/
├── billing/              # Subscription & IAP (PRODUCTION READY)
├── backup/              # Core backup engine (PRODUCTION READY)
├── cloud/               # Cloud providers (NEEDS: OAuth setup)
├── health/              # Health Connect (CRITICAL: Add encryption)
├── ml/                  # ML predictions (CRITICAL: Add model file)
├── security/            # Encryption & shell (PRODUCTION READY)
├── enterprise/          # Enterprise features (CRITICAL: Implement services)
├── wear/                # Wear OS app (MINOR: Fix intent)
├── tv/                  # Android TV app (PRODUCTION READY)
├── widgets/             # Home screen widgets (MINOR: Fix PendingIntent)
└── tasker/              # Tasker integration (MINOR: Harden security)

enterprise/src/main/kotlin/com/obsidianbackup/enterprise/
├── services/            # Device/Policy services (CRITICAL: Implement)
├── auth/                # SAML SSO (CRITICAL: Implement)
└── admin/               # Admin console backend (NEEDS: Complete)

web-companion/src/main/kotlin/
└── WebSocketHandler.kt  # (MINOR: Add auth check)
```

---

**END OF INSTRUCTIONS**

When implementing any fix or feature, reference this document to ensure consistency with the existing codebase quality and security standards.

**Remember:** You are building production software that handles root access, stores sensitive user data (PHI, app backups), charges users money via subscriptions, claims HIPAA compliance for health data, and promises enterprise-grade security. Every line of code must be secure by default, tested thoroughly, properly encrypted, subscription-gated appropriately, and auditable for compliance.

**The foundation is exceptional. Maintain that standard in every addition.**
