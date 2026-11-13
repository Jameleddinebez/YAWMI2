package com.yawmi.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.yawmi.app.data.local.entity.InstitutionEntity
import com.yawmi.app.data.local.entity.SubjectCatalogEntity
import com.yawmi.app.data.local.entity.TeacherProfileEntity
import com.yawmi.app.data.local.entity.TeacherSubjectEntity
import com.yawmi.app.data.local.entity.UserEntity
import com.yawmi.app.data.local.entity.WorkDayConfigEntity
import com.yawmi.app.data.local.entity.WorkProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Upsert
    suspend fun upsert(entity: UserEntity)
}

@Dao
interface TeacherProfileDao {
    @Upsert
    suspend fun upsert(entity: TeacherProfileEntity)

    @Query("SELECT fullName FROM teacher_profiles WHERE deletedAt IS NULL ORDER BY createdAt LIMIT 1")
    fun observeFullName(): Flow<String?>
}

@Dao
interface InstitutionDao {
    @Upsert
    suspend fun upsert(entity: InstitutionEntity)
}

@Dao
interface WorkProfileDao {
    @Upsert
    suspend fun upsert(entity: WorkProfileEntity)

    @Query("SELECT COUNT(*) FROM work_profiles WHERE deletedAt IS NULL")
    fun observeCount(): Flow<Int>
}

@Dao
interface SubjectCatalogDao {
    @Query(
        "SELECT * FROM subject_catalog WHERE educationLevel = :level AND isActive = 1 " +
            "ORDER BY isCustom, nameAr",
    )
    fun observeByLevel(level: String): Flow<List<SubjectCatalogEntity>>

    @Query("SELECT COUNT(*) FROM subject_catalog")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllIgnore(entities: List<SubjectCatalogEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(entity: SubjectCatalogEntity)
}

@Dao
interface TeacherSubjectDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllIgnore(entities: List<TeacherSubjectEntity>)
}

@Dao
interface WorkDayConfigDao {
    @Upsert
    suspend fun upsertAll(entities: List<WorkDayConfigEntity>)
}
