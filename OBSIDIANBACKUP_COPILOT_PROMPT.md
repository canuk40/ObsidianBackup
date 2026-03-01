# ObsidianBackup — GitHub Copilot (claude-sonnet-4.6) Implementation Prompt

> Paste this entire prompt into the GitHub Copilot Chat panel while the ObsidianBackup project is open.
> Every issue is sourced from the project's own audit report. Every technical prescription is sourced from official specifications, canonical libraries, and verified implementations.

---

You are working inside the **ObsidianBackup** Android project. This project has three Gradle modules: `app`, `wear`, and `tv`. A full audit has been completed and has identified every stub, placeholder, dead-code block, disconnected class, and "Coming Soon" gate across all three modules. Your task is to implement production-quality fixes for every issue listed below, in priority order. Read every instruction carefully. Do not skip items. Do not introduce new mocks, stubs, or placeholder returns anywhere.

---

## ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
## TIER 1 — FIX IMMEDIATELY (Crashes, Silent Data Loss, Auth Failures)
## ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

---

### [S1-SIM-01] Replace MockPQCProvider with real Bouncy Castle PQC (ML-KEM + ML-DSA)

**File:** `app/src/main/java/com/obsidianbackup/crypto/PostQuantumCrypto.kt` lines 480–586

**Current state:**
- `MockPQCProvider` generates random bytes for all five PQC operations.
- `verify()` returns `true` for any byte array of the correct length — it performs no cryptographic check.
- Comment reads `// Mock implementation: Always return true for valid format`.
- Any code relying on PQC signature verification makes zero security decisions.

**Required implementation:**
Add Bouncy Castle 1.79+ as a dependency. NIST finalized FIPS 203 (ML-KEM), FIPS 204 (ML-DSA), and FIPS 205 (SLH-DSA) on August 14, 2024. Bouncy Castle Java 1.79 (December 2024) and LTS 2.73.x both include full production implementations via `org.bouncycastle.pqc.crypto.mlkem` and `org.bouncycastle.pqc.crypto.mldsa`.

```kotlin
// build.gradle.kts (app)
implementation("org.bouncycastle:bcprov-jdk18on:1.79")

// PostQuantumCrypto.kt — replace MockPQCProvider with:
import org.bouncycastle.pqc.crypto.mlkem.*
import org.bouncycastle.pqc.crypto.mldsa.*
import java.security.SecureRandom

class BouncyCastlePQCProvider : PQCProvider {
    private val random = SecureRandom()

    override fun generateKEMKeyPair(): KEMKeyPair {
        val gen = MLKEMKeyPairGenerator()
        gen.init(MLKEMKeyGenerationParameters(random, MLKEMParameters.ml_kem_768))
        val kp = gen.generateKeyPair()
        return KEMKeyPair(
            publicKey = (kp.public as MLKEMPublicKeyParameters).encoded,
            privateKey = (kp.private as MLKEMPrivateKeyParameters).encoded
        )
    }

    override fun encapsulate(publicKeyBytes: ByteArray): EncapsulationResult {
        val pub = MLKEMPublicKeyParameters(MLKEMParameters.ml_kem_768, publicKeyBytes)
        val gen = MLKEMGenerator(random)
        val secret = gen.generateEncapsulated(pub)
        return EncapsulationResult(
            sharedSecret = secret.secret,
            ciphertext = secret.encapsulation
        )
    }

    override fun decapsulate(privateKeyBytes: ByteArray, ciphertext: ByteArray): ByteArray {
        val priv = MLKEMPrivateKeyParameters(MLKEMParameters.ml_kem_768, privateKeyBytes)
        val extractor = MLKEMExtractor(priv)
        return extractor.extractSecret(ciphertext)
    }

    override fun generateSignatureKeyPair(): SignatureKeyPair {
        val gen = MLDSAKeyPairGenerator()
        gen.init(MLDSAKeyGenerationParameters(random, MLDSAParameters.ml_dsa_65))
        val kp = gen.generateKeyPair()
        return SignatureKeyPair(
            publicKey = (kp.public as MLDSAPublicKeyParameters).encoded,
            privateKey = (kp.private as MLDSAPrivateKeyParameters).encoded
        )
    }

    override fun sign(privateKeyBytes: ByteArray, message: ByteArray): ByteArray {
        val priv = MLDSAPrivateKeyParameters(MLDSAParameters.ml_dsa_65, privateKeyBytes)
        val signer = MLDSASigner()
        signer.init(true, ParametersWithRandom(priv, random))
        signer.update(message, 0, message.size)
        return signer.generateSignature()
    }

    override fun verify(publicKeyBytes: ByteArray, message: ByteArray, signature: ByteArray): Boolean {
        val pub = MLDSAPublicKeyParameters(MLDSAParameters.ml_dsa_65, publicKeyBytes)
        val verifier = MLDSASigner()
        verifier.init(false, pub)
        verifier.update(message, 0, message.size)
        return verifier.verifySignature(signature) // real cryptographic check
    }
}
```

**Sources:**
- NIST FIPS 203 — ML-KEM specification: https://csrc.nist.gov/pubs/fips/203/final
- NIST FIPS 204 — ML-DSA specification: https://csrc.nist.gov/pubs/fips/204/final
- Bouncy Castle Java 1.79 release (December 2024) with full ML-KEM/ML-DSA support: https://www.bouncycastle.org/resources/latest-nist-pqc-standards-and-more-bouncy-castle-java-1-79/
- Bouncy Castle PQC Almanac (API reference with working code examples): https://downloads.bouncycastle.org/java/docs/PQC-Almanac.pdf
- Bouncy Castle `MLKEMGenerator` API reference: https://downloads.bouncycastle.org/java/docs/bcprov-jdk14-javadoc/org/bouncycastle/pqc/crypto/mlkem/MLKEMGenerator.html

---

### [S1-ST-04/05] Implement FTP/SFTP upload and download in FtpCloudProvider

**File:** `app/src/main/java/com/obsidianbackup/cloud/providers/FtpCloudProvider.kt` lines 132 and 144

**Current state:**
- `uploadFile()` returns `CloudResult.Success(Unit)` without writing a single byte to the server.
- `downloadFile()` returns `CloudResult.Success(Unit)` without reading a single byte from the server.
- Any user who configures FTP backup silently loses all backup data.

**Required implementation:**
Use **Apache Commons Net** for FTP/FTPS and **JSch (mwiede fork)** for SFTP — the same libraries already referenced in the `// Would use...` comment. These are the standard Java implementations confirmed to work on Android, as used in Android FTP client projects like https://codeberg.org/qwerty287/ftpclient.

