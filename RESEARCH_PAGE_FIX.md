# Research Page (Analysis & Opinion) - Navbar Removal

## Changes Made

### 1. Removed Category Navbar
- **Before**: Had tabs for "All", "Central Banks", "Energy & Commodities", etc.
- **After**: No category tabs - all articles shown together in one unified feed

### 2. Simplified Article Display Logic
- Removed `activeCategory` state variable
- Removed category filtering logic
- All articles now display together, sorted by publish date (newest first)

### 3. Removed Category Counts
- Removed `categoryCounts` calculation (no longer needed)
- Removed LazyRow with category tabs

### 4. What Remains
- ✅ Search functionality
- ✅ Refresh button
- ✅ Bookmarks
- ✅ Article filtering (removes calendar/upcoming events)
- ✅ All articles from all categories shown together

## Code Changes

### File: `app/src/main/java/com/researchcenter/ui/screens/MainScreen.kt`

1. **Removed category state**:
   ```kotlin
   // REMOVED: var activeCategory by remember { mutableStateOf("all") }
   ```

2. **Simplified displayArticles**:
   ```kotlin
   // Now always shows ALL articles (no category filtering)
   val baseList = articles
   ```

3. **Removed category navbar**:
   ```kotlin
   // REMOVED: LazyRow with category tabs
   // Now just: // Category navbar removed - all articles shown together
   ```

## Result
- Clean, unified feed showing all research articles
- No category tabs to navigate
- Everything together, sorted by date
- Faster navigation and simpler UX
