# ObsidianBackup - Bug List

**Generated:** 2024-02-10  
**Total Bugs:** 8  
**Critical:** 4  
**High:** 1  
**Medium:** 2  
**Low:** 1  

---

## Critical Bugs (Prevent Compilation)

### Bug #1: CatalogRepository Interface Mismatch
**Priority:** 🔴 Critical  
**Status:** Open  
**Blocks:** Compilation  

**Location:**
- File: `app/src/main/java/com/obsidianbackup/data/repository/CatalogRepository.kt`
- Lines: 16, 59

**Error Messages:**
```
e: Class 'CatalogRepository' is not abstract and does not implement abstract member 'getLastFullBackupForApp'.
e: 'getLastFullBackupForApp' overrides nothing.
```

**Description:**
The CatalogRepository class implements an interface (likely ICatalogRepository) that requires a `getLastFullBackupForApp` method, but the implementation is missing or has the wrong signature.

**Root Cause:**
- Interface definition requires: `getLastFullBackupForApp(appId: AppId): BackupMetadata?`
- Current implementation has wrong signature or is missing entirely

**Impact:**
- Blocks all compilation
- Prevents APK generation
- Prevents any testing

**Suggested Fix:**
```kotlin
// In CatalogRepository.kt
override suspend fun getLastFullBackupForApp(appId: AppId): BackupMetadata? {
    return backupCatalog.getLastFullBackupForApp(appId.value)
        ?.let { entity -> 
            // Convert entity to BackupMetadata
            BackupMetadata(
                id = BackupId(entity.id),
                appId = appId,
                timestamp = entity.timestamp,
                // ... other fields
            )
        }
}
```

**Estimated Fix Time:** 30 minutes

---

### Bug #2: AppId Unresolved Reference
**Priority:** 🔴 Critical  
**Status:** Open  
**Blocks:** Compilation  

**Location:**
- File: `app/src/main/java/com/obsidianbackup/data/repository/CatalogRepository.kt`
- Line: 59

**Error Message:**
```
e: Unresolved reference 'AppId'.
```

**Description:**
The `AppId` type is referenced but not imported or doesn't exist in the accessible scope.

**Root Cause:**
- Missing import statement for AppId
- OR: AppId is defined in wrong package
- OR: AppId doesn't exist and should be a different type

**Investigation Needed:**
1. Check if AppId is defined in `model/` package
2. Check if it should be a String or different type
3. Verify expected package structure

**Suggested Fix:**
```kotlin
// Option 1: Add import
import com.obsidianbackup.model.AppId

// Option 2: If AppId doesn't exist, create it
package com.obsidianbackup.model

@JvmInline
value class AppId(val value: String)

// Option 3: Use existing type (if AppId is actually packageName: String)
override suspend fun getLastFullBackupForApp(packageName: String): BackupMetadata? {
    // implementation
}
```

**Estimated Fix Time:** 15 minutes

---

### Bug #3: DI Binding Type Mismatch
**Priority:** 🔴 Critical  
**Status:** Open  
**Blocks:** Compilation  

**Location:**
- File: `app/src/main/java/com/obsidianbackup/di/AppModule.kt`
- Line: 228

**Error Messages:**
```
e: Argument type mismatch: actual type is 'com.obsidianbackup.storage.BackupCatalog', 
   but 'com.obsidianbackup.domain.repository.ICatalogRepository' was expected.
e: No value passed for parameter 'backupCatalog'.
```

**Description:**
The Hilt dependency injection is trying to bind `BackupCatalog` where `ICatalogRepository` is expected. Additionally, a required parameter `backupCatalog` is not being passed.

**Root Cause:**
Two possibilities:
1. CatalogRepository constructor expects ICatalogRepository but receives BackupCatalog
2. The @Provides method has wrong parameter types

**Current Code (Likely):**
```kotlin
@Provides
@Singleton
fun provideCatalogRepository(
    backupCatalog: BackupCatalog  // Wrong type
): ICatalogRepository {
    return CatalogRepository(backupCatalog)  // Type mismatch
}
```