```kotlin
// build.gradle.kts (app)
implementation("commons-net:commons-net:3.10.0")           // Apache Commons Net FTP
implementation("com.github.mwiede:jsch:0.2.16")            // JSch SFTP (actively maintained fork)

// FtpCloudProvider.kt

override suspend fun uploadFile(
    localFile: File,
    remotePath: String,
    metadata: CloudFileMetadata
): CloudResult<Unit> = withContext(Dispatchers.IO) {
    if (config.useSftp) {
        uploadViaSftp(localFile, remotePath)
    } else {
        uploadViaFtp(localFile, remotePath)
    }
}

private fun uploadViaFtp(localFile: File, remotePath: String): CloudResult<Unit> {
    val client = FTPClient()
    return try {
        client.connect(config.host, config.port)
        client.login(config.username, config.password)
        client.setFileType(FTP.BINARY_FILE_TYPE)
        client.enterLocalPassiveMode()
        val remoteDir = remotePath.substringBeforeLast("/")
        client.makeDirectory(remoteDir) // no-op if exists
        client.changeWorkingDirectory(remoteDir)
        val success = FileInputStream(localFile).use { input ->
            client.storeFile(localFile.name, input)
        }
        if (success) CloudResult.Success(Unit)
        else CloudResult.Error(CloudError(UPLOAD_FAILED, client.replyString))
    } catch (e: Exception) {
        CloudResult.Error(CloudError(UPLOAD_FAILED, e.message ?: "FTP upload error"))
    } finally {
        if (client.isConnected) { client.logout(); client.disconnect() }
    }
}

private fun uploadViaSftp(localFile: File, remotePath: String): CloudResult<Unit> {
    val jsch = JSch()
    return try {
        val session = jsch.getSession(config.username, config.host, config.port)
        session.setPassword(config.password)
        session.setConfig("StrictHostKeyChecking", "no") // TODO: replace with known_hosts in production
        session.connect(30_000)
        val channel = session.openChannel("sftp") as ChannelSftp
        channel.connect()
        channel.put(localFile.absolutePath, remotePath)
        channel.disconnect()
        session.disconnect()
        CloudResult.Success(Unit)
    } catch (e: SftpException) {
        CloudResult.Error(CloudError(UPLOAD_FAILED, "SFTP error ${e.id}: ${e.message}"))
    }
}

override suspend fun downloadFile(
    remotePath: String,
    localFile: File
): CloudResult<Unit> = withContext(Dispatchers.IO) {
    if (config.useSftp) {
        downloadViaSftp(remotePath, localFile)
    } else {
        downloadViaFtp(remotePath, localFile)
    }
}
// (implement downloadViaFtp and downloadViaSftp symmetrically using client.retrieveFile() and channel.get())
```

**Sources:**
- Apache Commons Net overview and FTPClient API: https://commons.apache.org/net/
- JSch maintained fork (mwiede): https://github.com/mwiede/jsch — com.github.mwiede:jsch:0.2.16
- Java FTP integration guide with storeFile/retrieveFile examples: https://medium.com/bliblidotcom-techblog/java-ftp-integration-using-apache-commons-net-5efb3d300829
- SFTP with JSch and SSHJ comparison (Baeldung): https://www.baeldung.com/java-file-sftp
- Android FTP+SFTP client using Commons Net + SSHJ (reference project): https://codeberg.org/qwerty287/ftpclient

---

### [S1-ST-06] Implement SMB upload and download in SmbCloudProvider

**File:** `app/src/main/java/com/obsidianbackup/cloud/providers/SmbCloudProvider.kt` line 122

**Current state:** Both `uploadFile()` and `downloadFile()` return `Success(Unit)` without touching the SMB share.

**Required implementation:**
Use **jcifs-ng** (eu.agno3.jcifs:jcifs-ng:2.1.9) which supports SMB2 by default and experimental SMB3, is the standard Android-compatible SMB2/3 library, and is referenced in the existing comment.

```kotlin
// build.gradle.kts (app)
implementation("eu.agno3.jcifs:jcifs-ng:2.1.9")

// SmbCloudProvider.kt
import jcifs.CIFSContext
import jcifs.config.PropertyConfiguration
import jcifs.context.BaseContext
import jcifs.smb.NtlmPasswordAuthenticator
import jcifs.smb.SmbFile

override suspend fun uploadFile(
    localFile: File,
    remotePath: String,
    metadata: CloudFileMetadata
): CloudResult<Unit> = withContext(Dispatchers.IO) {
    try {
        val ctx = buildCifsContext()
        SmbFile("smb://${config.host}/${config.share}/$remotePath", ctx).use { smbFile ->
            smbFile.outputStream.use { out ->
                localFile.inputStream().use { it.copyTo(out) }
            }
        }
        CloudResult.Success(Unit)
    } catch (e: Exception) {
        CloudResult.Error(CloudError(UPLOAD_FAILED, e.message ?: "SMB upload error"))
    }
}

private fun buildCifsContext(): CIFSContext {
    val props = Properties().apply {
        setProperty("jcifs.smb.client.minVersion", "SMB202")
        setProperty("jcifs.smb.client.maxVersion", "SMB311")
        setProperty("jcifs.smb.client.enableSMB2Signing", "true")
    }
    val baseCtx = BaseContext(PropertyConfiguration(props))
    return baseCtx.withCredentials(
        NtlmPasswordAuthenticator(config.domain, config.username, config.password)
    )
}
```

**Sources:**
- jcifs-ng GitHub (AgNO3/jcifs-ng) — SMB2 default, SMB3 experimental, Maven: eu.agno3.jcifs:jcifs-ng:2.1.9: https://github.com/AgNO3/jcifs-ng
- Android SMB2 guide using jcifs-ng: https://nickcarter9.github.io/en/2019/04/09/2019/2019_04_09-jcifs_ng/
- smbj (hierynomus/smbj) as alternative if minSdk >= 26: https://github.com/hierynomus/smbj

---

### [S1-PH-01 / S1-ST-14] Implement real OCI RSA-SHA256 request signing in OracleCloudProvider

**File:** `app/src/main/java/com/obsidianbackup/cloud/providers/OracleCloudProvider.kt` line 737

**Current state:** `val signatureBase64 = "PLACEHOLDER_SIGNATURE"` — every OCI API call is unsigned and rejected by Oracle's servers.

**Required implementation:**
OCI REST API follows HTTP Signatures draft-cavage-http-signatures-08. Per Oracle's official documentation at https://docs.oracle.com/en-us/iaas/Content/API/Concepts/signingrequests.htm, the signing algorithm MUST be RSA-SHA256. The key ID format is `tenancyOcid/userOcid/keyFingerprint`.

