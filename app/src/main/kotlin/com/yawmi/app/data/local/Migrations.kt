package com.yawmi.app.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * 1 → 2: إضافة ملفات العمل والمواد وإعدادات أيام العمل، وربط الجدول بملف العمل.
 * إضافات فقط: لا حذف ولا تعديل لأي بيانات موجودة. نصوص SQL مطابقة لما يولّده Room (يتحقق منها عند الفتح).
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `work_profiles` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, " +
                "`educationLevel` TEXT NOT NULL, `institutionId` TEXT, `durationProfileId` TEXT, " +
                "`lessonMinutes` INTEGER NOT NULL, `breakMinutes` INTEGER NOT NULL, `isArchived` INTEGER NOT NULL, " +
                "`createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `deletedAt` INTEGER, " +
                "PRIMARY KEY(`id`), FOREIGN KEY(`institutionId`) REFERENCES `institutions`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE SET NULL )",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_work_profiles_institutionId` ON `work_profiles` (`institutionId`)")

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `subject_catalog` (`id` TEXT NOT NULL, `nameAr` TEXT NOT NULL, `nameFr` TEXT, " +
                "`educationLevel` TEXT NOT NULL, `grade` TEXT, `stream` TEXT, `academicYear` TEXT NOT NULL, " +
                "`sourceNote` TEXT NOT NULL, `catalogVersion` TEXT NOT NULL, `isVerified` INTEGER NOT NULL, " +
                "`isCustom` INTEGER NOT NULL, `isActive` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, " +
                "`updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_subject_catalog_educationLevel` ON `subject_catalog` (`educationLevel`)")

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `teacher_subjects` (`id` TEXT NOT NULL, `workProfileId` TEXT NOT NULL, " +
                "`subjectId` TEXT NOT NULL, `colorHex` TEXT, `isDefault` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, " +
                "`updatedAt` INTEGER NOT NULL, `deletedAt` INTEGER, PRIMARY KEY(`id`), " +
                "FOREIGN KEY(`workProfileId`) REFERENCES `work_profiles`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT , " +
                "FOREIGN KEY(`subjectId`) REFERENCES `subject_catalog`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT )",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_teacher_subjects_workProfileId` ON `teacher_subjects` (`workProfileId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_teacher_subjects_subjectId` ON `teacher_subjects` (`subjectId`)")
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_teacher_subjects_workProfileId_subjectId` " +
                "ON `teacher_subjects` (`workProfileId`, `subjectId`)",
        )

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `work_day_configs` (`id` TEXT NOT NULL, `workProfileId` TEXT NOT NULL, " +
                "`dayOfWeek` INTEGER NOT NULL, `mode` TEXT NOT NULL, `morningStartMinutes` INTEGER NOT NULL, " +
                "`morningPeriods` INTEGER NOT NULL, `eveningStartMinutes` INTEGER NOT NULL, `eveningPeriods` INTEGER NOT NULL, " +
                "`createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`), " +
                "FOREIGN KEY(`workProfileId`) REFERENCES `work_profiles`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_work_day_configs_workProfileId` ON `work_day_configs` (`workProfileId`)")
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_work_day_configs_workProfileId_dayOfWeek` " +
                "ON `work_day_configs` (`workProfileId`, `dayOfWeek`)",
        )

        // ربط الجدول بملف العمل (عمود اختياري + فهرس). الصفوف القديمة تبقى بقيمة NULL.
        db.execSQL("ALTER TABLE `schedule_profiles` ADD COLUMN `workProfileId` TEXT")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_schedule_profiles_workProfileId` ON `schedule_profiles` (`workProfileId`)")
    }
}
