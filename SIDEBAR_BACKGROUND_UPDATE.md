# Sidebar Background Color Update

## Changes Made

Updated the sidebar to use the same background color as the homepage cards for a consistent design across the app.

### Color Applied
- **Background**: `Color.White.copy(alpha = 0.035f)` - Semi-transparent white (3.5% opacity)
- **Border**: `Color.White.copy(alpha = 0.06f)` - Semi-transparent white (6% opacity)

This creates a subtle dark gray appearance on the black background, matching the "WAITING", "NO OPEN TRADE" cards in the homepage.

### Components Updated

1. **Header Section**
   - Changed from `DeepBlack` to `Color.White.copy(alpha = 0.035f)`
   - Contains: Avatar, Name, Offline status, Search, Settings icons

2. **QuickAccessCard**
   - Background: `DeepBlack` → `Color.White.copy(alpha = 0.035f)`
   - Border: `HairlineBorder` → `Color.White.copy(alpha = 0.06f)`
   - Used for: Post-Move Recon, AI Intel, Trade Dashboard, Event Calendar, etc.

3. **MenuGroupContainer**
   - Background: `DeepBlack` → `Color.White.copy(alpha = 0.035f)`
   - Border: `HairlineBorder` → `Color.White.copy(alpha = 0.06f)`
   - Contains all menu sections: TRACK, LIVE MARKETS, MARKET INTELLIGENCE, etc.

### Overall Background
- Maintained as `PureBlack` (pure black #000000)
- Only the info boxes/cards have the semi-transparent white background

## Visual Result
- Consistent card appearance across homepage and sidebar
- Subtle elevation effect with the semi-transparent backgrounds
- Clean, modern look with proper visual hierarchy
- Better distinction between different UI sections

## Files Modified
- `app/src/main/java/com/asc/markets/ui/components/Sidebar.kt`
