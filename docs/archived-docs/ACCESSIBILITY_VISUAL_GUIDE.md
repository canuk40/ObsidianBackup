# Visual Accessibility Features Guide

## Overview

This guide provides visual examples and descriptions of accessibility features in ObsidianBackup.

## 🎨 High Contrast Modes

### Standard Theme (Light)
```
┌─────────────────────────────────────┐
│ ObsidianBackup         [SAF]        │ ← Top Bar
├─────────────────────────────────────┤
│                                     │
│  ┌─────────────────────────────┐  │
│  │ Quick Stats                 │  │ ← Card
│  │                             │  │
│  │ Total Backups: 5            │  │
│  │ Last Backup: Today          │  │
│  │ Total Size: 2.5 GB          │  │
│  └─────────────────────────────┘  │
│                                     │
│  [  Backup Apps  ] [ Restore  ]    │ ← Buttons
│                                     │
└─────────────────────────────────────┘
│ 🏠  📱  💾  ⏰  📝  ⚙️ │ ← Nav Bar
└─────────────────────────────────────┘

Colors:
- Background: #FFFFFF (white)
- Primary: #4CAF50 (green)
- Text: #000000 (black)
- Contrast Ratio: 4.5:1 (WCAG AA)
```

### High Contrast Theme (Light)
```
┌─────────────────────────────────────┐
│ ObsidianBackup         [SAF]        │
├─────────────────────────────────────┤
│                                     │
│  ┌─────────────────────────────┐  │
│  │ Quick Stats                 │  │
│  │                             │  │
│  │ Total Backups: 5            │  │
│  │ Last Backup: Today          │  │
│  │ Total Size: 2.5 GB          │  │
│  └─────────────────────────────┘  │
│                                     │
│  [  Backup Apps  ] [ Restore  ]    │
│                                     │
└─────────────────────────────────────┘
│ 🏠  📱  💾  ⏰  📝  ⚙️ │
└─────────────────────────────────────┘

Colors:
- Background: #FFFFFF (pure white)
- Primary: #007700 (dark green)
- Text: #000000 (pure black)
- Contrast Ratio: 7.2:1 (WCAG AAA) ⭐
```

### High Contrast Theme (Dark)
```
┌─────────────────────────────────────┐
│ ObsidianBackup         [SAF]        │
├─────────────────────────────────────┤
│                                     │
│  ┌─────────────────────────────┐  │
│  │ Quick Stats                 │  │
│  │                             │  │
│  │ Total Backups: 5            │  │
│  │ Last Backup: Today          │  │
│  │ Total Size: 2.5 GB          │  │
│  └─────────────────────────────┘  │
│                                     │
│  [  Backup Apps  ] [ Restore  ]    │
│                                     │
└─────────────────────────────────────┘
│ 🏠  📱  💾  ⏰  📝  ⚙️ │
└─────────────────────────────────────┘

Colors:
- Background: #000000 (pure black)
- Primary: #00FF00 (bright green)
- Text: #FFFFFF (pure white)
- Contrast Ratio: 15.3:1 (WCAG AAA) ⭐⭐
```

## 📏 Touch Target Sizes

### Standard Buttons (48x48 dp minimum)
```
┌──────────────────────────┐
│                          │
│   ☁  Backup Apps         │  48dp height
│                          │  (WCAG 2.2 Level AAA)
└──────────────────────────┘
          ↕
       48 dp
```

### Simplified Mode Buttons (80dp height)
```
┌──────────────────────────┐
│                          │
│                          │
│   ☁  Backup Now          │  80dp height
│                          │  (50% larger)
│                          │
└──────────────────────────┘
          ↕
       80 dp
```

### Checkbox Touch Targets
```
Standard:              Accessible:
┌─────┐               ┌─────────────┐
│  ☑  │  24x24 dp     │      ☑      │  48x48 dp
└─────┘               └─────────────┘
```

## 🔊 Screen Reader Announcements

