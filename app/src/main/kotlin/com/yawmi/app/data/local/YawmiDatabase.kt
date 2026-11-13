package com.yawmi.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.yawmi.app.data.local.dao.AppointmentDao
import com.yawmi.app.data.local.dao.DurationProfileDao
import com.yawmi.app.data.local.dao.HolidayDao
import com.yawmi.app.data.local.dao.InstitutionDao
import com.yawmi.app.data.local.dao.LessonActionDao
import com.yawmi.app.data.local.dao.LessonTypeDao
import com.yawmi.app.data.local.dao.ScheduleItemDao
import com.yawmi.app.data.local.dao.ScheduleProfileDao
import com.yawmi.app.data.local.dao.SequenceTemplateDao
import com.yawmi.app.data.local.dao.SubjectCatalogDao
import com.yawmi.app.data.local.dao.TeacherProfileDao
import com.yawmi.app.data.local.dao.TeacherSubjectDao
import com.yawmi.app.data.local.dao.UserDao
import com.yawmi.app.data.local.dao.WorkDayConfigDao
import com.yawmi.app.data.local.dao.WorkProfileDao
import com.yawmi.app.data.local.dao.TaskDao
import com.yawmi.app.data.local.entity.AppointmentEntity
import com.yawmi.app.data.local.entity.AssessmentEventEntity
import com.yawmi.app.data.local.entity.BackupRecordEntity
import com.yawmi.app.data.local.entity.ClassGroupEntity
import com.yawmi.app.data.local.entity.DurationProfileEntity
import com.yawmi.app.data.local.entity.HolidayEntity
import com.yawmi.app.data.local.entity.InstitutionEntity
import com.yawmi.app.data.local.entity.LessonActionEntity
import com.yawmi.app.data.local.entity.LessonSessionEntity
import com.yawmi.app.data.local.entity.LessonTypeEntity
import com.yawmi.app.data.local.entity.NoteEntity
import com.yawmi.app.data.local.entity.NotificationEntity
import com.yawmi.app.data.local.entity.ScheduleItemEntity
import com.yawmi.app.data.local.entity.ScheduleOverrideEntity
import com.yawmi.app.data.local.entity.ScheduleProfileEntity
import com.yawmi.app.data.local.entity.SequenceTemplateEntity
import com.yawmi.app.data.local.entity.SubjectCatalogEntity
import com.yawmi.app.data.local.entity.SubjectSequenceEntity
import com.yawmi.app.data.local.entity.TaskEntity
import com.yawmi.app.data.local.entity.TeacherProfileEntity
import com.yawmi.app.data.local.entity.TeacherSubjectEntity
import com.yawmi.app.data.local.entity.UserEntity
import com.yawmi.app.data.local.entity.WorkDayConfigEntity
import com.yawmi.app.data.local.entity.WorkProfileEntity

@Database(
    entities = [
        UserEntity::class,
        TeacherProfileEntity::class,
        InstitutionEntity::class,
        ScheduleProfileEntity::class,
        DurationProfileEntity::class,
        ScheduleItemEntity::class,
        ScheduleOverrideEntity::class,
        LessonTypeEntity::class,
        SequenceTemplateEntity::class,
        ClassGroupEntity::class,
        SubjectSequenceEntity::class,
        LessonSessionEntity::class,
        LessonActionEntity::class,
        AssessmentEventEntity::class,
        AppointmentEntity::class,
        HolidayEntity::class,
        TaskEntity::class,
        NoteEntity::class,
        NotificationEntity::class,
        BackupRecordEntity::class,
        WorkProfileEntity::class,
        SubjectCatalogEntity::class,
        TeacherSubjectEntity::class,
        WorkDayConfigEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class YawmiDatabase : RoomDatabase() {
    abstract fun appointmentDao(): AppointmentDao
    abstract fun taskDao(): TaskDao
    abstract fun scheduleProfileDao(): ScheduleProfileDao
    abstract fun scheduleItemDao(): ScheduleItemDao
    abstract fun holidayDao(): HolidayDao
    abstract fun lessonTypeDao(): LessonTypeDao
    abstract fun durationProfileDao(): DurationProfileDao
    abstract fun sequenceTemplateDao(): SequenceTemplateDao
    abstract fun lessonActionDao(): LessonActionDao
    abstract fun userDao(): UserDao
    abstract fun teacherProfileDao(): TeacherProfileDao
    abstract fun institutionDao(): InstitutionDao
    abstract fun workProfileDao(): WorkProfileDao
    abstract fun subjectCatalogDao(): SubjectCatalogDao
    abstract fun teacherSubjectDao(): TeacherSubjectDao
    abstract fun workDayConfigDao(): WorkDayConfigDao

    companion object {
        const val NAME = "yawmi.db"

        fun build(context: Context): YawmiDatabase =
            Room.databaseBuilder(context.applicationContext, YawmiDatabase::class.java, NAME)
                // لا fallbackToDestructiveMigration: فقدان بيانات المستخدم غير مقبول. الترحيلات تُكتب صراحةً.
                .addMigrations(MIGRATION_1_2)
                .build()
    }
}
