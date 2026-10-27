package com.lifestyle.nestly.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.lifestyle.nestly.data.db.entity.CaregiverEntity
import com.lifestyle.nestly.data.db.entity.FamilyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FamilyDao {
    @Upsert
    suspend fun upsertFamily(family: FamilyEntity)

    @Upsert
    suspend fun upsertCaregiver(caregiver: CaregiverEntity)

    @Query("SELECT * FROM family WHERE id = :id")
    suspend fun getFamily(id: String): FamilyEntity?

    @Query("SELECT * FROM family WHERE deletedAt IS NULL ORDER BY createdAt LIMIT 1")
    fun observeCurrentFamily(): Flow<FamilyEntity?>

    @Query("SELECT * FROM caregiver WHERE familyId = :familyId AND name = :name LIMIT 1")
    suspend fun findCaregiverByName(familyId: String, name: String): CaregiverEntity?

    @Query("SELECT * FROM caregiver WHERE deletedAt IS NULL ORDER BY name")
    suspend fun getAllActiveCaregivers(): List<CaregiverEntity>

    @Query("SELECT * FROM family WHERE deletedAt IS NULL")
    suspend fun getAllActiveFamilies(): List<FamilyEntity>

    @Query("SELECT * FROM caregiver WHERE familyId = :familyId AND deletedAt IS NULL ORDER BY name")
    fun observeCaregivers(familyId: String): Flow<List<CaregiverEntity>>
}
