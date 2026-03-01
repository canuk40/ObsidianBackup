# UX Enhancements Component Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    ObsidianBackup App                            │
│                  (Material You 3.0 Theme)                        │
└─────────────────────────────────────────────────────────────────┘
                              │
                              │
        ┌─────────────────────┴─────────────────────┐
        │                                           │
        ▼                                           ▼
┌───────────────┐                          ┌───────────────┐
│  UI Screens   │                          │  Navigation   │
└───────────────┘                          └───────────────┘
        │                                           │
        │                                           │
        ├── DashboardScreen                        ├── NavigationTransitions
        ├── BackupsScreen                          │   ├── Horizontal Slide
        ├── EnhancedBackupsScreen ✨               │   ├── Fade Through
        ├── AppsScreen                             │   ├── Shared Axis Z
        ├── SettingsScreen                         │   └── Modal Slide
        └── ...                                    │
                                                   └── Predictive Back
                                                       └── Android 14+ Support
        │
        │
        ▼
┌─────────────────────────────────────────────────────────────────┐
│                        Component Layer                           │
└─────────────────────────────────────────────────────────────────┘
        │
        ├───────────────┬───────────────┬──────────────┬──────────────┐
        │               │               │              │              │
        ▼               ▼               ▼              ▼              ▼
┌────────────┐  ┌──────────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐
│ Enhanced   │  │  Animations  │  │ Loading  │  │  Empty   │  │  Micro-  │
│ Components │  │  (Lottie)    │  │  States  │  │  States  │  │interactions│
└────────────┘  └──────────────┘  └──────────┘  └──────────┘  └──────────┘
      │               │                 │             │              │
      │               │                 │             │              │
      ▼               ▼                 ▼             ▼              ▼
                                                               
┌──────────────────────────────────────────────────────────────────┐
│              Enhanced Components Details                          │
├──────────────────────────────────────────────────────────────────┤
│ • EnhancedButton        (scale + haptics)                        │
│ • EnhancedFAB           (breathing + haptics)                    │
│ • EnhancedIconButton    (ripple + haptics)                       │
│ • EnhancedCard          (press animation + elevation)            │
│ • EnhancedSwitch        (haptic on toggle)                       │
│ • EnhancedCheckbox      (haptic on check)                        │
│ • EnhancedSlider        (haptic on change)                       │
└──────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────┐
│                Lottie Animations Details                          │
├──────────────────────────────────────────────────────────────────┤
│ • BackupProgressAnimation    (circular progress)                 │
│ • SuccessAnimation          (checkmark + bounce)                 │
│ • ErrorAnimation            (shake effect)                       │
│ • CloudSyncAnimation        (floating cloud)                     │
│ • EmptyStateAnimation       (illustrations)                      │
│ • LoadingAnimation          (spinner)                            │
│ • PullToRefreshAnimation    (pull indicator)                     │
└──────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────┐
│                  Loading States Details                           │
├──────────────────────────────────────────────────────────────────┤
│ • SkeletonBox              (shimmer effect base)                 │
│ • AppItemSkeleton          (app list placeholder)                │
│ • BackupCardSkeleton       (backup card placeholder)             │
│ • DashboardStatsSkeleton   (stats placeholder)                   │
│ • AppsScreenSkeleton       (full screen placeholder)             │
│ • BackupsScreenSkeleton    (full screen placeholder)             │
│ • SkeletonList             (generic list placeholder)            │
└──────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────┐
│                   Empty States Details                            │
├──────────────────────────────────────────────────────────────────┤
│ • EmptyState                     (generic base)                  │
│ • NoBackupsEmptyState           (with CTA)                       │
│ • NoAppsSelectedEmptyState      (guidance)                       │
│ • NoSearchResultsEmptyState     (with query)                     │
│ • NoLogsEmptyState              (info)                           │
│ • CloudNotConnectedEmptyState   (with action)                    │
│ • NoAutomationRulesEmptyState   (with CTA)                       │
│ • ErrorState                    (with retry)                     │
└──────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────┐
│                 Microinteractions Details                         │
├──────────────────────────────────────────────────────────────────┤
│ • PullToRefresh                 (swipe down gesture)             │
│ • AnimatedCircularProgressIndicator (smooth progress)            │
│ • AnimatedSuccessCheckmark      (draw animation)                 │
│ • PulsatingBadge                (breathing badge)                │
│ • BreathingEffect               (subtle scale)                   │
└──────────────────────────────────────────────────────────────────┘

        │
        │
        ▼
┌─────────────────────────────────────────────────────────────────┐
│                         Utility Layer                            │
└─────────────────────────────────────────────────────────────────┘
        │
        ├───────────────┬───────────────┬──────────────┐
        │               │               │              │
        ▼               ▼               ▼              ▼
┌────────────┐  ┌──────────────┐  ┌──────────┐  ┌──────────┐
│  Haptic    │  │  Animation   │  │Predictive│  │ Material │
│  Feedback  │  │    Specs     │  │   Back   │  │  You 3.0 │
└────────────┘  └──────────────┘  └──────────┘  └──────────┘

