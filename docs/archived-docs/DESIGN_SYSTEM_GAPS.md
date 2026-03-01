# Design System Gaps - ObsidianBackup

**Purpose:** Catalog missing Material 3 components and design patterns  
**Status:** 🔴 Multiple gaps identified  

---

## Executive Summary

ObsidianBackup's design system is **70% complete**. While core components exist, several Material 3 patterns and advanced components are missing or underutilized.

**Key Gaps:**
1. 🔴 **No spacing/elevation tokens** (inconsistency)
2. 🔴 **Bottom sheets unused** (missed UX pattern)
3. 🟠 **SearchBar missing** (expected in modern apps)
4. 🟠 **NavigationBar/Rail missing** (tablet optimization)
5. 🟡 **Badge component missing** (notification counts)
6. 🟡 **Chip variants underused** (filters, tags)

---

## 1. Design Tokens (CRITICAL)

### ❌ **Missing: Spacing.kt**

**Current State:**
- 89 instances of `16.dp` (hardcoded)
- 47 instances of `8.dp`
- 18 instances of `12.dp`
- No standard reference

**Required:**
```kotlin
object Spacing {
    val xxxs = 2.dp
    val xxs = 4.dp
    val xs = 8.dp
    val sm = 12.dp
    val md = 16.dp    // PRIMARY standard
    val lg = 24.dp
    val xl = 32.dp
    val xxl = 48.dp
    val xxxl = 64.dp
}
```

**Impact:** 🔴 Critical - Affects all screens  
**Effort:** ⏱️ 2 hours to create, 8 hours to refactor  

---

### ❌ **Missing: Elevation.kt**

**Current State:**
- Some cards have no elevation
- Mixed values (2dp, 4dp, 8dp) without standard

**Required:**
```kotlin
object Elevation {
    val none = 0.dp
    val subtle = 1.dp
    val low = 2.dp
    val medium = 4.dp     // DEFAULT
    val high = 8.dp
    val highest = 12.dp
}
```

**Impact:** 🔴 High - Inconsistent depth perception  
**Effort:** ⏱️ 1 hour to create, 4 hours to refactor  

---

### ❌ **Missing: IconSize.kt**

**Current State:**
- Mixed sizes: 16.dp, 24.dp, 48.dp, 64.dp, 120.dp

**Required:**
```kotlin
object IconSize {
    val small = 16.dp
    val medium = 24.dp    // DEFAULT
    val large = 48.dp
    val xlarge = 64.dp
    val hero = 120.dp
}
```

**Impact:** 🟡 Medium - Visual consistency  
**Effort:** ⏱️ 1 hour to create, 2 hours to refactor  

---

## 2. Material 3 Components

### Component Coverage Matrix

