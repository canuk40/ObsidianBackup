# Quick Import Patterns Reference
## For Future Development

### Most Common Missing Imports

#### 1. Result Types (Cloud & Repository Code)
```kotlin
import com.obsidianbackup.model.Result
import com.obsidianbackup.model.Result.Success
import com.obsidianbackup.model.Result.Error
```
**Used in**: CloudSyncManager, CloudSyncRepository, OAuth2Manager, GoogleDriveProvider, etc.

#### 2. Data Models
```kotlin
import com.obsidianbackup.model.AppInfo
import com.obsidianbackup.model.VerificationResult
import com.obsidianbackup.model.FeatureTier
import com.obsidianbackup.model.BackupComponent
```
**Used in**: UI screens, ViewModels, Engine classes

#### 3. Coroutine Flow (ViewModels & State Management)
```kotlin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.delay
```
**Used in**: ViewModels, Managers, Server classes

#### 4. Navigation & Hilt (Compose Screens)
```kotlin
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
```
**Used in**: All Composable screens

#### 5. Material3 Components (UI)
```kotlin
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
```
**Used in**: All Composable screens

#### 6. Standard Library
```kotlin
import java.util.UUID
import java.io.File
import java.security.MessageDigest
```
**Used in**: Utility classes, Engines

### Material2 → Material3 Migration Patterns

| Material 2 | Material 3 |
|------------|------------|
| `MaterialTheme.colors.primary` | `MaterialTheme.colorScheme.primary` |
| `MaterialTheme.colors.background` | `MaterialTheme.colorScheme.background` |
| `typography.h6` | `typography.titleLarge` |
| `typography.subtitle1` | `typography.titleMedium` |
| `typography.body1` | `typography.bodyLarge` |
| `typography.body2` | `typography.bodyMedium` |
| `typography.caption` | `typography.labelSmall` |

### Common Fix Patterns

#### Pattern 1: Unresolved Result.Success/Error
**Error**: `Unresolved reference: Success`
**Fix**: Add specific imports
```kotlin
import com.obsidianbackup.model.Result.Success
import com.obsidianbackup.model.Result.Error
```

#### Pattern 2: Flow state management
**Error**: `Unresolved reference: asStateFlow`
**Fix**:
```kotlin
import kotlinx.coroutines.flow.asStateFlow
```

#### Pattern 3: Missing delay
**Error**: `Unresolved reference: delay`
**Fix**:
```kotlin
import kotlinx.coroutines.delay
```

#### Pattern 4: UUID generation
**Error**: `Unresolved reference: UUID`
**Fix**:
```kotlin
import java.util.UUID
```

### Files That Typically Need These Imports

**ViewModels** need:
- Flow imports (MutableStateFlow, asStateFlow)
- Result types
- Data models

**Screens** need:
- hiltViewModel
- Material3 components
- Navigation imports
- Data models for display

**Repositories** need:
- Result types (Success, Error)
- Coroutine flow
- Data models

**Managers** need:
- Flow state management
- Delay for retry logic
- Result types

**Engines** need:
- UUID for transaction IDs
- File operations
- Data models
- Result types
