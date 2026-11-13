package com.yawmi.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.yawmi.app.data.local.entity.AppointmentEntity
import com.yawmi.app.data.local.entity.DurationProfileEntity
import com.yawmi.app.data.local.entity.HolidayEntity
import com.yawmi.app.data.local.entity.LessonActionEntity
import com.yawmi.app.data.local.entity.LessonTypeEntity
import com.yawmi.app.data.local.entity.ScheduleItemEntity
import com.yawmi.app.data.local.entity.ScheduleProfileEntity
import com.yawmi.app.data.local.entity.SequenceTemplateEntity
import com.yawmi.app.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppointmentDao {
    @Query(
        "SELECT * FROM appointments WHERE dateIso = :dateIso AND deletedAt IS NULL " +
            "ORDER BY startMinutes IS NULL, startMinutes",
    )
    fun observeForDate(dateIso: String): Flow<List<AppointmentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: AppointmentEntity)
}

@Dao
interface TaskDao {
    @Query(
        "SELECT * FROM tasks WHERE isDone = 0 AND deletedAt IS NULL " +
            "ORDER BY priority DESC, dueDateIso IS NULL, dueDateIso, createdAt",
    )
    fun observeOpen(): Flow<List<TaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: TaskEntity)

    @Query("UPDATE tasks SET isDone = :done, updatedAt = :now WHERE id = :id")
    suspend fun setDone(id: String, done: Boolean, now: Long)
}

@Dao
interface ScheduleProfileDao {
    @Upsert
    suspend fun upsert(entity: ScheduleProfileEntity)
}

@Dao
interface ScheduleItemDao {
    /** عناصر الجدول المتكررة لهذا اليوم من الجداول النشطة فقط. (الاستثناءات ScheduleOverrides تُطبَّق في المرحلة 3.) */
    @Query(
        "SELECT si.* FROM schedule_items si " +
            "INNER JOIN schedule_profiles sp ON sp.id = si.scheduleProfileId " +
            "WHERE sp.isActive = 1 AND sp.deletedAt IS NULL AND si.deletedAt IS NULL " +
            "AND si.dayOfWeek = :dayOfWeek ORDER BY si.startMinutes",
    )
    fun observeActiveForDay(dayOfWeek: Int): Flow<List<ScheduleItemEntity>>

    @Upsert
    suspend fun upsert(entity: ScheduleItemEntity)
}

@Dao
interface HolidayDao {
    @Query(
        "SELECT * FROM holidays WHERE startDateIso <= :dateIso AND endDateIso >= :dateIso AND deletedAt IS NULL",
    )
    fun observeCovering(dateIso: String): Flow<List<HolidayEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: HolidayEntity)
}

@Dao
interface LessonTypeDao {
    @Query("SELECT * FROM lesson_types WHERE deletedAt IS NULL ORDER BY sortOrder")
    fun observeAll(): Flow<List<LessonTypeEntity>>

    @Query("SELECT COUNT(*) FROM lesson_types")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllIgnore(entities: List<LessonTypeEntity>)
}

@Dao
interface DurationProfileDao {
    @Query("SELECT * FROM duration_profiles WHERE deletedAt IS NULL ORDER BY minutes DESC")
    fun observeAll(): Flow<List<DurationProfileEntity>>

    @Query("SELECT COUNT(*) FROM duration_profiles")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllIgnore(entities: List<DurationProfileEntity>)
}

@Dao
interface SequenceTemplateDao {
    @Query("SELECT * FROM sequence_templates WHERE deletedAt IS NULL ORDER BY name")
    fun observeAll(): Flow<List<SequenceTemplateEntity>>

    @Query("SELECT COUNT(*) FROM sequence_templates")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllIgnore(entities: List<SequenceTemplateEntity>)
}

@Dao
interface LessonActionDao {
    /** IGNORE: إعادة إدراج الإجراء نفسه (بالمعرّف نفسه) لا تفعل شيئاً — أساس منع التكرار عند المزامنة. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(entity: LessonActionEntity): Long

    @Query("SELECT * FROM lesson_actions WHERE subjectSequenceId = :sequenceId ORDER BY timestampMillis, id")
    fun observeForSequence(sequenceId: String): Flow<List<LessonActionEntity>>
}
