# Quick Access Customization Feature

## Overview
Implemented a fully customizable Quick Access section in the sidebar that allows users to add/remove items via long-press gestures.

## Features

### 1. **Dynamic Quick Access Section**
- Quick Access items are now stored and managed dynamically
- Items persist across app sessions using SharedPreferences
- Section remains visible even when empty (shows helpful empty state)

### 2. **Long Press on Quick Access Items**
- **Action**: Long press any item in Quick Access
- **Result**: Shows dialog asking "Remove from Quick Access?"
- **Options**: 
  - "Remove" (red text) - Removes item from Quick Access
  - "Cancel" - Dismisses dialog
- Item is removed but can be added back later

### 3. **Long Press on Menu Items**
- **Action**: Long press any item in TRACK, LIVE MARKETS, MARKET INTELLIGENCE, etc. sections
- **Result**: Shows dialog with context-aware options:
  - If NOT in Quick Access: "Add to Quick Access?"
  - If ALREADY in Quick Access: "Remove from Quick Access?"
- **Visual Indicator**: Items already in Quick Access show an orange star (⭐) icon

### 4. **Empty State**
- When Quick Access is empty, shows:
  - Touch icon
  - "No Quick Access items" message
  - "Long press any menu item below to add" hint

### 5. **Default Items**
The following items are in Quick Access by default:
1. Post-Move Recon
2. AI Intel
3. Trade Dashboard
4. Event Calendar
5. Vigilance Setup
6. Event Stream
7. AI Simulation
8. My Simulation
9. AI Terminal
10. AI Sentiment

## Technical Implementation

### Files Created
1. **QuickAccessManager.kt**
   - Manages Quick Access state using StateFlow
   - Persists items to SharedPreferences
   - Provides add/remove functionality
   - Tracks which items are in Quick Access

### Files Modified
1. **Sidebar.kt**
   - Added `combinedClickable` for long-press support
   - Integrated QuickAccessManager
   - Added dialogs for add/remove confirmation
   - Dynamic rendering of Quick Access items
   - Visual indicators (star icon) for items in Quick Access

## User Experience

### Adding Items to Quick Access
1. Scroll to any menu section (TRACK, LIVE MARKETS, etc.)
2. Long press on any menu item
3. Dialog appears: "Add to Quick Access?"
4. Tap "Add" to confirm
5. Item appears in Quick Access section at the top

### Removing Items from Quick Access
**Method 1: From Quick Access Section**
1. Long press on any Quick Access item
2. Dialog appears: "Remove from Quick Access?"
3. Tap "Remove" to confirm
4. Item is removed from Quick Access (but still available in menu sections)

**Method 2: From Menu Sections**
1. Long press on any menu item that has a star (⭐) icon
2. Dialog appears: "Remove from Quick Access?"
3. Tap "Remove" to confirm
4. Item is removed from Quick Access

## Benefits
1. **Personalization**: Users can customize their Quick Access to match their workflow
2. **Efficiency**: Frequently used features are always one tap away
3. **Flexibility**: Easy to add/remove items as needs change
4. **Persistence**: Customizations are saved and restored on app restart
5. **Discoverability**: Visual indicators and helpful empty state guide users
6. **Non-destructive**: Removing from Quick Access doesn't delete the item from the app

## Data Persistence
- Quick Access preferences are stored in SharedPreferences
- Key: `quick_access_prefs`
- Stored as: Set of item IDs
- Automatically loaded on app start
- Survives app restarts and updates