| Component | Status | Usage | Priority | Notes |
|-----------|--------|-------|----------|-------|
| **Button** | ✅ Used | 90% | - | Good |
| **FilledButton** | ✅ Used | 70% | - | Primary action |
| **OutlinedButton** | 🟡 Partial | 30% | P2 | Underused |
| **TextButton** | ✅ Used | 60% | - | Dialogs |
| **FilledTonalButton** | ❌ Missing | 0% | P1 | Secondary actions |
| **ElevatedButton** | ❌ Missing | 0% | P3 | Alternative style |
| **FloatingActionButton** | ✅ Used | 80% | - | Good |
| **ExtendedFAB** | ✅ Used | 60% | - | Good |
| **SmallFAB** | ❌ Missing | 0% | P3 | Compact screens |
| **LargeFAB** | ❌ Missing | 0% | P3 | Hero actions |
| **Card** | ✅ Used | 95% | - | Excellent |
| **ElevatedCard** | 🟡 Partial | 20% | P2 | Alternative style |
| **OutlinedCard** | ❌ Missing | 0% | P1 | Borders |
| **TextField** | ✅ Used | 70% | - | Forms |
| **OutlinedTextField** | 🟡 Partial | 50% | P1 | Should be primary |
| **SearchBar** | ❌ Missing | 0% | 🔴 P0 | Critical UX |
| **DockedSearchBar** | ❌ Missing | 0% | P1 | Alternative |
| **Switch** | ✅ Used | 90% | - | Excellent |
| **Checkbox** | ✅ Used | 80% | - | Good |
| **RadioButton** | ❌ Missing | 0% | P2 | Single choice |
| **Slider** | ✅ Used | 40% | - | Good |
| **RangeSlider** | ❌ Missing | 0% | P3 | Range selection |
| **Chip** | 🟡 Partial | 10% | P1 | Filters |
| **FilterChip** | 🟡 Partial | 10% | P1 | Filtering |
| **InputChip** | ❌ Missing | 0% | P2 | Tags |
| **AssistChip** | ❌ Missing | 0% | P2 | Suggestions |
| **SuggestionChip** | ❌ Missing | 0% | P3 | Autocomplete |
| **Badge** | ❌ Missing | 0% | P1 | Notification counts |
| **BadgedBox** | ❌ Missing | 0% | P1 | Icon badges |
| **BottomSheet** | ❌ Missing | 0% | 🔴 P0 | Critical pattern |
| **ModalBottomSheet** | ❌ Missing | 0% | 🔴 P0 | Options/filters |
| **BottomSheetScaffold** | ❌ Missing | 0% | P2 | Persistent sheet |
| **NavigationBar** | ❌ Missing | 0% | P1 | Bottom nav |
| **NavigationRail** | ❌ Missing | 0% | P1 | Tablet |
| **NavigationDrawer** | ✅ Used | 100% | - | Excellent |
| **ModalDrawer** | ✅ Used | 100% | - | Excellent |
| **TopAppBar** | ✅ Used | 90% | - | Good |
| **CenterAlignedTopAppBar** | ❌ Missing | 0% | P3 | Alternative |
| **LargeTopAppBar** | ❌ Missing | 0% | P2 | Collapsing |
| **MediumTopAppBar** | ❌ Missing | 0% | P2 | Semi-collapsing |
| **SnackBar** | ✅ Used | 70% | - | Good |
| **Dialog** | ✅ Used | 80% | - | AlertDialog |
| **FullScreenDialog** | ❌ Missing | 0% | P2 | Complex forms |
| **DatePicker** | ❌ Missing | 0% | P2 | Date selection |
| **TimePicker** | ❌ Missing | 0% | P2 | Time selection |
| **DateRangePicker** | ❌ Missing | 0% | P3 | Range selection |
| **ProgressIndicator** | ✅ Used | 90% | - | Good |
| **LinearProgressIndicator** | ✅ Used | 80% | - | Good |
| **CircularProgressIndicator** | ✅ Used | 90% | - | Good |
| **Divider** | 🟡 Partial | 60% | P1 | Should use HorizontalDivider |
| **HorizontalDivider** | ❌ Missing | 0% | P1 | New M3 API |
| **VerticalDivider** | ❌ Missing | 0% | P2 | Split layouts |
| **Icon** | ✅ Used | 100% | - | Excellent |
| **IconButton** | ✅ Used | 95% | - | Excellent |
| **IconToggleButton** | ❌ Missing | 0% | P2 | Toggle states |
| **ListItem** | ✅ Used | 90% | - | Excellent |
| **Surface** | ✅ Used | 70% | - | Good |
| **Scaffold** | ✅ Used | 95% | - | Excellent |

**Summary:**
- **Used Well:** 25 components (45%)
- **Partially Used:** 9 components (16%)
- **Not Used:** 22 components (39%)

---

## 3. Critical Missing Components

### 🔴 **P0: SearchBar**

**Current State:** No search functionality in any screen

**Expected Usage:**
- AppsScreen: Search installed apps
- BackupsScreen: Search backup history
- SettingsScreen: Search settings

**Implementation:**
```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    active: Boolean,
    onActiveChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) { ... }
```

**Files Needing It:**
1. AppsScreen.kt (high priority)
2. BackupsScreen.kt (medium)
3. PluginsScreen.kt (medium)

**Effort:** ⏱️ 8 hours per screen

---

### 🔴 **P0: ModalBottomSheet**

**Current State:** All actions use full-screen dialogs

**Expected Usage:**
1. **Backup Options Sheet**
   - Select backup components (APK, data, media)
   - Choose compression level
   - Set encryption options

2. **Filter Sheet**
   - Filter apps by category
   - Sort options
   - Show/hide system apps

3. **Share/Export Sheet**
   - Export to cloud
   - Share via...
   - Export to file