**Suggested Fix:**
```kotlin
@Provides
@Singleton
fun provideCatalogRepository(
    backupCatalog: BackupCatalog
): ICatalogRepository {
    // Option 1: If CatalogRepository wraps BackupCatalog
    return CatalogRepository(
        catalog = backupCatalog  // Named parameter
    )
    
    // Option 2: If BackupCatalog implements ICatalogRepository
    return backupCatalog as ICatalogRepository
    
    // Option 3: Create adapter
    return CatalogRepositoryAdapter(backupCatalog)
}
```

**Estimated Fix Time:** 20 minutes

---

### Bug #4: Spacing Import Conflict
**Priority:** 🔴 Critical  
**Status:** Open  
**Blocks:** Compilation  

**Location:**
- Files: 
  - `app/src/main/java/com/obsidianbackup/ui/screens/AppsScreen.kt`
  - `app/src/main/java/com/obsidianbackup/ui/screens/AutomationScreen.kt`
  - (Possibly more)

**Error Messages:**
```
e: Conflicting import: imported name 'Spacing' is ambiguous.
```

**Description:**
Multiple packages export a `Spacing` object/class, causing import ambiguity. Common in projects where both Material theme and custom theme define spacing constants.

**Root Cause:**
Likely imports:
- `com.obsidianbackup.ui.theme.Spacing`
- `androidx.compose.material3.Spacing` (or similar)

**Current Code:**
```kotlin
import com.obsidianbackup.ui.theme.Spacing
import com.some.other.package.Spacing  // Conflict!
```

**Suggested Fix:**
```kotlin
// Option 1: Use fully qualified names
import com.obsidianbackup.ui.theme.Spacing
// Then use: Spacing.md

// Option 2: Import with alias
import com.obsidianbackup.ui.theme.Spacing as ThemeSpacing
import com.some.other.package.Spacing as OtherSpacing
// Then use: ThemeSpacing.md

// Option 3: Remove conflicting import
// Only import the one actually needed
import com.obsidianbackup.ui.theme.Spacing
// Remove or comment out the other import
```

**Files to Fix:**
1. AppsScreen.kt
2. AutomationScreen.kt
3. (Run grep to find all files with Spacing import conflict)

**Estimated Fix Time:** 10 minutes (5 minutes per file × 2 files)

---

## High Priority Bugs (Prevent Runtime)

### Bug #5: BackupId/SnapshotId Type Mismatch
**Priority:** 🟠 High  
**Status:** Open  
**Blocks:** Runtime  

**Location:**
- File: `app/src/main/java/com/obsidianbackup/domain/backup/BackupOrchestrator.kt`
- Line: 319

**Error Message:**
```
e: Argument type mismatch: actual type is 'com.obsidianbackup.model.BackupId', 
   but 'com.obsidianbackup.model.SnapshotId' was expected.
```

**Description:**
BackupOrchestrator is passing a `BackupId` to a function that expects `SnapshotId`. Both are value classes wrapping String, but Kotlin treats them as distinct types.

**Root Cause:**
Type safety between BackupId (used internally by engines) and SnapshotId (used in results/UI) requires explicit conversion.

**Current Code (Likely):**
```kotlin
// Line 319
someFunction(backupId)  // BackupId passed
```

**Suggested Fix:**
```kotlin
// Option 1: Convert explicitly
someFunction(SnapshotId(backupId.value))

// Option 2: Add extension function
fun BackupId.toSnapshotId(): SnapshotId = SnapshotId(this.value)
fun SnapshotId.toBackupId(): BackupId = BackupId(this.value)

// Then use:
someFunction(backupId.toSnapshotId())
```

**Impact:**
- Won't crash at compile time (once compilation errors fixed)
- WILL crash at runtime when this code path is executed
- Likely affects: Backup completion, snapshot creation

**Estimated Fix Time:** 15 minutes

---

## Medium Priority Bugs (Affect Functionality)

### Bug #6: AnimatedVisibility Unresolved
**Priority:** 🟡 Medium  
**Status:** Open  
**Blocks:** UI Animations  

**Location:**
- File: `app/src/main/java/com/obsidianbackup/ui/screens/AppsScreen.kt`
- Line: 81