### App Selection Flow
```
User Action              TalkBack Announces
───────────────────────────────────────────
Tap checkbox        →   "Select application"
Select app          →   "Example App 1 selected"
Deselect app        →   "Example App 1 deselected"
Tap backup button   →   "Backup 3 applications"
Start backup        →   "Backup started"
Complete backup     →   "Backup completed successfully"
```

### Navigation Flow
```
User Action              TalkBack Announces
───────────────────────────────────────────
Tap Dashboard       →   "Navigate to Dashboard"
Tap Apps            →   "Navigate to Apps screen"
Tap Backups         →   "Navigate to Backups screen"
Tap Settings        →   "Navigate to Settings screen"
```

## 🎤 Voice Commands

### Command Recognition Flow
```
User Says               System Response
────────────────────────────────────────────
"Backup my apps"    →  Opens backup dialog
"Restore my apps"   →  Opens restore dialog
"Backup status"     →  Shows progress screen
"Open settings"     →  Navigates to settings
"Cancel"            →  Cancels operation
"Help"              →  Shows help dialog
```

### Visual Feedback
```
┌─────────────────────────────────────┐
│          🎤 Listening...            │
│                                     │
│    Say a command like:              │
│    • "Backup my apps"               │
│    • "Restore my apps"              │
│    • "Backup status"                │
│                                     │
│         [Cancel Listening]          │
└─────────────────────────────────────┘
```

## 👴 Simplified Mode

### Simplified Home Screen
```
┌─────────────────────────────────────┐
│                                     │
│      Simple Backup                  │  32sp
│                                     │
├─────────────────────────────────────┤
│                                     │
│  ┌─────────────────────────────┐  │
│  │                             │  │
│  │  Welcome! Tap a button to   │  │  24sp
│  │  get started.               │  │
│  │                             │  │
│  └─────────────────────────────┘  │
│                                     │
├─────────────────────────────────────┤
│                                     │
│  ┌─────────────────────────────┐  │
│  │                             │  │
│  │   ☁  Backup Now             │  │  80dp
│  │                             │  │  height
│  └─────────────────────────────┘  │
│                                     │
│  ┌─────────────────────────────┐  │
│  │                             │  │
│  │   ↺  Restore                │  │  80dp
│  │                             │  │  height
│  └─────────────────────────────┘  │
│                                     │
│  ┌─────────────────────────────┐  │
│  │                             │  │
│  │   📋 View Backups           │  │  80dp
│  │                             │  │  height
│  └─────────────────────────────┘  │
│                                     │
└─────────────────────────────────────┘

Features:
- Extra large buttons (80dp)
- Large text (28-32sp)
- Clear spacing (24dp)
- Simple language
- 3 main actions only
```

## 🎯 Focus Indicators

### Keyboard Navigation
```
No Focus:                 With Focus:
┌──────────────┐         ┌══════════════┐
│ Backup Apps  │         ║ Backup Apps  ║  3px blue border
└──────────────┘         ╚══════════════╝  3:1 contrast ratio

Tab Order:
1. Dashboard → 2. Apps → 3. Backups → 4. Settings
   ↓              ↓          ↓            ↓
   [Home]      [Apps]    [Backups]    [Settings]
```

## 📱 App List Accessibility

### Standard App List
```
┌─────────────────────────────────────┐
│ Select apps to backup               │ ← Heading
│ 2 of 10 selected                    │
├─────────────────────────────────────┤
│ 📱 Example App 1              ☐    │ ← 48dp height
│    com.example.app1 • v1.0.0        │   checkbox
│    Data: 50 MB • APK: 10 MB         │
├─────────────────────────────────────┤
│ 📱 Example App 2              ☑    │
│    com.example.app2 • v2.0.0        │
│    Data: 100 MB • APK: 20 MB        │
├─────────────────────────────────────┤

TalkBack reads:
"Example App 1, not selected, checkbox"
"Example App 2, selected, checkbox"
```