**Implementation:**
```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupOptionsBottomSheet(
    onDismiss: () -> Unit,
    onConfirm: (BackupOptions) -> Unit
) {
    var includeApk by remember { mutableStateOf(true) }
    var includeData by remember { mutableStateOf(true) }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md)
        ) {
            Text(
                "Backup Options",
                style = MaterialTheme.typography.titleLarge
            )
            
            Spacer(modifier = Modifier.height(Spacing.md))
            
            SwitchRow(
                title = "Include APK",
                checked = includeApk,
                onCheckedChange = { includeApk = it }
            )
            
            SwitchRow(
                title = "Include Data",
                checked = includeData,
                onCheckedChange = { includeData = it }
            )
            
            Spacer(modifier = Modifier.height(Spacing.md))
            
            Button(
                onClick = { onConfirm(BackupOptions(includeApk, includeData)) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Start Backup")
            }
        }
    }
}
```

**Files Needing It:**
1. AppsScreen.kt (backup options)
2. BackupsScreen.kt (restore options)
3. AppsScreen.kt (filter sheet)

**Effort:** ⏱️ 6 hours per sheet

---

### 🟠 **P1: Badge Component**

**Current State:** No notification badges anywhere

**Expected Usage:**
- Show unread notification count
- Indicate pending backups
- Show update available

**Implementation:**
```kotlin
@Composable
fun BadgedNavIcon(
    icon: ImageVector,
    badgeCount: Int,
    onClick: () -> Unit
) {
    BadgedBox(
        badge = {
            if (badgeCount > 0) {
                Badge {
                    Text(badgeCount.toString())
                }
            }
        }
    ) {
        IconButton(onClick = onClick) {
            Icon(icon, contentDescription = null)
        }
    }
}
```

**Files Needing It:**
1. Navigation drawer items
2. TopAppBar actions
3. Tab indicators

**Effort:** ⏱️ 4 hours

---

### 🟠 **P1: FilledTonalButton**

**Current State:** Only FilledButton and OutlinedButton used

**Purpose:** Secondary actions that need emphasis but not as strong as FilledButton

**Example Use Cases:**
- "Save Draft" (vs "Publish" primary)
- "Import" (vs "Create New" primary)
- "Retry" (vs "Cancel" secondary)

**Files Needing It:**
- BackupsScreen.kt (Export vs Restore)
- SettingsScreen.kt (Reset vs Save)
- AppsScreen.kt (Select All vs Backup)

**Effort:** ⏱️ 2 hours

---

### 🟠 **P1: NavigationBar/Rail**

**Current State:** Only ModalDrawer for navigation

**Problem:** 
- Drawer is hidden by default
- No persistent navigation
- Not optimized for tablets

**Recommendation:**
- **Phone:** NavigationBar (bottom)
- **Tablet:** NavigationRail (side)

**Implementation:**
```kotlin
@Composable
fun AdaptiveNavigation() {
    val windowSize = calculateWindowSizeClass()
    
    when (windowSize) {
        WindowSizeClass.Compact -> {
            // Phone: Bottom navigation
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, null) },
                    label = { Text("Dashboard") },
                    selected = currentRoute == Screen.Dashboard.route,
                    onClick = { }
                )
                // ... more items
            }
        }
        else -> {
            // Tablet: Side rail
            NavigationRail {
                NavigationRailItem(
                    icon = { Icon(Icons.Default.Home, null) },
                    label = { Text("Dashboard") },
                    selected = currentRoute == Screen.Dashboard.route,
                    onClick = { }
                )
                // ... more items
            }
        }
    }
}
```

**Effort:** ⏱️ 12 hours

---

### 🟡 **P2: Chip Variants**

**Current State:** FilterChip used in AutomationScreen only (10% usage)

**Missing Variants:**
1. **InputChip** - Tags, removable items
2. **AssistChip** - Suggestions, quick actions
3. **SuggestionChip** - Autocomplete

**Expected Usage:**

#### 1. App Tags (InputChip)
```kotlin
FlowRow(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(8.dp)
) {
    tags.forEach { tag ->
        InputChip(
            selected = false,
            onClick = { },
            label = { Text(tag) },
            trailingIcon = {
                IconButton(onClick = { removeTag(tag) }) {
                    Icon(Icons.Default.Close, "Remove")
                }
            }
        )
    }
}
```

#### 2. Quick Filters (FilterChip)
```kotlin
Row(
    horizontalArrangement = Arrangement.spacedBy(8.dp)
) {
    FilterChip(
        selected = showSystemApps,
        onClick = { showSystemApps = !showSystemApps },
        label = { Text("System Apps") },
        leadingIcon = { Icon(Icons.Default.Android, null) }
    )
    
    FilterChip(
        selected = showOnlyInstalled,
        onClick = { showOnlyInstalled = !showOnlyInstalled },
        label = { Text("Installed") }
    )
}
```

