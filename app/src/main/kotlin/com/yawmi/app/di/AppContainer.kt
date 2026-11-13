package com.yawmi.app.di

import android.content.Context
import com.yawmi.app.data.cloud.AuthRepository
import com.yawmi.app.data.cloud.NoOpAuthRepository
import com.yawmi.app.data.cloud.NoOpSyncRepository
import com.yawmi.app.data.cloud.SyncRepository
import com.yawmi.app.data.local.YawmiDatabase
import com.yawmi.app.data.prefs.OnboardingDraftStore
import com.yawmi.app.data.prefs.UserPrefs
import com.yawmi.app.data.repository.AgendaRepository
import com.yawmi.app.data.repository.DefaultDataSeeder
import com.yawmi.app.data.repository.OnboardingRepository
import com.yawmi.app.data.repository.TaskRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * حاوية الاعتماديات (DI يدوي عبر المُنشئات). اختير عمداً بدل Hilt في المرحلة 1 لتقليل مخاطر البناء
 * (لا معالج تعليقات إضافي فوق Room)، ويمكن الانتقال إلى Hilt لاحقاً دون تغيير الطبقات لأنها تستقبل اعتمادياتها في مُنشئاتها.
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database: YawmiDatabase by lazy { YawmiDatabase.build(appContext) }
    val userPrefs: UserPrefs by lazy { UserPrefs(appContext) }

    val agendaRepository: AgendaRepository by lazy {
        AgendaRepository(database.appointmentDao(), database.scheduleItemDao(), database.holidayDao())
    }
    val taskRepository: TaskRepository by lazy { TaskRepository(database.taskDao()) }
    val onboardingRepository: OnboardingRepository by lazy { OnboardingRepository(database) }
    val onboardingDraftStore: OnboardingDraftStore by lazy { OnboardingDraftStore(appContext) }

    val authRepository: AuthRepository = NoOpAuthRepository()
    val syncRepository: SyncRepository = NoOpSyncRepository()

    fun seedDefaults() {
        applicationScope.launch { DefaultDataSeeder(database).seedIfEmpty() }
    }
}
