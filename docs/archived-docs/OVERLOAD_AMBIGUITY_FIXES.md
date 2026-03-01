# Overload Resolution Ambiguity Fixes

## Summary
Fixed all 14 "Overload resolution ambiguity" errors in the ObsidianBackup project.

## Files Modified

### 1. WebDavCloudProvider.kt (2 fixes)
**Problem**: Two `downloadFile` functions with same signature:
- `override suspend fun downloadFile(remotePath: String, localFile: File)` (line 395)
- `private fun downloadFile(remoteUrl: String, localFile: File)` (line 475)

**Solution**: Renamed private helper to `downloadFileInternal` to disambiguate.

**Changes**:
- Line 475: Renamed `downloadFile` → `downloadFileInternal`
- Line 190: Updated call to `downloadFileInternal`
- Line 411: Updated call to `downloadFileInternal`

### 2. BackblazeB2Provider.kt (4 fixes)
**Problem**: Two `downloadFile` functions with ambiguous signatures:
- `override suspend fun downloadFile(remotePath: String, localFile: File)` (line 486)
- `private suspend fun downloadFile(fileId: String, destination: File)` (line 809)

**Solution**: Renamed private helper to `downloadFileById` to make intent clear.

**Changes**:
- Line 809: Renamed `downloadFile` → `downloadFileById`
- Line 245: Updated call to `downloadFileById`
- Line 437: Updated call to `downloadFileById`
- Line 499: Updated call to `downloadFileById`
- Line 828: Updated call to `downloadFileById`

### 3. BoxCloudProvider.kt (5 fixes)
**Problem**: Similar ambiguity with `downloadFile` overloads, plus recursive call issue.
- `override suspend fun downloadFile(remotePath: String, localFile: File)` (line 481)
- `private suspend fun downloadFile(fileId: String, destination: File)` (line 691)

**Solution**: 
1. Renamed private helper to `downloadFileById`
2. Fixed recursive call by implementing proper file lookup logic
3. Added explicit type cast for `fileSize.toLong()` to resolve `Long + ?` ambiguity

**Changes**:
- Line 691: Renamed `downloadFile` → `downloadFileById`
- Line 250: Updated call to `downloadFileById`
- Line 251: Changed `downloadedBytes += fileSize` to `downloadedBytes += fileSize.toLong()`
- Line 432: Updated call to `downloadFileById`
- Line 484-495: Fixed recursive call by adding file lookup logic
- Line 708: Updated call to `downloadFileById`

### 4. SmartBackupIntegration.kt (1 fix)
**Problem**: Lambda parameter `padding` conflicts with imported `padding()` function.

**Solution**: Renamed lambda parameter to `paddingValues`.

**Changes**:
- Line 138: Changed `{ padding ->` to `{ paddingValues ->`
- Line 141: Changed `.padding(padding)` to `.padding(paddingValues)`
- Added missing imports:
  - `androidx.compose.runtime.Composable`
  - `androidx.compose.runtime.collectAsState`
  - `androidx.compose.runtime.getValue`
  - `androidx.compose.runtime.setValue`
- Line 159: Added explicit type to lambda: `{ prediction: com.obsidianbackup.ml.models.BackupPrediction ->`

### 5. AppScanner.kt (2 fixes)
**Problem**: `sumOf` lambda had ambiguous type inference with `File(path).length()`.

**Solution**: Added explicit type annotations to lambda parameter.

**Changes**:
- Line 107: Changed `sumOf { path ->` to `sumOf { path: String ->`
- Line 107: Added explicit type `val splitSizes: Long = ...`

## Verification
All 14 overload resolution ambiguity errors have been resolved. Confirmed by compiling with:
```bash
./gradlew :app:compileFreeDebugKotlin --no-daemon
```

No "Overload resolution ambiguity" errors remain in the build output.

## Root Causes Summary
1. **Function overloading**: Multiple functions with same name and similar parameters
2. **Name shadowing**: Lambda parameters conflicting with imported function names
3. **Type inference failures**: Compiler unable to determine correct overload without explicit types

## Best Practices Applied
1. Use descriptive function names to avoid ambiguity (e.g., `downloadFileById` vs `downloadFileInternal`)
2. Rename lambda parameters that conflict with function names
3. Add explicit type annotations when type inference is ambiguous
4. Use fully qualified names when necessary
5. Avoid wildcard imports that might cause conflicts
