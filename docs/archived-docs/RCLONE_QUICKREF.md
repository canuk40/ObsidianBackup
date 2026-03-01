# Rclone Integration - Quick Reference

## Component Locations

| Component | File Path | Purpose |
|-----------|-----------|---------|
| **Binary Manager** | `app/src/main/java/com/obsidianbackup/cloud/rclone/RcloneBinaryManager.kt` | Binary packaging, extraction, verification |
| **Google Drive** | `app/src/main/java/com/obsidianbackup/cloud/rclone/backends/RcloneGoogleDriveProvider.kt` | Google Drive backend |
| **Dropbox** | `app/src/main/java/com/obsidianbackup/cloud/rclone/backends/RcloneDropboxProvider.kt` | Dropbox backend |
| **S3** | `app/src/main/java/com/obsidianbackup/cloud/rclone/backends/RcloneS3Provider.kt` | S3 + compatible backends |
| **Factory** | `app/src/main/java/com/obsidianbackup/cloud/rclone/RcloneProviderFactory.kt` | Provider instantiation |
| **Executor** | `app/src/main/java/com/obsidianbackup/cloud/rclone/RcloneExecutor.kt` | Command execution |
| **Config Manager** | `app/src/main/java/com/obsidianbackup/cloud/rclone/RcloneConfigManager.kt` | Config file management |
| **Base Provider** | `app/src/main/java/com/obsidianbackup/cloud/rclone/RcloneCloudProvider.kt` | Abstract base class |
| **UI Selection** | `app/src/main/java/com/obsidianbackup/ui/cloud/RcloneProviderSelectionScreen.kt` | Provider selection UI |

## Quick Usage

### Create Provider
```kotlin
val provider = RcloneProviderFactory.create(
    context,
    RcloneProviderFactory.ProviderType.GOOGLE_DRIVE
)
```

### Initialize
```kotlin
val config = CloudConfig(
    providerId = "rclone-gdrive",
    credentials = mapOf("token" to "...")
)
provider.initialize(config)
```

### Upload
```kotlin
provider.uploadFile(localFile, "remote/path.tar.zst")
```

### Download
```kotlin
provider.downloadFile("remote/path.tar.zst", localFile)
```

### List
```kotlin
val files = provider.listFiles("remote/")
```

### Delete
```kotlin
provider.deleteFile("remote/path.tar.zst")
```

## Binary Packaging

Place rclone binaries in:
```
app/src/main/jniLibs/
├── armeabi-v7a/librclone.so
├── arm64-v8a/librclone.so
├── x86/librclone.so
└── x86_64/librclone.so
```

Download from: https://rclone.org/downloads/

## Provider Types

| Type | Provider ID | Auth Method | Max File Size |
|------|-------------|-------------|---------------|
| GOOGLE_DRIVE | rclone-gdrive | OAuth2 | 5TB |
| DROPBOX | rclone-dropbox | OAuth2 | 350GB |
| S3 | rclone-s3 | Access Key | 5TB |
| ONEDRIVE | rclone-onedrive | OAuth2 | - |
| BACKBLAZE_B2 | rclone-b2 | Key/Secret | - |
| WEBDAV | rclone-webdav | Basic Auth | - |
| SFTP | rclone-sftp | SSH Key | - |

## Configuration Examples

### Google Drive
```kotlin
CloudConfig(
    providerId = "rclone-gdrive",
    credentials = mapOf(
        "token" to """{"access_token":"...","refresh_token":"..."}"""
    )
)
```

### Dropbox
```kotlin
CloudConfig(
    providerId = "rclone-dropbox",
    credentials = mapOf(
        "token" to """{"access_token":"..."}"""
    )
)
```

### S3
```kotlin
CloudConfig(
    providerId = "rclone-s3",
    credentials = mapOf(
        "access_key_id" to "AKIAIOSFODNN7EXAMPLE",
        "secret_access_key" to "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY"
    ),
    region = "us-east-1",
    bucket = "my-bucket"
)
```

## Status: 100% Complete ✅

All components implemented and integrated.