**Error Message:**
```
e: Unresolved reference 'AnimatedVisibility'.
```

**Description:**
Missing import for Compose animation component.

**Root Cause:**
Import statement missing: `import androidx.compose.animation.AnimatedVisibility`

**Suggested Fix:**
```kotlin
// Add to imports
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
```

**Impact:**
- UI animations won't work
- Content transitions will be instant (no animation)
- No visual polish

**Estimated Fix Time:** 5 minutes

---

### Bug #7: Animations Object Unresolved
**Priority:** 🟡 Medium  
**Status:** Open  
**Blocks:** UI Transitions  

**Location:**
- File: `app/src/main/java/com/obsidianbackup/ui/screens/AppsScreen.kt`
- Lines: 83-84

**Error Message:**
```
e: Unresolved reference 'Animations'.
```

**Description:**
Custom `Animations` object/class is referenced but not imported or doesn't exist.

**Root Cause:**
Either:
1. Missing import for custom Animations utility
2. Animations object doesn't exist and needs to be created
3. Should use Compose animation APIs directly

**Investigation Needed:**
Check if `Animations` object exists in codebase:
```bash
grep -r "object Animations" app/src/main/java/
```

**Suggested Fix:**
```kotlin
// Option 1: If Animations exists, add import
import com.obsidianbackup.ui.animations.Animations

// Option 2: If doesn't exist, create it
package com.obsidianbackup.ui.animations

object Animations {
    val fadeInOut = fadeIn() + fadeOut()
    val slideInOut = slideInVertically() + slideOutVertically()
    // etc.
}

// Option 3: Use Compose APIs directly
AnimatedVisibility(
    visible = isVisible,
    enter = fadeIn(animationSpec = tween(300)),
    exit = fadeOut(animationSpec = tween(300))
) {
    // content
}
```

**Impact:**
- UI transition animations won't work
- Possible crash if Animations is accessed
- Degrades user experience

**Estimated Fix Time:** 10 minutes (if exists), 30 minutes (if needs creation)

---

## Low Priority Issues

### Bug #8: Room Schema Export Warning
**Priority:** 🟢 Low  
**Status:** Open  
**Blocks:** Nothing (warning only)  

**Location:**
- All Room database compilation

**Warning Message:**
```
warning: Schema export directory was not provided to the annotation processor 
so Room cannot export the schema.
```

**Description:**
Room is configured to export database schema for versioning, but no export directory is specified.

**Root Cause:**
Missing `room.schemaLocation` in build.gradle.kts or missing Room Gradle plugin.

**Suggested Fix:**
```kotlin
// In app/build.gradle.kts

// Option 1: Apply Room Gradle plugin
plugins {
    id("androidx.room") version "2.6.0"
}

// Option 2: Configure kapt
kapt {
    arguments {
        arg("room.schemaLocation", "$projectDir/schemas")
    }
}

// Option 3: Disable schema export
@Database(
    entities = [/* ... */],
    version = 1,
    exportSchema = false  // Disable export
)
abstract class BackupDatabase : RoomDatabase()
```

**Impact:**
- Schema migration tracking is harder
- No schema versioning files generated
- Purely development/maintenance concern
- No runtime impact

**Estimated Fix Time:** 5 minutes

---

## Bug Summary Table

| Bug # | Title | Priority | Type | Impact | Est. Fix Time |
|-------|-------|----------|------|--------|---------------|
| 1 | CatalogRepository Interface Mismatch | 🔴 Critical | Compilation | Blocks all compilation | 30 min |
| 2 | AppId Unresolved Reference | 🔴 Critical | Compilation | Blocks all compilation | 15 min |
| 3 | DI Binding Type Mismatch | 🔴 Critical | Compilation | Blocks all compilation | 20 min |
| 4 | Spacing Import Conflict | 🔴 Critical | Compilation | Blocks all compilation | 10 min |
| 5 | BackupId/SnapshotId Mismatch | 🟠 High | Runtime | Crashes at runtime | 15 min |
| 6 | AnimatedVisibility Unresolved | 🟡 Medium | UI | No animations | 5 min |
| 7 | Animations Object Unresolved | 🟡 Medium | UI | No transitions | 10-30 min |
| 8 | Room Schema Export Warning | 🟢 Low | Warning | None (warning only) | 5 min |

