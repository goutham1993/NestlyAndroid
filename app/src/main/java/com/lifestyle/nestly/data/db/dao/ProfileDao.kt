package com.lifestyle.nestly.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.lifestyle.nestly.data.db.entity.ProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Upsert
    suspend fun upsert(profile: ProfileEntity)

    @Query("SELECT * FROM profile WHERE id = :id")
    suspend fun get(id: String): ProfileEntity?

    @Query("SELECT * FROM profile WHERE familyId = :familyId AND deletedAt IS NULL ORDER BY createdAt")
    fun observeProfiles(familyId: String): Flow<List<ProfileEntity>>

    @Query("SELECT * FROM profile WHERE deletedAt IS NULL ORDER BY createdAt")
    suspend fun getAllActive(): List<ProfileEntity>

    @Query("SELECT * FROM profile WHERE deletedAt IS NULL ORDER BY createdAt")
    fun observeAll(): Flow<List<ProfileEntity>>
}