## 🌈 Color Contrast Examples

### Text Contrast
```
Standard Theme:
━━━━━━━━━━━━━━━━━━━━━━━━━
█ Normal Text #000000   █  Background: #FFFFFF
█ on White              █  Ratio: 21:1 ✅
━━━━━━━━━━━━━━━━━━━━━━━━━

High Contrast Dark:
━━━━━━━━━━━━━━━━━━━━━━━━━
█ Bright Text #00FF00   █  Background: #000000
█ on Black              █  Ratio: 15.3:1 ⭐
━━━━━━━━━━━━━━━━━━━━━━━━━
```

### UI Component Contrast
```
Button Background:
┌────────────────┐
│   #4CAF50      │  Text: #FFFFFF
│   Action       │  Ratio: 4.6:1 ✅
└────────────────┘

High Contrast:
┌────────────────┐
│   #007700      │  Text: #FFFFFF
│   Action       │  Ratio: 7.2:1 ⭐
└────────────────┘
```

## 📊 Capability Status Display

### Visual + Semantic
```
Permission Status
─────────────────
Using: ROOT

Capabilities:
✅ Backup APK      ← Icon + text (not color alone)
✅ Backup Data     
✅ Incremental     
❌ Restore SELinux 

TalkBack reads:
"Capability enabled: Backup APK"
"Capability enabled: Backup Data"
"Capability disabled: Restore SELinux"
```

## 🔄 Loading States

### Standard Loading
```
┌─────────────────────────┐
│   Processing...         │
│   ⌛                     │
│   Please wait           │
└─────────────────────────┘

TalkBack: "Processing. Please wait."
```

### Simplified Mode Loading
```
┌─────────────────────────────────────┐
│                                     │
│                                     │
│           ⌛  80dp                  │
│                                     │
│       Backing up your apps...       │  32sp
│                                     │
│                                     │
└─────────────────────────────────────┘

TalkBack: "Backing up your apps. Please wait."
```

## ⌨️ Keyboard Shortcuts

### Navigation
```
Key                Action
──────────────────────────────
Tab             →  Next element
Shift+Tab       →  Previous element
Enter           →  Activate button
Space           →  Toggle checkbox
Arrow Keys      →  Navigate lists
Esc             →  Close dialog
```

## 📋 Accessibility Testing Overlay

### Accessibility Scanner Results
```
┌─────────────────────────────────────┐
│ Accessibility Scanner Results       │
├─────────────────────────────────────┤
│                                     │
│ ✅ No issues found                  │
│                                     │
│ Touch Targets:    ✅ All 48dp+      │
│ Contrast:         ✅ All 4.5:1+     │
│ Content Desc:     ✅ All present    │
│ Focus Order:      ✅ Logical        │
│                                     │
└─────────────────────────────────────┘
```

## 🎨 Theme Comparison

### Color Palette Comparison
```
                Standard    High Contrast
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Primary         #4CAF50     #007700 (L)
                            #00FF00 (D)
                            
Background      #FFFFFF     #FFFFFF (L)
                #121212     #000000 (D)

Text            #000000     #000000 (L)
                #E0E0E0     #FFFFFF (D)

Contrast        4.5:1       7.2:1 (L)
                            15.3:1 (D)
```

## 📖 Legend

```
Icons:
─────
✅  Feature enabled / Test passed
❌  Feature disabled / Test failed
⭐  WCAG AAA compliance
📱  Application
☁  Cloud/Backup
⚙️  Settings
🎤  Voice control
👴  Simplified mode
```

---

**Note:** All measurements are in density-independent pixels (dp) to ensure consistent sizing across devices.

For implementation details, see:
- `ACCESSIBILITY_GUIDE.md` - Complete guide
- `ACCESSIBILITY_QUICK_REFERENCE.md` - Code examples
- `ACCESSIBILITY_IMPLEMENTATION_SUMMARY.md` - Technical details