#### 3. Backup Suggestions (AssistChip)
```kotlin
Row(
    horizontalArrangement = Arrangement.spacedBy(8.dp)
) {
    AssistChip(
        onClick = { backupAllMedia() },
        label = { Text("Backup All Media") },
        leadingIcon = { Icon(Icons.Default.Image, null) }
    )
    
    AssistChip(
        onClick = { backupRecentlyUpdated() },
        label = { Text("Backup Recently Updated") },
        leadingIcon = { Icon(Icons.Default.Update, null) }
    )
}
```

**Files Needing It:**
1. AppsScreen.kt (filters, tags)
2. BackupsScreen.kt (suggestions)
3. SettingsScreen.kt (quick actions)

**Effort:** ⏱️ 6 hours

---

### 🟡 **P2: DatePicker/TimePicker**

**Current State:** No date/time selection UI

**Expected Usage:**
- Schedule backup time
- Select date range for backup history
- Retention policy dates

**Files Needing It:**
1. AutomationScreen.kt (schedule time)
2. BackupsScreen.kt (filter by date)
3. SettingsScreen.kt (retention policies)

**Effort:** ⏱️ 8 hours

---

### 🟡 **P2: HorizontalDivider (M3 API)**

**Current State:** Using deprecated `Divider()`

**Migration:**
```kotlin
// Before
Divider()

// After
HorizontalDivider(
    thickness = 1.dp,
    color = MaterialTheme.colorScheme.outlineVariant
)
```

**Effort:** ⏱️ 2 hours (global replace)

---

## 4. Advanced Animation Components

### ❌ **Missing: AnimatedContent**

**Current State:** No animated transitions between content states

**Expected Usage:**
```kotlin
AnimatedContent(
    targetState = currentStep,
    transitionSpec = {
        slideInHorizontally { it } + fadeIn() with
        slideOutHorizontally { -it } + fadeOut()
    }
) { step ->
    StepContent(step)
}
```

**Files Needing It:**
1. OnboardingScreen.kt (step transitions)
2. BackupsScreen.kt (detail <-> list)
3. All multi-state screens

**Effort:** ⏱️ 4 hours per screen

---

### ❌ **Missing: SharedElement Transitions**

**Current State:** No hero animations

**Expected Usage:**
- App icon transitions (list → detail)
- Backup card expansion
- Image previews

**Implementation:**
```kotlin
SharedTransitionLayout {
    AnimatedContent(targetState = selectedApp) { app ->
        if (app == null) {
            AppList(onAppClick = { selectedApp = it })
        } else {
            AppDetail(
                app = app,
                sharedContentState = rememberSharedContentState(key = "app-${app.id}")
            )
        }
    }
}
```

**Effort:** ⏱️ 8 hours

---

## 5. Custom Component Library

### ✅ **Existing Custom Components**

**Good Coverage:**
1. ✅ **EnhancedComponents.kt** (7 components with haptics)
2. ✅ **EmptyStates.kt** (8 specialized empty states)
3. ✅ **SkeletonLoading.kt** (4 skeleton variants)
4. ✅ **Microinteractions.kt** (pull-to-refresh, animations)

**Problem:** Not widely used in screens

---

### ❌ **Missing Reusable Components**

#### 1. **StatusBadge**

**Current:** Duplicated in BackupsScreen.kt

**Should Be:**
```kotlin
// StatusBadge.kt
@Composable
fun StatusBadge(
    icon: ImageVector,
    text: String,
    type: BadgeType = BadgeType.Success
) {
    val colors = when (type) {
        BadgeType.Success -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        BadgeType.Warning -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        BadgeType.Error -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        BadgeType.Info -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
    }
    
    Surface(
        color = colors.first,
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.second,
                modifier = Modifier.size(IconSize.small)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = colors.second
            )
        }
    }
}

enum class BadgeType {
    Success, Warning, Error, Info
}
```

**Files Needing It:**
- BackupsScreen.kt (verified, encrypted)
- AppsScreen.kt (system app, updated)
- SettingsScreen.kt (status indicators)

---

#### 2. **InfoCard**

**Current:** Duplicated in multiple screens

**Should Be:**
```kotlin
@Composable
fun InfoCard(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primaryContainer
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = accentColor.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier.padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(IconSize.large)
            )
            Spacer(modifier = Modifier.width(Spacing.md))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
```

---

#### 3. **ProgressCard**

**Current:** GamingBackupScreen has custom implementation

