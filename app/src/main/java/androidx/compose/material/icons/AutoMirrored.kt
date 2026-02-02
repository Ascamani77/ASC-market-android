@file:Suppress("unused", "DEPRECATION")
package androidx.compose.material.icons

import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector

// Holder object exposing Filled and Outlined icon accessors that mirror the existing Icons.
object AutoMirrored {
    object Filled {
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
        val Person: ImageVector get() = Icons.Filled.Person
        val Edit: ImageVector get() = Icons.Filled.Edit
        val Verified: ImageVector get() = Icons.Filled.Verified
        val Shield: ImageVector get() = Icons.Filled.Shield
        val Mail: ImageVector get() = Icons.Filled.Mail
        val Phone: ImageVector get() = Icons.Filled.Phone
        val Business: ImageVector get() = Icons.Filled.Business
        val Language: ImageVector get() = Icons.Filled.Language
        val Logout: ImageVector get() = Icons.Filled.Logout
    }

    object Outlined {
        val Settings: ImageVector get() = Icons.Outlined.Settings
        val Timeline: ImageVector get() = Icons.Outlined.Timeline
        val FilterList: ImageVector get() = Icons.Outlined.FilterList
        val Lock: ImageVector get() = Icons.Outlined.Lock
        val AccountBalance: ImageVector get() = Icons.Outlined.AccountBalance
        val Notifications: ImageVector get() = Icons.Outlined.Notifications
        val List: ImageVector get() = Icons.Outlined.List
        val Tune: ImageVector get() = Icons.Outlined.Tune
        val Memory: ImageVector get() = Icons.Outlined.Memory
        val ArrowBack: ImageVector get() = Icons.Outlined.ArrowBack
        val Download: ImageVector get() = Icons.Outlined.Download
        val Delete: ImageVector get() = Icons.Outlined.Delete
        val Save: ImageVector get() = Icons.Outlined.Save
        val Info: ImageVector get() = Icons.Outlined.Info
        val ChevronRight: ImageVector get() = Icons.Outlined.ChevronRight
        val Search: ImageVector get() = Icons.Outlined.Search
        val Star: ImageVector get() = Icons.Outlined.Star
        val Visibility: ImageVector get() = Icons.Outlined.Visibility
        val ArrowUpward: ImageVector get() = Icons.Outlined.ArrowUpward
        val ArrowDownward: ImageVector get() = Icons.Outlined.ArrowDownward
        val TrendingUp: ImageVector get() = Icons.Outlined.ShowChart
        val TrendingDown: ImageVector get() = Icons.Outlined.ShowChart
        val ShowChart: ImageVector get() = Icons.Outlined.ShowChart
        val Person: ImageVector get() = Icons.Outlined.Person
        val Edit: ImageVector get() = Icons.Outlined.Edit
        val Verified: ImageVector get() = Icons.Outlined.Verified
        val Shield: ImageVector get() = Icons.Outlined.Shield
        val Mail: ImageVector get() = Icons.Outlined.Mail
        val Phone: ImageVector get() = Icons.Outlined.Phone
        val Business: ImageVector get() = Icons.Outlined.Business
        val Language: ImageVector get() = Icons.Outlined.Language
        val Logout: ImageVector get() = Icons.Outlined.Logout
    }
}

// Extension property so you can write Icons.AutoMirrored
val Icons.AutoMirrored: AutoMirrored get() = AutoMirrored