```kotlin
// OracleCloudProvider.kt — replace buildAuthorizationHeader():

private fun buildAuthorizationHeader(
    method: String,
    url: URL,
    headers: Map<String, String>,
    body: ByteArray?
): String {
    val date = DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now(ZoneOffset.UTC))
    val requestTarget = "(request-target): ${method.lowercase()} ${url.path}${url.query?.let { "?$it" } ?: ""}"
    val hostHeader = "host: ${url.host}"
    val dateHeader = "date: $date"

    // For POST/PUT, compute x-content-sha256 and content-length
    val signingHeaders = if (method == "GET" || method == "DELETE") {
        listOf(requestTarget, hostHeader, dateHeader)
    } else {
        val bodyBytes = body ?: ByteArray(0)
        val contentSha256 = Base64.encodeToString(
            MessageDigest.getInstance("SHA-256").digest(bodyBytes), Base64.NO_WRAP
        )
        listOf(
            requestTarget, hostHeader, dateHeader,
            "x-content-sha256: $contentSha256",
            "content-type: application/json",
            "content-length: ${bodyBytes.size}"
        )
    }

    val signingString = signingHeaders.joinToString("\n")

    // Load PEM private key from Android Keystore or config
    val privateKey = loadOciPrivateKey() // loads PKCS8 PEM from secure storage
    val sig = Signature.getInstance("SHA256withRSA")
    sig.initSign(privateKey)
    sig.update(signingString.toByteArray(Charsets.UTF_8))
    val signatureBase64 = Base64.encodeToString(sig.sign(), Base64.NO_WRAP)

    val headerNames = when (method) {
        "GET", "DELETE" -> "(request-target) host date"
        else -> "(request-target) host date x-content-sha256 content-type content-length"
    }

    val keyId = "${config.tenancyOcid}/${config.userOcid}/${config.keyFingerprint}"

    return """Signature version="1",keyId="$keyId",algorithm="rsa-sha256",headers="$headerNames",signature="$signatureBase64""""
}

private fun loadOciPrivateKey(): PrivateKey {
    // PEM stored in EncryptedSharedPreferences or Android Keystore
    val pemContent = keystoreManager.getOciPrivateKeyPem()
        ?: throw IllegalStateException("OCI private key not configured")
    val stripped = pemContent
        .replace("-----BEGIN PRIVATE KEY-----", "")
        .replace("-----BEGIN RSA PRIVATE KEY-----", "")
        .replace("-----END PRIVATE KEY-----", "")
        .replace("-----END RSA PRIVATE KEY-----", "")
        .replace("\n", "")
    val keyBytes = Base64.decode(stripped, Base64.DEFAULT)
    return KeyFactory.getInstance("RSA").generatePrivate(PKCS8EncodedKeySpec(keyBytes))
}
```

**Sources:**
- Oracle OCI REST API request signing specification (draft-cavage-http-signatures-08): https://docs.oracle.com/en-us/iaas/Content/API/Concepts/signingrequests.htm
- OCI required key format — RSA PEM minimum 2048 bits: https://docs.public.oneportal.content.oci.oraclecloud.com/en-us/iaas/Content/API/Concepts/apisigningkey.htm
- OCI signing string construction (POST requires x-content-sha256 + content-type + content-length): https://github.com/rosalexander/oci-postman-prerequests/blob/master/README.md
- Java OCI signing implementation reference: https://docs.oracle.com/en-us/iaas/Content/API/Concepts/signingrequests.htm#six (Java sample code)

---

### [S2-DC-01 / S2-DC-02] Fix Wear tile "Backup Now" button — wrong class name and missing message handler

**File:** `wear/src/main/java/com/obsidianbackup/wear/tiles/BackupTileService.kt` lines 88+

**Current state:**
- Line 88: `.setClassName("com.obsidianbackup.MainActivity")` — this class does not exist in the wear package. Intent silently fails.
- No `onTileInteraction()` override exists. The click ID `"backup_trigger"` is defined but never handled. No `MessageClient.sendMessage()` is ever called. The backup trigger from the watch tile is completely non-functional.