**Should Be Reusable:**
```kotlin
@Composable
fun ProgressCard(
    title: String,
    progress: Float,
    currentItem: String? = null,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            
            Spacer(modifier = Modifier.height(Spacing.xs))
            
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier.fillMaxWidth()
            )
            
            if (currentItem != null) {
                Spacer(modifier = Modifier.height(Spacing.xs))
                Text(
                    text = currentItem,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
```

---

#### 4. **SectionHeader**

**Current:** Text-only headers in SettingsScreen

**Should Be:**
```kotlin
@Composable
fun SectionHeader(
    title: String,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = Spacing.md,
                vertical = Spacing.sm
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(IconSize.medium)
                )
                Spacer(modifier = Modifier.width(Spacing.xs))
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
```

---

#### 5. **FeatureCard**

**Purpose:** Highlight premium/new features

```kotlin
@Composable
fun FeatureCard(
    title: String,
    description: String,
    icon: ImageVector,
    isPremium: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    EnhancedCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(IconSize.xlarge)
            )
            
            Spacer(modifier = Modifier.width(Spacing.md))
            
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (isPremium) {
                        Spacer(modifier = Modifier.width(Spacing.xs))
                        StatusBadge(
                            icon = Icons.Default.Star,
                            text = "PRO",
                            type = BadgeType.Warning
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(Spacing.xxs))
                
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
```

**Files Needing It:**
- SettingsScreen.kt (feature list)
- DashboardScreen.kt (quick actions)
- PluginsScreen.kt (plugin list)

---

## 6. Typography Enhancements

### 🟡 **Custom Font Families**

**Current:** All screens use `FontFamily.Default`

**Recommendation:** Add custom font for brand identity

**Options:**
1. **Roboto Flex** (Variable font, modern)
2. **Inter** (Clean, professional)
3. **Work Sans** (Friendly, approachable)
4. **Manrope** (Geometric, technical)

**Implementation:**
```kotlin
// fonts/Fonts.kt
val RobotoFlex = FontFamily(
    Font(R.font.roboto_flex_regular, FontWeight.Normal),
    Font(R.font.roboto_flex_medium, FontWeight.Medium),
    Font(R.font.roboto_flex_semibold, FontWeight.SemiBold),
    Font(R.font.roboto_flex_bold, FontWeight.Bold)
)

// Update Type.kt
val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = RobotoFlex,  // Instead of FontFamily.Default
        fontWeight = FontWeight.Normal,
        fontSize = 57.sp,
        ...
    ),
    ...
)
```

**Effort:** ⏱️ 4 hours

---

### 🟡 **Monospace for Numbers**

**Current:** Stats use default font

**Recommendation:** Use monospace for better alignment

```kotlin
// Add to Type.kt
val monoTypography = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontWeight = FontWeight.Normal,
    fontSize = 16.sp
)

// Usage in DashboardScreen
Text(
    text = "Size: ${formatSize(totalSize)}",
    style = MaterialTheme.typography.monoTypography
)
```

**Files Needing It:**
- DashboardScreen.kt (stats)
- BackupsScreen.kt (sizes, counts)
- LogsScreen.kt (timestamps)

**Effort:** ⏱️ 2 hours

---

## 7. Color System Enhancements

### 🟡 **Semantic Color Roles**

**Current:** Only primary/secondary/error used

**Recommendation:** Add semantic roles

```kotlin
// Color.kt
object SemanticColors {
    val success = Color(0xFF4CAF50)
    val successContainer = Color(0xFFC8E6C9)
    val onSuccessContainer = Color(0xFF1B5E20)
    
    val warning = Color(0xFFFFA726)
    val warningContainer = Color(0xFFFFE0B2)
    val onWarningContainer = Color(0xFFE65100)
    
    val info = Color(0xFF29B6F6)
    val infoContainer = Color(0xFFB3E5FC)
    val onInfoContainer = Color(0xFF01579B)
}
```

**Usage:**
```kotlin
// Success state
Card(colors = CardDefaults.cardColors(
    containerColor = SemanticColors.successContainer
))

// Warning state
StatusBadge(
    icon = Icons.Default.Warning,
    text = "Low Storage",
    backgroundColor = SemanticColors.warningContainer,
    contentColor = SemanticColors.onWarningContainer
)
```

**Effort:** ⏱️ 4 hours

---

## 8. Layout Enhancements

### ❌ **Missing: Adaptive Layouts**

**Current:** All screens single-column

**Recommendation:** Multi-column for tablets