┌──────────────────────────────────────────────────────────────────┐
│                  Haptic Feedback Details                          │
├──────────────────────────────────────────────────────────────────┤
│ • light()         - Button presses, selections                   │
│ • medium()        - Important actions, toggles                   │
│ • heavy()         - Critical actions, deletions                  │
│ • success()       - Double tap pattern (operation success)       │
│ • error()         - Sharp pulses (operation failed)              │
│ • longPress()     - Long press confirmation                      │
│ • gestureStart()  - Gesture initiated (Android 10+)              │
│ • gestureEnd()    - Gesture completed (Android 10+)              │
└──────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────┐
│                  Animation Specs Details                          │
├──────────────────────────────────────────────────────────────────┤
│ Durations:                                                       │
│ • FAST: 150ms        - Quick interactions                        │
│ • NORMAL: 300ms      - Standard transitions                      │
│ • SLOW: 500ms        - Emphasized animations                     │
│ • VERY_SLOW: 700ms   - Special effects                           │
│                                                                  │
│ Easing Curves:                                                   │
│ • StandardEasing     - Default (cubic bezier)                    │
│ • EmphasizedEasing   - Important transitions                     │
│ • EaseIn/Out/InOut   - Standard curves                           │
│                                                                  │
│ Springs:                                                         │
│ • FastSpring         - Quick bouncy                              │
│ • MediumSpring       - Balanced                                  │
│ • SlowSpring         - Smooth no bounce                          │
│                                                                  │
│ Special:                                                         │
│ • shakeSpec          - Error animation                           │
│ • bounceSpec         - Success animation                         │
│ • pulseSpec          - Infinite breathing                        │
└──────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────┐
│                        Data Flow                                  │
└──────────────────────────────────────────────────────────────────┘

User Action
    │
    ▼
Enhanced Component (Button/Card/etc)
    │
    ├──► Haptic Feedback (immediate tactile response)
    │
    ├──► Animation (visual feedback)
    │    ├── Scale animation
    │    ├── Elevation change
    │    └── Color transition
    │
    ▼
Action Executed
    │
    ├──► Loading State (skeleton)
    │
    ├──► Lottie Animation (progress/success/error)
    │
    ▼
Result
    ├──► Success: SuccessAnimation + haptic.success()
    ├──► Error: ErrorAnimation + haptic.error()
    └──► Empty: EmptyState with guidance

┌──────────────────────────────────────────────────────────────────┐
│                    Integration Pattern                            │
└──────────────────────────────────────────────────────────────────┘

1. Screen Loads
   └──► Show Skeleton Loading
   
2. Data Fetched
   └──► Animate transition to content
   
3. User Interacts
   ├──► Haptic feedback on touch
   ├──► Visual animation
   └──► Execute action
   
4. Action Processing
   └──► Show Lottie animation (progress)
   
5. Action Complete
   ├──► Success: SuccessAnimation + haptic.success()
   ├──► Error: ErrorAnimation + haptic.error()
   └──► Navigate with smooth transition

6. Empty State Handling
   └──► Show relevant EmptyState with CTA

┌──────────────────────────────────────────────────────────────────┐
│                  Material You 3.0 Layer                           │
└──────────────────────────────────────────────────────────────────┘

┌────────────────┐     ┌────────────────┐     ┌────────────────┐
│  Color Scheme  │────▶│     Shapes     │────▶│   Typography   │
│ (Dynamic/Static)│     │  (Rounded)     │     │  (Roboto)      │
└────────────────┘     └────────────────┘     └────────────────┘
       │                       │                       │
       └───────────────────────┴───────────────────────┘
                               │
                               ▼
                    ┌─────────────────────┐
                    │  Theme Applied to   │
                    │  All Components     │
                    └─────────────────────┘

Shape Specifications:
├── extraSmall: 4dp   (chips, badges)
├── small: 8dp        (buttons)
├── medium: 12dp      (cards)
├── large: 16dp       (FAB, dialogs)
└── extraLarge: 28dp  (bottom sheets)

┌──────────────────────────────────────────────────────────────────┐
│                         Dependencies                              │
└──────────────────────────────────────────────────────────────────┘

Material 3 + Compose
         │
         ├──► Lottie Compose (6.3.0)
         │    └──► Animation rendering
         │
         ├──► Accompanist (0.32.0)
         │    ├──► System UI Controller
         │    └──► Navigation Animation
         │
         └──► Android System
              ├──► Vibrator (haptics)
              ├──► OnBackInvokedDispatcher (predictive back)
              └──► Dynamic Color (Android 12+)

┌──────────────────────────────────────────────────────────────────┐
│                  Performance Considerations                       │
└──────────────────────────────────────────────────────────────────┘

• Lazy loading of animations
• Efficient skeleton rendering (shimmer as composable)
• Minimal recomposition (remember/LaunchedEffect)
• Hardware acceleration for animations
• Conditional haptics (respect system settings)
• Optimized Lottie JSON (simplified inline)

┌──────────────────────────────────────────────────────────────────┐
│                    Accessibility Support                          │
└──────────────────────────────────────────────────────────────────┘

• Respects reduced motion preferences
• Haptic feedback optional (system settings)
• TalkBack compatible
• High contrast support
• Content descriptions for all icons
• Semantic structure maintained
```

## Key Relationships

1. **Screens** use **Enhanced Components**
2. **Enhanced Components** use **Haptic Feedback** + **Animations**
3. **Loading States** transition to **Content** or **Empty States**
4. **Navigation** uses **Transitions** for screen changes
5. **All Components** follow **Material You 3.0** theme

## Component Reusability

```
Base Component (Material 3)
        ↓
Enhanced Component (+ animations + haptics)
        ↓
Screen Implementation
```

## Example Flow

```
User taps "Backup" button
    ↓
EnhancedButton triggers
    ├─► haptic.medium()
    ├─► Scale animation (0.95x)
    └─► onClick callback
        ↓
BackupScreen shows
    ├─► BackupProgressAnimation
    └─► AnimatedCircularProgressIndicator
        ↓
Backup completes
    ├─► SuccessAnimation
    ├─► haptic.success()
    └─► Navigate with transition
```