**Total Estimated Fix Time:** 1.5 - 2 hours

---

## Fix Priority Order

### Phase 1: Unblock Compilation (Critical)
1. **Bug #2: AppId Unresolved** (15 min)
   - Must fix first (blocking #1)
2. **Bug #1: CatalogRepository Interface** (30 min)
   - Depends on #2 being fixed
3. **Bug #3: DI Binding Type Mismatch** (20 min)
   - Depends on #1 being fixed
4. **Bug #4: Spacing Import Conflict** (10 min)
   - Independent, can be done anytime

**Phase 1 Total:** 75 minutes

### Phase 2: Fix Runtime Issues (High)
5. **Bug #5: BackupId/SnapshotId Mismatch** (15 min)
   - Once compilation works, fix this to prevent crashes

**Phase 2 Total:** 15 minutes

### Phase 3: Improve UI (Medium)
6. **Bug #6: AnimatedVisibility Import** (5 min)
7. **Bug #7: Animations Object** (10-30 min)

**Phase 3 Total:** 15-35 minutes

### Phase 4: Clean Warnings (Low)
8. **Bug #8: Room Schema Export** (5 min)

**Phase 4 Total:** 5 minutes

**Grand Total:** 110-130 minutes (1.8-2.2 hours)

---

## Testing After Fixes

### After Phase 1 (Compilation Fixed):
```bash
# Verify compilation
./gradlew clean assembleFreeDebug

# Expected: BUILD SUCCESSFUL
```

### After Phase 2 (Runtime Fixed):
```bash
# Run unit tests
./gradlew testFreeDebugUnitTest

# Expected: All tests pass (or pre-existing failures only)
```

### After Phase 3 (UI Fixed):
```bash
# Build and install
./gradlew installFreeDebug

# Manual test: Check UI animations work
```

### After Phase 4 (Warnings Cleaned):
```bash
# Verify no warnings
./gradlew assembleFreeDebug 2>&1 | grep -i warning

# Expected: No Room schema warnings
```

---

## Prevention Strategies

### To Prevent Future Bugs:

1. **Enable Strict Type Checking**
   ```kotlin
   // In build.gradle.kts
   tasks.withType<KotlinCompile> {
       kotlinOptions {
           allWarningsAsErrors = true
       }
   }
   ```

2. **Add Pre-commit Hooks**
   ```bash
   # .git/hooks/pre-commit
   #!/bin/bash
   ./gradlew assembleFreeDebug
   if [ $? -ne 0 ]; then
       echo "Build failed. Commit rejected."
       exit 1
   fi
   ```

3. **CI/CD Integration**
   ```yaml
   # .github/workflows/build.yml
   - name: Build Debug APK
     run: ./gradlew assembleFreeDebug
   - name: Run Unit Tests
     run: ./gradlew testFreeDebugUnitTest
   ```

4. **Add Architecture Tests**
   ```kotlin
   // ArchitectureTest.kt
   @Test
   fun `verify all interfaces have implementations`() {
       // Check all @Provides methods match parameter types
   }
   
   @Test
   fun `verify no import conflicts`() {
       // Scan for ambiguous imports
   }
   ```

5. **Use Detekt Rules**
   ```yaml
   # detekt.yml
   style:
     UnusedImports:
       active: true
     MagicNumber:
       active: true
   ```

---

## Related Documentation

- **Architecture:** `ARCHITECTURE_AUDIT_REPORT.md`
- **Testing:** `FEATURE_TEST_REPORT.md`
- **Build Guide:** `00_START_HERE_TESTING.md`
- **Performance:** `PERFORMANCE_AUDIT_REPORT.md`

---

## Bug Tracking

### Status Definitions:
- **Open:** Bug identified, not yet fixed
- **In Progress:** Currently being worked on
- **Fixed:** Fix implemented and verified
- **Closed:** Fix deployed to production
- **Won't Fix:** Decided not to fix (with justification)

### Update History:
- 2024-02-10: Initial bug list created (8 bugs identified)

---

**Bug List End**