```kotlin
@Composable
fun AdaptiveLayout(
    content: @Composable BoxScope.() -> Unit
) {
    val windowSize = calculateWindowSizeClass()
    
    when (windowSize) {
        WindowSizeClass.Compact -> {
            // Phone: Single column
            Box(content = content)
        }
        WindowSizeClass.Medium -> {
            // Tablet portrait: 2 columns
            TwoColumnLayout(content = content)
        }
        WindowSizeClass.Expanded -> {
            // Tablet landscape: 3 columns
            ThreeColumnLayout(content = content)
        }
    }
}
```

**Files Needing It:**
- DashboardScreen.kt (grid stats)
- AppsScreen.kt (grid view option)
- CloudProvidersScreen.kt (provider grid)

**Effort:** ⏱️ 12 hours

---

## 9. Accessibility Enhancements

### 🟡 **Missing Focus Indicators**

**Current:** No visible focus indicators

**Recommendation:**
```kotlin
@Composable
fun FocusableCard(
    onClick: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val isFocused = remember { mutableStateOf(false) }
    
    Card(
        onClick = onClick,
        modifier = Modifier
            .focusRequester(focusRequester)
            .onFocusChanged { isFocused.value = it.isFocused }
            .border(
                width = if (isFocused.value) 2.dp else 0.dp,
                color = MaterialTheme.colorScheme.primary,
                shape = MaterialTheme.shapes.medium
            ),
        content = content
    )
}
```

**Effort:** ⏱️ 6 hours

---

### 🟡 **Screen Reader Hints**

**Current:** Limited `contentDescription` usage

**Recommendation:** Add semantic hints everywhere

```kotlin
Button(
    onClick = { },
    modifier = Modifier.semantics {
        contentDescription = "Backup selected apps. ${selectedApps.size} apps selected."
        role = Role.Button
        stateDescription = if (isEnabled) "Enabled" else "Disabled. Select apps first."
    }
) { ... }
```

**Effort:** ⏱️ 8 hours

---

## 10. Performance Optimizations

### 🟡 **Lazy Layout Keys**

**Current:** Many LazyColumn items lack keys

**Problem:** Recomposition inefficiency

**Fix:**
```kotlin
// Before
LazyColumn {
    items(apps) { app ->
        AppItem(app)
    }
}

// After
LazyColumn {
    items(
        items = apps,
        key = { it.appId.value }
    ) { app ->
        AppItem(app)
    }
}
```

**Files Needing It:** All screens with LazyColumn/LazyRow

**Effort:** ⏱️ 2 hours

---

## Summary

### Priority Matrix

| Gap | Priority | Impact | Effort | Status |
|-----|----------|--------|--------|--------|
| **Spacing tokens** | 🔴 P0 | High | 10h | ❌ Missing |
| **Elevation tokens** | 🔴 P0 | High | 5h | ❌ Missing |
| **SearchBar** | 🔴 P0 | High | 8h | ❌ Missing |
| **ModalBottomSheet** | 🔴 P0 | High | 18h | ❌ Missing |
| **HorizontalDivider** | 🟠 P1 | Low | 2h | ❌ Missing |
| **FilledTonalButton** | 🟠 P1 | Medium | 2h | ❌ Missing |
| **Badge** | 🟠 P1 | Medium | 4h | ❌ Missing |
| **NavigationBar/Rail** | 🟠 P1 | High | 12h | ❌ Missing |
| **StatusBadge** | 🟠 P1 | Medium | 3h | ❌ Missing |
| **Chip variants** | 🟡 P2 | Medium | 6h | 🟡 Partial |
| **DatePicker** | 🟡 P2 | Medium | 8h | ❌ Missing |
| **Custom fonts** | 🟡 P2 | Low | 4h | ❌ Missing |
| **Adaptive layouts** | 🟡 P2 | High | 12h | ❌ Missing |
| **IconSize tokens** | 🟡 P2 | Low | 3h | ❌ Missing |
| **SharedElement** | 🟢 P3 | Low | 8h | ❌ Missing |

**Total Estimated Effort:** ~110 hours (3 weeks)

---

## Next Steps

1. ✅ Review audit reports
2. ⏳ Implement Phase 1 (Design tokens)
3. ⏳ Implement Phase 2 (Animations)
4. ⏳ Implement Phase 3 (Component adoption)
5. ⏳ Implement Phase 4 (Missing components)

---

**Conclusion:** ObsidianBackup's design system is **functional but incomplete**. Addressing P0/P1 gaps will significantly improve UX consistency and modernization.