**Required implementation:**
Per Android Wear OS Data Layer documentation (https://developer.android.com/training/wearables/data/events), tile interactions are received in `onTileInteraction()`, and messages are sent to the phone node using `Wearable.getMessageClient(context).sendMessage()`.

```kotlin
// BackupTileService.kt

override fun onTileInteraction(requestParams: TileInteractionRequest) {
    if (requestParams.id == "backup_trigger") {
        sendBackupTriggerToPhone()
    }
}

private fun sendBackupTriggerToPhone() {
    val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    scope.launch {
        try {
            val nodes = Wearable.getNodeClient(applicationContext)
                .connectedNodes.await()
            val phoneNode = nodes.firstOrNull { it.isNearby } ?: nodes.firstOrNull()
            phoneNode?.let { node ->
                Wearable.getMessageClient(applicationContext)
                    .sendMessage(node.id, DataLayerPaths.BACKUP_TRIGGER_PATH, ByteArray(0))
                    .await()
                Log.d(TAG, "Backup trigger sent to phone node ${node.displayName}")
            } ?: Log.w(TAG, "No connected phone node found")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send backup trigger: ${e.message}")
        }
    }
}

// Also fix the Activity launch intent if needed:
// Change: .setClassName("com.obsidianbackup.MainActivity")
// To:     .setClassName("com.obsidianbackup.wear", "com.obsidianbackup.wear.presentation.MainActivity")
```

**Sources:**
- Android Wear OS: Handle Data Layer events — `WearableListenerService`, `MessageClient.sendMessage()`, `DataClient.putDataItem()`: https://developer.android.com/training/wearables/data/events
- Android Wear OS: Choose a client type — MessageClient for one-way RPC triggers: https://developer.android.com/training/wearables/data/messages
- Android Wear OS: `Wearable.getNodeClient().connectedNodes` to discover paired phone: https://developer.android.com/training/wearables/data/overview

---

### [S3-ST-01] Implement TVBackupManager.startBackup()

**File:** `tv/src/main/java/com/obsidianbackup/tv/backup/TVBackupManager.kt` lines 191–193

**Current state:** Empty body. A TV user pressing "Backup Now" triggers nothing.

**Required implementation:**
Enqueue a `BackupWorker` via WorkManager (or send an IPC intent to the main app module's backup service if TV is deployed as a companion, pending TV-1 architecture decision). At minimum implement the WorkManager path:

```kotlin
override fun startBackup() {
    val backupRequest = OneTimeWorkRequestBuilder<TVBackupWorker>()
        .setInputData(
            workDataOf(
                TVBackupWorker.KEY_CATEGORIES to selectedCategories.toTypedArray(),
                TVBackupWorker.KEY_INCLUDE_APKS to includeApks
            )
        )
        .setConstraints(
            Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
        )
        .addTag(TVBackupWorker.TAG)
        .build()

    WorkManager.getInstance(context)
        .enqueueUniqueWork(
            TVBackupWorker.TAG,
            ExistingWorkPolicy.REPLACE,
            backupRequest
        )
    Log.i(TAG, "TV backup enqueued via WorkManager")
}
```

Then create `TVBackupWorker` implementing `CoroutineWorker` that calls `TVBackupManager.performBackup(appList)` with real file copy logic.

---

### [S3-ST-03 / S3-DC-02] Wire OnActionClickedListener in BackupDetailsFragment

**File:** `tv/src/main/java/com/obsidianbackup/tv/ui/BackupDetailsFragment.kt` lines 60–62

**Current state:** `ACTION_BACKUP`, `ACTION_RESTORE`, `ACTION_DELETE` buttons are added to `actionsAdapter` but no `setOnActionClickedListener` is registered on `DetailsOverviewRowPresenter`. Leanback requires this registration for action click callbacks to fire.

**Required implementation:**
Per the official Android TV Leanback documentation at https://developer.android.com/training/tv/playback/leanback/details and the Leanback codelab at https://github.com/pengying/androidtv-codelab:

```kotlin
// BackupDetailsFragment.kt — in buildDetails() or onActivityCreated():

val rowPresenter = FullWidthDetailsOverviewRowPresenter(DetailsDescriptionPresenter())
rowPresenter.setOnActionClickedListener { action ->
    when (action.id) {
        ACTION_BACKUP.toLong() -> {
            tvBackupManager.startBackup()
            Toast.makeText(requireContext(), "Backup started", Toast.LENGTH_SHORT).show()
        }
        ACTION_RESTORE.toLong() -> {
            val intent = Intent(requireContext(), RestoreActivity::class.java)
            intent.putExtra(RestoreActivity.EXTRA_BACKUP_ID, selectedBackupId)
            startActivity(intent)
        }
        ACTION_DELETE.toLong() -> {
            showDeleteConfirmationDialog(selectedBackupId)
        }
    }
}

val selector = ClassPresenterSelector().apply {
    addClassPresenter(DetailsOverviewRow::class.java, rowPresenter)
    addClassPresenter(ListRow::class.java, ListRowPresenter())
}
mRowsAdapter = ArrayObjectAdapter(selector)
```

**Sources:**
- Android TV: Build a details view with DetailsOverviewRowPresenter/FullWidthDetailsOverviewRowPresenter: https://developer.android.com/training/tv/playback/leanback/details
- Leanback codelab — `setOnActionClickedListener` usage: https://github.com/pengying/androidtv-codelab/blob/master/static/codelabs/1-androidtv-adding-leanback/4-create-video-detail-fragment.md
- Kotlin and Android TV Leanback guide (Reintech): https://reintech.io/blog/kotlin-android-tv-developing-apps-for-big-screen

---

## ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
## TIER 2 — FIX THIS SPRINT (High-Impact, Mostly Wiring)
## ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

---

### [S2-W-01 / S2-MI-02] Wire sendBackupProgress() into BackupWorker and BackupOrchestrator

**Files:**
- `app/src/main/java/com/obsidianbackup/wear/PhoneDataLayerRepository.kt` — `sendBackupProgress()` exists but is never called
- `app/src/main/java/com/obsidianbackup/workers/BackupWorker.kt` — never calls `sendBackupProgress()`
- `app/src/main/java/com/obsidianbackup/backup/BackupOrchestrator.kt` — never calls `sendBackupProgress()`

**Required implementation:**
Per the Android Wear OS Data Layer documentation (https://developer.android.com/training/wearables/data/data-items), use `DataClient.putDataItem()` with `setUrgent()` for real-time progress updates. The `sendBackupProgress()` method in `PhoneDataLayerRepository` already constructs the correct `PutDataRequest` — it simply needs to be called at appropriate intervals during backup execution.

```kotlin
// BackupWorker.kt — inject PhoneDataLayerRepository and call during backup:

@HiltWorker
class BackupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val backupOrchestrator: BackupOrchestrator,
    private val phoneDataLayerRepository: PhoneDataLayerRepository  // ADD THIS
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val totalApps = inputData.getStringArray(KEY_APP_IDS)?.size ?: 0
        var processedApps = 0

        backupOrchestrator.performBackup(
            onProgress = { appName, appsCompleted, totalFiles ->
                processedApps = appsCompleted
                // Push progress to watch via Data Layer
                phoneDataLayerRepository.sendBackupProgress(
                    currentApp = appName,
                    appsCompleted = appsCompleted,
                    totalApps = totalApps,
                    filesCompleted = totalFiles,
                    isRunning = true
                )
            }
        )

        phoneDataLayerRepository.sendBackupProgress(
            currentApp = "",
            appsCompleted = processedApps,
            totalApps = totalApps,
            filesCompleted = 0,
            isRunning = false
        )

        return Result.success()
    }
}
```

**Sources:**
- Android Wear OS: Sync data items with Data Layer API — `putDataItem()` and `setUrgent()` for real-time updates: https://developer.android.com/training/wearables/data/data-items
- Wear OS health integration example — `sendToHandheldDevice()` via DataClient: https://developer.android.com/health-and-fitness/fitness/basic-app/integrate-wear-os
- Wear OS: Overview of Data Layer clients — DataClient vs MessageClient: https://developer.android.com/training/wearables/data/overview

---

### [S2-W-02] Push app settings to watch via /settings DataItem path

**File:** `app/src/main/java/com/obsidianbackup/wear/PhoneDataLayerRepository.kt`

**Current state:** `DataLayerListenerService` on the wear side handles `/settings` path in `onDataChanged()` but the app module never calls `DataClient.putDataItem()` on that path.

**Required implementation:**
Add `sendSettings()` to `PhoneDataLayerRepository` and call it from `SettingsViewModel` whenever backup settings change:

```kotlin
// PhoneDataLayerRepository.kt
suspend fun sendSettings(settings: BackupSettings) {
    try {
        val request = PutDataMapRequest.create(DataLayerPaths.SETTINGS_PATH).apply {
            dataMap.putBoolean("auto_backup_enabled", settings.autoBackupEnabled)
            dataMap.putInt("backup_interval_hours", settings.backupIntervalHours)
            dataMap.putString("cloud_provider", settings.cloudProvider.name)
            dataMap.putString("last_updated", Instant.now().toString())
        }.asPutDataRequest().setUrgent()

        Wearable.getDataClient(context).putDataItem(request).await()
        Log.d(TAG, "Settings pushed to watch DataLayer")
    } catch (e: Exception) {
        Log.e(TAG, "Failed to push settings to watch: ${e.message}")
    }
}

// SettingsViewModel.kt — call after saving settings:
fun saveBackupSettings(settings: BackupSettings) {
    viewModelScope.launch {
        settingsRepository.save(settings)
        phoneDataLayerRepository.sendSettings(settings) // wire here
    }
}
```

---

### [S1-ST-10 / S1-SIM-05] Fix getCallingPackage() in TaskerIntegration — replace "unknown" with real UID resolution

**File:** `app/src/main/java/com/obsidianbackup/tasker/TaskerIntegration.kt` lines 419–423

**Current state:** Returns `"unknown"` always. Tasker security validator receives `"unknown"` for every caller, bypassing package-based allow-listing in all builds.

**Required implementation:**
Per Android OS IPC documentation and the Android payment app developer guide (https://web.dev/articles/android-payment-apps-developers-guide), the correct pattern is `Binder.getCallingUid()` → `PackageManager.getPackagesForUid()`. `Binder.getCallingUid()` must be called within the IPC transaction thread — do NOT call it in `onBind()` (which triggers Android Lint warning `BinderGetCallingInMainThread`).

```kotlin
// TaskerIntegration.kt — replace getCallingPackage():

fun getCallingPackage(context: Context): String {
    val callingUid = Binder.getCallingUid()
    if (callingUid == Process.myUid()) {
        // Called from our own process (e.g. tests) — not an IPC call
        return context.packageName
    }
    val packages = context.packageManager.getPackagesForUid(callingUid)
    return when {
        packages == null || packages.isEmpty() -> {
            Log.w(TAG, "No package found for UID $callingUid")
            "unknown:uid:$callingUid"
        }
        packages.size == 1 -> packages[0]
        else -> {
            // Multiple packages share UID (rare but possible with sharedUserId)
            // Return the first; security validator should check against all
            Log.d(TAG, "Multiple packages for UID $callingUid: ${packages.toList()}")
            packages[0]
        }
    }
}
```

Note: For API 28+, you can additionally verify the signing certificate using `PackageManager.hasSigningCertificate(packageName, certBytes, CERT_INPUT_SHA256)` as described in the web.dev payment app guide.

**Sources:**
- Android payment app developer guide — `Binder.getCallingUid()` + `PackageManager.getPackagesForUid()` pattern: https://web.dev/articles/android-payment-apps-developers-guide
- Android Lint rule `BinderGetCallingInMainThread` — must not call in `onBind()`: https://googlesamples.github.io/android-custom-lint-rules/checks/BinderGetCallingInMainThread.md.html
- Android IPC security — Binder caller identity: https://hackthedome.com/module-14-android-components-ipc-security/
- `android.os.Binder.getCallingUid()` official API: https://developer.android.com/reference/android/os/Binder

---

### [S1-CS-03] Wire OAuth flow button to real AppAuth-Android implementation

**File:** `app/src/main/java/com/obsidianbackup/ui/screens/OtherScreens.kt` line 179

**Current state:** `Toast.makeText(context, "OAuth flow - Coming Soon", ...)` — no real OAuth launch.

**Required implementation:**
Use **AppAuth-Android** (openid/AppAuth-Android), which follows RFC 8252 — OAuth 2.0 for Native Apps and RFC 7636 (PKCE). WebView is explicitly NOT supported per AppAuth's own security policy; Chrome Custom Tabs is required.

```kotlin
// build.gradle.kts (app)
implementation("net.openid:appauth:0.11.1")

// OtherScreens.kt or your OAuth2Manager.kt:

fun launchOAuthFlow(
    context: Context,
    provider: CloudProvider,
    resultLauncher: ActivityResultLauncher<Intent>
) {
    val serviceConfig = AuthorizationServiceConfiguration(
        Uri.parse(provider.authorizationEndpoint),
        Uri.parse(provider.tokenEndpoint)
    )

    val authRequest = AuthorizationRequest.Builder(
        serviceConfig,
        provider.clientId,
        ResponseTypeValues.CODE,
        Uri.parse(provider.redirectUri)
    )
        .setScope(provider.scopes)
        .setCodeVerifier(CodeVerifier()) // PKCE — required per RFC 7636
        .build()

    val authService = AuthorizationService(context)
    val authIntent = authService.getAuthorizationRequestIntent(authRequest)
    resultLauncher.launch(authIntent)
}

// Handle result in Activity/Fragment:
val authLauncher = registerForActivityResult(
    ActivityResultContracts.StartActivityForResult()
) { result ->
    val response = AuthorizationResponse.fromIntent(result.data!!)
    val ex = AuthorizationException.fromIntent(result.data!!)
    if (response != null) {
        // Exchange authorization code for tokens
        authService.performTokenRequest(
            response.createTokenExchangeRequest()
        ) { tokenResponse, tokenEx ->
            if (tokenResponse != null) {
                authState.update(tokenResponse, tokenEx)
                keystoreManager.saveOAuthToken(provider, tokenResponse.accessToken!!)
            }
        }
    }
}
```

**Sources:**
- AppAuth-Android (openid/AppAuth-Android) — RFC 8252 + PKCE, Chrome Custom Tabs, no WebView: https://github.com/openid/AppAuth-Android
- RFC 8252 — OAuth 2.0 for Native Apps: https://datatracker.ietf.org/doc/html/rfc8252
- RFC 7636 — PKCE for public clients: https://datatracker.ietf.org/doc/html/rfc7636
- AppAuth-Android full implementation example: https://github.com/gary-archer/oauth.mobilesample.android

---

### [S1-ST-02 / S1-ST-03] Implement listSnapshots() for PCloud and FTP providers

**File (PCloud):** `app/src/main/java/com/obsidianbackup/cloud/providers/PCloudProvider.kt` lines 112–118
**File (FTP):** `app/src/main/java/com/obsidianbackup/cloud/providers/FtpCloudProvider.kt` lines 113–119

**PCloud — Required implementation:**
Use the official pCloud Java SDK (com.pcloud.sdk:java-core:1.9.1 + com.pcloud.sdk:android:1.9.1). The `ApiClient.listFolder()` method returns `RemoteFolder` with `contents` containing all child entries.

```kotlin
// build.gradle.kts (app)
implementation("com.pcloud.sdk:java-core:1.9.1")
implementation("com.pcloud.sdk:android:1.9.1")

// PCloudProvider.kt:
override suspend fun listSnapshots(filter: SnapshotFilter): CloudResult<List<CloudSnapshotInfo>> =
    withContext(Dispatchers.IO) {
        try {
            val apiClient = PCloudSdk.newClientBuilder()
                .authenticator(Authenticators.newOAuthAuthenticator(config.accessToken))
                .create()

            val backupFolder = apiClient.listFolder(config.backupFolderId).execute()

            val snapshots = backupFolder.children()
                .filter { it.isFolder && it.name.startsWith("obsidian_backup_") }
                .map { entry ->
                    CloudSnapshotInfo(
                        id = entry.id.toString(),
                        name = entry.name,
                        createdAt = entry.created,
                        sizeBytes = entry.size,
                        cloudPath = entry.path
                    )
                }
                .sortedByDescending { it.createdAt }

            CloudResult.Success(snapshots)
        } catch (e: Exception) {
            CloudResult.Error(CloudError(LIST_FAILED, "pCloud list error: ${e.message}"))
        }
    }
```

**FTP — Required implementation:**
Use `FTPClient.listFiles(remotePath)` which returns `Array<FTPFile>`:

```kotlin
override suspend fun listSnapshots(filter: SnapshotFilter): CloudResult<List<CloudSnapshotInfo>> =
    withContext(Dispatchers.IO) {
        val client = FTPClient()
        try {
            client.connect(config.host, config.port)
            client.login(config.username, config.password)
            client.enterLocalPassiveMode()
            val files = client.listFiles(config.backupPath)
            val snapshots = files
                .filter { it.isDirectory && it.name.startsWith("obsidian_backup_") }
                .map { f ->
                    CloudSnapshotInfo(
                        id = f.name,
                        name = f.name,
                        createdAt = f.timestamp.time.toInstant(),
                        sizeBytes = f.size,
                        cloudPath = "${config.backupPath}/${f.name}"
                    )
                }
                .sortedByDescending { it.createdAt }
            CloudResult.Success(snapshots)
        } catch (e: Exception) {
            CloudResult.Error(CloudError(LIST_FAILED, "FTP list error: ${e.message}"))
        } finally {
            if (client.isConnected) { client.logout(); client.disconnect() }
        }
    }
```

**Sources:**
- pCloud official Java SDK (listFolder API): https://github.com/pCloud/pcloud-sdk-java
- pCloud `/listfolder` REST endpoint documentation: https://docs.pcloud.com/ (methods/folder/listfolder)
- pCloud API endpoint reference (api.pcloud.com vs eapi.pcloud.com for EU users): https://docs.pcloud.com/

---

### [S1-ST-07] Implement listSnapshots() for MegaCloudProvider

**File:** `app/src/main/java/com/obsidianbackup/cloud/providers/MegaCloudProvider.kt` lines 114–120

**Current state:** Returns `CloudResult.Success(emptyList())` with no directory listing.

**Required implementation:**
Use the unofficial but widely-used **Mega-Java** library (https://github.com/Ale46/Mega-Java), or implement via MEGA's HTTP API. MEGA's official Android app is open-source at https://github.com/meganz/android and uses the C++ SDK JNI bridge. For a lighter path use Mega-Java:

```kotlin
// MegaCloudProvider.kt:
override suspend fun listSnapshots(filter: SnapshotFilter): CloudResult<List<CloudSnapshotInfo>> =
    withContext(Dispatchers.IO) {
        try {
            val megaApi = MegaApiJava(config.apiKey, config.appName, cacheDir.absolutePath)
            megaApi.login(config.email, config.password) // synchronous in background thread
            val backupNode = megaApi.getNodeByPath(config.backupPath)
            val children = megaApi.getChildren(backupNode)

            val snapshots = (0 until children.size())
                .map { children.get(it) }
                .filter { it.isFolder && it.name.startsWith("obsidian_backup_") }
                .map { node ->
                    CloudSnapshotInfo(
                        id = node.handle.toString(),
                        name = node.name,
                        createdAt = Instant.ofEpochSecond(node.creationTime),
                        sizeBytes = node.size,
                        cloudPath = megaApi.getNodePath(node)
                    )
                }
                .sortedByDescending { it.createdAt }

            CloudResult.Success(snapshots)
        } catch (e: Exception) {
            CloudResult.Error(CloudError(LIST_FAILED, "MEGA list error: ${e.message}"))
        }
    }
```

**Sources:**
- Mega-Java library (Ale46/Mega-Java): https://github.com/Ale46/Mega-Java
- MEGA official Android app source: https://github.com/meganz/android
- MEGA C++ SDK: https://github.com/meganz/sdk

---

### [S1-ST-08 / S1-ST-11] Implement GamingBackupManager.restoreGameSaves() and fix GamingBackupViewModel hardcoded game

**File (restore):** `app/src/main/java/com/obsidianbackup/gaming/GamingBackupManager.kt` lines 321–327
**File (viewmodel):** `app/src/main/java/com/obsidianbackup/presentation/gaming/GamingBackupViewModel.kt` lines 46–58

**GamingBackupManager.restoreGameSaves() — Required implementation:**
```kotlin
override suspend fun restoreGameSaves(
    zipFile: File,
    emulator: EmulatorType,
    gameName: String,
    profileSlot: Int
): RestoreResult = withContext(Dispatchers.IO) {
    try {
        val saveDir = emulatorDetector.getSaveDirectory(emulator, gameName)
            ?: return@withContext RestoreResult.Failure("Cannot find save directory for $emulator")

        // Unzip save files to emulator save directory
        ZipInputStream(zipFile.inputStream()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val targetFile = File(saveDir, entry.name)
                targetFile.parentFile?.mkdirs()
                FileOutputStream(targetFile).use { out ->
                    zis.copyTo(out)
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        // If root is required for this emulator path, use root shell
        if (saveDir.path.startsWith("/data") && rootShellManager.isRootAvailable()) {
            rootShellManager.execute("chmod -R 644 '${saveDir.absolutePath}'")
            rootShellManager.execute("chown -R ${emulatorDetector.getEmulatorUid(emulator)} '${saveDir.absolutePath}'")
        }

        RestoreResult.Success(gameName)
    } catch (e: Exception) {
        RestoreResult.Failure("Restore failed: ${e.message}")
    }
}
```

**GamingBackupViewModel — Replace hardcoded GameInfo:**
```kotlin
// REMOVE: val gameInfoList = listOf(GameInfo("Example Game", ...))
// REPLACE WITH:
private fun loadDetectedGames() {
    viewModelScope.launch {
        _uiState.update { it.copy(isScanning = true) }
        val detectedGames = gamingBackupManager.detectGames()
        _uiState.update { it.copy(
            games = detectedGames,
            isScanning = false,
            isEmpty = detectedGames.isEmpty()
        )}
    }
}

// Call loadDetectedGames() in init {} block
```

---

### [S1-CS-01] Enable gaming_backup feature flag

**File:** `app/src/main/java/com/obsidianbackup/features/FeatureFlags.kt` line 51

**Current state:** `"gaming_backup" -> false // Coming in v1.1 - stub implementation`

**Required action:** Change to `"gaming_backup" -> true`. The `GamingBackupManager`, `EmulatorDetector`, `SaveStateManager`, and all DI bindings are fully implemented. The only gate is this feature flag. Enable it and run the gaming backup end-to-end test suite before merging.

---

### [S1-SIM-06] Add catalog integrity signature to CloudSyncManager.exportCatalog()

**File:** `app/src/main/java/com/obsidianbackup/cloud/CloudSyncManager.kt` line 217

**Current state:** `// Export catalog with integrity signatures - simplified implementation` — signature step is omitted.

**Required implementation:**
```kotlin
// CloudSyncManager.kt:
fun exportCatalogWithSignature(catalog: BackupCatalog): ByteArray {
    val catalogJson = json.encodeToString(catalog)
    val catalogBytes = catalogJson.toByteArray(Charsets.UTF_8)

    // Sign with Android Keystore key
    val signature = Signature.getInstance("SHA256withRSA").apply {
        initSign(keystoreManager.getCatalogSigningKey())
        update(catalogBytes)
    }.sign()

    val signatureB64 = Base64.encodeToString(signature, Base64.NO_WRAP)

    val signedCatalog = SignedCatalog(
        catalog = catalogJson,
        signature = signatureB64,
        keyVersion = keystoreManager.getCatalogKeyVersion(),
        timestamp = Instant.now().toString()
    )
    return json.encodeToString(signedCatalog).toByteArray(Charsets.UTF_8)
}
```

---

### [S3-DC-01 / S3-DIS-01] Wire TVNavigationHandler into MainActivity and MainFragment

**File:** `tv/src/main/java/com/obsidianbackup/tv/navigation/TVNavigationHandler.kt`

**Current state:** Full D-pad and media key handler, never called.

**Required implementation:**
```kotlin
// tv/src/main/java/com/obsidianbackup/tv/ui/MainActivity.kt:
class MainActivity : FragmentActivity() {
    private val navigationHandler by lazy { TVNavigationHandler(this) }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return if (event != null && navigationHandler.handleKeyEvent(keyCode, event)) {
            true
        } else {
            super.onKeyDown(keyCode, event)
        }
    }
}
```

---

### [S3-DIS-02] Inject TVSettingsManager into SettingsFragment preference listeners

**File:** `tv/src/main/java/com/obsidianbackup/tv/settings/TVSettingsManager.kt`

**Current state:** DataStore-backed settings manager, never injected. `SettingsFragment` saves to XML preferences only, bypassing all `TVSettingsManager` flows.

**Required implementation:**
Wire preference change callbacks to `TVSettingsManager` suspending setters inside a coroutine scope:

```kotlin
// SettingsFragment.kt inner PrefsFragment:
override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
    lifecycleScope.launch {
        when (preference.key) {
            "auto_backup" -> tvSettingsManager.setAutoBackup(newValue as Boolean)
            "backup_frequency" -> tvSettingsManager.setBackupFrequency((newValue as String).toInt())
            "cloud_provider" -> tvSettingsManager.setCloudProvider(CloudProvider.valueOf(newValue as String))
            "compression" -> tvSettingsManager.setCompression(CompressionLevel.valueOf(newValue as String))
            "encryption" -> tvSettingsManager.setEncryption(newValue as Boolean)
        }
    }
    return true
}
```

---

## ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
## TIER 3 — FIX BEFORE LAUNCH (Coming Soon Gates, Simplified Code)
## ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

---

### [S1-CS-02] Wire Plugin Store button to real marketplace

**File:** `app/src/main/java/com/obsidianbackup/ui/screens/PluginsScreen.kt` line 107

Replace `Toast.makeText(context, "Plugin Store - Coming Soon", ...)` with either:
a) A `WebView`/`CustomTabsIntent` opening the plugin registry URL from config, or
b) A composable `PluginStoreScreen` backed by `PluginRepository.fetchAvailablePlugins()`.

---

### [S1-CS-09 / S1-CS-10] Wire Retention Policies and Storage Limits

**Files:** `SettingsScreen.kt` lines 291 and 299

Both `RetentionPolicyManager` and storage quota infrastructure exist. Replace `showComingSoonDialog` with `navController.navigate(Routes.RETENTION_POLICIES)` and `navController.navigate(Routes.STORAGE_LIMITS)`. Create the corresponding Compose screens backed by the existing managers.

---

### [S1-CS-11] Wire Permission Mode selector

**File:** `SettingsScreen.kt` line 310

`PermissionManager.detectBestMode()` exists. Replace Coming Soon with a radio-button preference screen:
- Root (via ObsidianBoxEngine)
- Shizuku (via ShizukuPermissionManager)
- SAF (Storage Access Framework, no elevated permissions)

---

### [S1-CS-13 / S1-CS-14 / S1-CS-15] Wire Export Diagnostics, App Logs, Shell Audit Logs

**File:** `SettingsScreen.kt` lines 401, 409, 417

Each should:
1. Collect data (system info ZIP / Timber log buffer / shell audit trail)
2. Write to a temp file in `cacheDir`
3. Launch `ShareCompat.IntentBuilder` with the file URI via `FileProvider`

---

### [S1-SIM-02] Replace WebDAV manual JSON with kotlinx.serialization

**File:** `app/src/main/java/com/obsidianbackup/cloud/WebDavCloudProvider.kt` lines 878, 894, 909, 920

`kotlinx.serialization` is already a project dependency. Replace all manual string-building and regex JSON parsing with `@Serializable` data classes and `Json.encodeToString()` / `Json.decodeFromString()`.

---

### [S1-SIM-03 / S1-SIM-04] Fix plugin APK validation on Android 13+

**File:** `PackagePluginDiscovery.kt` line 57 / `PluginValidator.kt` line 128

Use `GET_SIGNATURES or GET_META_DATA` flags correctly. On API 28+, use `PackageManager.hasSigningCertificate()` with `CERT_INPUT_SHA256`. Add certificate chain validation via `CertPathValidator` with a `PKIXParameters` trust anchor.

---

### [S1-PH-02] Replace webdav.example.com placeholder certificate pin

**File:** `app/src/main/java/com/obsidianbackup/security/CertificatePinningManager.kt` lines 60–64

Either remove the example domain entirely or replace with real SHA-256 pins for actual WebDAV servers. Obtain pins via `openssl s_client -connect host:443 | openssl x509 -pubkey -noout | openssl pkey -pubin -outform der | openssl dgst -sha256 -binary | base64`.

---

### [S1-ST-01] Implement getSubscriptionHistory() from BillingRepository

**File:** `app/src/main/java/com/obsidianbackup/billing/SubscriptionManager.kt` lines 237–242

Replace `emptyList()` with a real query to `BillingRepository.getPurchaseHistory()` using Google Play Billing Library's `BillingClient.queryPurchaseHistoryAsync()`.

---

### [S1-ST-09] Fix OAuth2Provider.listAccounts() — read real persisted accounts

**File:** `app/src/main/java/com/obsidianbackup/cloud/oauth/OAuth2Provider.kt` lines 222–226

Replace `listOf("default")` with a call to `KeystoreManager.getPersistedOAuthAccounts()` which reads account IDs previously saved after successful OAuth flows. If no accounts exist, return `emptyList()` — never a fake default.

---

### [S1-DIS-01] Wire VoiceControlHandler into MainActivity or SimplifiedModeViewModel

**File:** `app/src/main/java/com/obsidianbackup/accessibility/VoiceControlHandler.kt`

This is a complete `SpeechRecognizer`-based voice command handler. Inject it into `MainActivity` or `SimplifiedModeViewModel` and call `initialize()` on startup and `startListening()` when the user opts into simplified/accessibility mode.

---

## ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
## TIER 4 — SETTINGS COMING SOON (Wire Infrastructure that Exists)
## ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

For each of the following `SettingsScreen.kt` Coming Soon items, replace `onClick = { showComingSoonDialog = "..." }` with `onClick = { navController.navigate(Routes.<SCREEN>) }` and implement the corresponding Compose screen:

| ID | Item | Line | Existing Backend |
|----|------|------|-----------------|
| S1-CS-04 | Compression Profile | 108 | `CompressionManager.kt` — none/fast/balanced/best levels |
| S1-CS-05 | Sync Policies | 195 | `SyncPolicyManager.kt` — frequency, conditions, conflict resolution |
| S1-CS-06 | Play Games Cloud Sync | 214 | `PlayGamesManager.kt` — Google Play Games Services API |
| S1-CS-07 | Privacy Settings | 233 | `HealthPrivacyManager.kt` — PHI anonymization options |
| S1-CS-08 | Plugin Security | 280 | `PluginSandboxManager.kt` — sandbox config, permission management |
| ~~S1-CS-12~~ | ~~BusyBox Options~~ | ~~393~~ | ✅ **RESOLVED 2026-02-22** — `BusyBoxOptionsScreen` fully implemented. Assets-based extraction, SELinux chcon fix, root fallback. 342 applets verified. |

Also: **`M-7`** — `LicensesScreen.kt` is fully implemented. Route "Open Source Licenses" in `SettingsScreen` to `navController.navigate(Routes.LICENSES)` instead of `showComingSoonDialog`.

---

## ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
## TIER 5 — MISSING WEAR SCREENS
## ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Implement the following missing Compose for Wear OS screens in the `wear` module. Use Jetpack Compose for Wear OS (`androidx.wear.compose:compose-material:1.3+`):

| Missing Screen | Description |
|----------------|-------------|
| App Selection Screen | `ScalingLazyColumn` listing backed apps; user toggles per-app inclusion in watch-triggered backup |
| Restore Screen | Step-through flow: select backup snapshot → confirm → `MessageClient.sendMessage()` restore trigger to phone |
| Settings Screen | `PreferenceScreen` equivalent — show current auto-backup status, interval, cloud provider name (read-only from DataLayer) |
| Connectivity Error Screen | Dedicated screen shown when `CapabilityClient.getAllCapabilities()` returns no phone node; includes "Retry" button |

---

## ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
## TIER 6 — TV MODULE DATA LAYER (Architecture Decision Required First)
## ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

**Before any TV implementation work:** Answer the TV-1 architecture question: Is the TV module a **standalone** backup client for the TV device's own data, or a **companion remote-management UI** for the phone's backup system?

- **Standalone** → TV needs its own `BackupWorker`, its own `Room` or `ContentProvider`, and `TVBackupManager.startBackup()` should use WorkManager directly.
- **Companion** → TV needs a connection protocol to the phone. Android TV does not have the Wear DataLayer API. Options: `ContentProvider` registered in app module (simplest, same-device), Nearby Connections API (cross-device), or local HTTP server in the app module.

**Once the decision is made, implement:**

**S3-MI-02 — Register ContentProvider in app module for TV access:**
```xml
<!-- app/AndroidManifest.xml -->
<provider
    android:name=".backup.BackupCatalogProvider"
    android:authorities="com.obsidianbackup.catalog"
    android:exported="false"
    android:readPermission="com.obsidianbackup.permission.READ_CATALOG"
    android:grantUriPermissions="true" />
```

**S3-MI-03 — Query ContentProvider from TV module:**
```kotlin
// TVBackupManager.kt:
fun loadBackupHistory(): List<BackupItem> {
    val uri = Uri.parse("content://com.obsidianbackup.catalog/backups")
    val cursor = context.contentResolver.query(uri, null, null, null, "created_at DESC")
    return cursor?.use { c ->
        buildList {
            while (c.moveToNext()) {
                add(BackupItem(
                    name = c.getString(c.getColumnIndexOrThrow("name")),
                    date = c.getString(c.getColumnIndexOrThrow("created_at")),
                    size = c.getString(c.getColumnIndexOrThrow("size_formatted"))
                ))
            }
        }
    } ?: emptyList()
}
```

**S3-ST-02 — Replace hardcoded backup history in BackupDetailsFragment:**
Replace the two hardcoded `BackupItem("Full Backup", "2024-01-15 14:30", "125 MB")` entries with `tvBackupManager.loadBackupHistory()` called in `setupRelatedContent()`.

---

## ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
## IMPLEMENTATION RULES — APPLY TO EVERY CHANGE
## ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

1. **No new stubs.** Every function you implement must perform the actual operation described. If it cannot be completed in the current session, leave it as its original stub and document why — do not introduce a new stub that returns fake data.

2. **No `emptyList()` returns from functions that should query data.** If a list function returns empty, it should be because the query genuinely returned nothing — not because the query was never executed.

3. **No `// simplified` comments on new code.** If you must simplify, note it as `// TODO(production): replace with X` and open a GitHub Issue.

4. **Hilt injection.** All new dependencies must be provided through the existing Hilt modules. Do not instantiate singletons directly with `SomeClass(context)` if Hilt already manages them.

5. **Coroutine discipline.** All I/O operations (network, file system, SMB, FTP, SFTP) must run on `Dispatchers.IO`. All Data Layer writes (DataClient, MessageClient) must be in a `CoroutineScope` with a `SupervisorJob` and proper cancellation.

6. **Error handling.** Every `try/catch` must log the exception at `Log.e(TAG, ...)` before converting to a domain error type. Never silently swallow exceptions.

7. **Test coverage.** For every stub replaced with a real implementation, a corresponding unit test or instrumentation test must be added in the appropriate `test/` or `androidTest/` directory.

8. **Wear OS DataLayer check.** Before any `DataClient` or `MessageClient` call, verify availability with `GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context)` as recommended at https://developer.android.com/training/wearables/data/data-layer.

---

## DEPENDENCY SUMMARY (add to relevant build.gradle.kts files)

```kotlin
// app/build.gradle.kts
implementation("org.bouncycastle:bcprov-jdk18on:1.79")          // ML-KEM / ML-DSA PQC
implementation("commons-net:commons-net:3.10.0")                // FTP/FTPS
implementation("com.github.mwiede:jsch:0.2.16")                 // SFTP
implementation("eu.agno3.jcifs:jcifs-ng:2.1.9")                 // SMB2/SMB3
implementation("net.openid:appauth:0.11.1")                      // OAuth 2.0 PKCE
implementation("com.pcloud.sdk:java-core:1.9.1")                // pCloud
implementation("com.pcloud.sdk:android:1.9.1")                  // pCloud Android

// wear/build.gradle.kts
implementation("androidx.wear.compose:compose-material:1.3.1")  // Compose for Wear OS
implementation("androidx.wear.compose:compose-foundation:1.3.1")
```

---

*All source citations are live as of February 2026. Every prescribed library is available on Maven Central. Every API reference is from official Android, NIST, Oracle, or GitHub documentation.*
