package com.yawmi.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yawmi.app.YawmiApplication
import com.yawmi.app.ui.onboarding.OnboardingScreen
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.yawmi.app.ui.account.AccountScreen
import com.yawmi.app.ui.common.PhasePlaceholderScreen
import com.yawmi.app.ui.home.HomeScreen
import com.yawmi.app.ui.navigation.Destination
import com.yawmi.app.ui.theme.YawmiTheme

@Composable
fun YawmiApp() {
    val container = (LocalContext.current.applicationContext as YawmiApplication).container
    val onboardingComplete by container.onboardingRepository.isComplete.collectAsStateWithLifecycle(initialValue = null)
    YawmiTheme {
        when (onboardingComplete) {
            null -> Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
            false -> OnboardingScreen()
            true -> MainShell()
        }
    }
}

@Composable
private fun MainShell() {
    run {
        val navController = rememberNavController()
        val backStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = backStackEntry?.destination?.route

        Scaffold(
            bottomBar = {
                NavigationBar {
                    Destination.entries.forEach { dest ->
                        NavigationBarItem(
                            selected = currentRoute == dest.route,
                            onClick = {
                                navController.navigate(dest.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(dest.icon, contentDescription = dest.label) },
                            label = { Text(dest.label) },
                        )
                    }
                }
            },
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Destination.HOME.route,
                modifier = Modifier.padding(innerPadding),
            ) {
                composable(Destination.HOME.route) { HomeScreen() }
                composable(Destination.SCHEDULE.route) {
                    PhasePlaceholderScreen(
                        title = "الجدول الأسبوعي",
                        phase = "المرحلة 3",
                        description = "إدخال الجدول يدوياً، الاستثناءات، كشف التعارضات. الجداول في قاعدة البيانات جاهزة.",
                    )
                }
                composable(Destination.APPOINTMENTS.route) {
                    PhasePlaceholderScreen(
                        title = "المواعيد",
                        phase = "المرحلة 3",
                        description = "تقويم كامل للمواعيد. الإضافة السريعة لموعد اليوم متاحة من الصفحة الرئيسية.",
                    )
                }
                composable(Destination.HOLIDAYS.route) {
                    PhasePlaceholderScreen(
                        title = "العطل",
                        phase = "المرحلة 3",
                        description = "العطل الرسمية والمدرسية والراحة الشخصية، مع تحديث سنوي وتعديل يدوي.",
                    )
                }
                composable(Destination.ACCOUNT.route) { AccountScreen() }
            }
        }
    }
}
