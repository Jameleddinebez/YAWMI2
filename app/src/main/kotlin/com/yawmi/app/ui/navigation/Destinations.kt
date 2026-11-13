package com.yawmi.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.CalendarViewWeek
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Home
import androidx.compose.ui.graphics.vector.ImageVector

/** أقسام شريط التنقل السفلي الخمسة (بترتيب RTL: الرئيسية أقصى اليمين). */
enum class Destination(val route: String, val label: String, val icon: ImageVector) {
    HOME("home", "الرئيسية", Icons.Filled.Home),
    SCHEDULE("schedule", "الجدول", Icons.Filled.CalendarViewWeek),
    APPOINTMENTS("appointments", "المواعيد", Icons.Filled.Event),
    HOLIDAYS("holidays", "العطل", Icons.Filled.BeachAccess),
    ACCOUNT("account", "حسابي", Icons.Filled.AccountCircle),
}
