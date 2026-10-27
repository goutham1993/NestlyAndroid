package com.lifestyle.nestly.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.lifestyle.nestly.data.db.entity.ReminderEntity
import com.lifestyle.nestly.data.db.entity.ReminderKind
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
    @Upsert
    suspend fun upsert(reminder: ReminderEntity)

    @Query("SELECT * FROM reminder WHERE familyId = :familyId ORDER BY kind")
    fun observeForFamily(familyId: String): Flow<List<ReminderEntity>>

    @Query(
        """
        SELECT * FROM reminder
        WHERE familyId = :familyId
          AND (
            (kind = 'PUMP' AND profileId IS NULL)
            OR (kind != 'PUMP' AND profileId = :profileId)
          )
        ORDER BY kind
        """,
    )
    fun observeForProfile(familyId: String, profileId: String): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminder WHERE enabled = 1")
    suspend fun getEnabled(): List<ReminderEntity>

    @Query(
        """
        SELECT * FROM reminder
        WHERE kind = :kind
          AND (
            (:profileId IS NULL AND profileId IS NULL)
            OR profileId = :profileId
          )
        LIMIT 1
        """,
    )
    suspend fun get(kind: ReminderKind, profileId: String?): ReminderEntity?

    @Query("SELECT * FROM reminder WHERE id = :id")
    suspend fun getById(id: String): ReminderEntity?
}
