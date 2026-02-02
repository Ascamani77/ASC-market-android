@file:Suppress("unused", "DEPRECATION")
// Compatibility shim: provide androidx.compose.material.icons.autoMirrored.filled.* symbols
// so older/newer Compose versions referencing Icons.AutoMirrored.Filled.* resolve to the existing Icons.Filled.*
package androidx.compose.material.icons.autoMirrored.filled

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

// Map commonly used icon names to the originals under Icons.Filled
val Settings: ImageVector get() = Icons.Filled.Settings
val Timeline: ImageVector get() = Icons.Filled.Timeline
val FilterList: ImageVector get() = Icons.Filled.FilterList
val Lock: ImageVector get() = Icons.Filled.Lock
val AccountBalance: ImageVector get() = Icons.Filled.AccountBalance
val Notifications: ImageVector get() = Icons.Filled.Notifications
val List: ImageVector get() = Icons.Filled.List
val Tune: ImageVector get() = Icons.Filled.Tune
val Memory: ImageVector get() = Icons.Filled.Memory
val ArrowBack: ImageVector get() = Icons.Filled.ArrowBack
val Download: ImageVector get() = Icons.Filled.Download
val Delete: ImageVector get() = Icons.Filled.Delete
val Save: ImageVector get() = Icons.Filled.Save
val Info: ImageVector get() = Icons.Filled.Info
val ChevronRight: ImageVector get() = Icons.Filled.ChevronRight
val Search: ImageVector get() = Icons.Filled.Search
val Star: ImageVector get() = Icons.Filled.Star
val Visibility: ImageVector get() = Icons.Filled.Visibility
val ArrowUpward: ImageVector get() = Icons.Filled.ArrowUpward
val ArrowDownward: ImageVector get() = Icons.Filled.ArrowDownward
val TrendingUp: ImageVector get() = Icons.Filled.ShowChart
val TrendingDown: ImageVector get() = Icons.Filled.ShowChart
val ShowChart: ImageVector get() = Icons.Filled.ShowChart
val ExpandLess: ImageVector get() = Icons.Filled.ExpandLess
val ExpandMore: ImageVector get() = Icons.Filled.ExpandMore
// Add more mappings as needed
