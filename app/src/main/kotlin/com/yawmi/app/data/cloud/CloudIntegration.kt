package com.yawmi.app.data.cloud

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * طبقة التكامل السحابي (واجهات فقط في المرحلة 1).
 *
 * لا توجد أي تبعية لـ Firebase ولا أي مفاتيح في هذا المشروع حتى الآن، عن قصد:
 * إضافة إضافة google-services بدون ملف google-services.json تكسر البناء، ولا نريد مفاتيح وهمية.
 * في المرحلة 6 تُضاف تبعيات Firebase وتُنفَّذ هذه الواجهات (انظر docs/FIREBASE_SETUP.md).
 */
sealed interface CloudStatus {
    /** لم يُربط Firebase بعد (الحالة الحالية). */
    data object NotConfigured : CloudStatus
    data object SignedOut : CloudStatus
    data class SignedIn(val uid: String, val email: String?) : CloudStatus
}

interface AuthRepository {
    val status: Flow<CloudStatus>
    suspend fun signUp(email: String, password: String): Result<Unit>
    suspend fun signIn(email: String, password: String): Result<Unit>
    suspend fun signOut()
}

/** مزامنة سجلات التغيير (Outbox) مع Firestore — تنفَّذ في المرحلة 6. */
interface SyncRepository {
    suspend fun syncNow(): Result<Unit>
}

class NotConfiguredException : IllegalStateException("خدمات Firebase غير مُعدّة بعد")

/** التنفيذ الافتراضي الصادق: يقول إن السحابة غير مُعدّة بدل أن يتظاهر بالاتصال. */
class NoOpAuthRepository : AuthRepository {
    private val _status = MutableStateFlow<CloudStatus>(CloudStatus.NotConfigured)
    override val status: Flow<CloudStatus> = _status.asStateFlow()
    override suspend fun signUp(email: String, password: String) = Result.failure<Unit>(NotConfiguredException())
    override suspend fun signIn(email: String, password: String) = Result.failure<Unit>(NotConfiguredException())
    override suspend fun signOut() = Unit
}

class NoOpSyncRepository : SyncRepository {
    override suspend fun syncNow() = Result.failure<Unit>(NotConfiguredException())
}
