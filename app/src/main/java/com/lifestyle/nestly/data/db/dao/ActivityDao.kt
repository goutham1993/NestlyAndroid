package com.lifestyle.nestly.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.lifestyle.nestly.data.db.entity.ActivityEntity
import com.lifestyle.nestly.data.db.entity.ActivityType
import kotlinx.coroutines.flow.Flow

data class TypeCount(val type: ActivityType, val count: Int)

data class DailyVolume(val day: String, val totalMl: Double, val count: Int)

@Dao
interface ActivityDao {
    @Upsert
    suspend fun upsert(activity: ActivityEntity)

    @Upsert
    suspend fun upsertAll(activities: List<ActivityEntity>)

    @Query("SELECT * FROM activity WHERE id = :id")
    suspend fun get(id: String): ActivityEntity?

    @Query("UPDATE activity SET deletedAt = :now, updatedAt = :now, syncState = 'PENDING_DELETE' WHERE id = :id")
    suspend fun softDelete(id: String, now: Long)

    /** Child activities plus family-level ones (pumping) in reverse chronological order. */
    @Query(
        """
        SELECT * FROM activity
        WHERE familyId = :familyId
          AND (profileId = :profileId OR profileId IS NULL)
          AND deletedAt IS NULL
          AND startTime < :beforeTime
        ORDER BY startTime DESC
        LIMIT :limit
        """,
    )
    fun observeTimeline(
        familyId: String,
        profileId: String,
        beforeTime: Long = Long.MAX_VALUE,
        limit: Int = 100,
    ): Flow<List<ActivityEntity>>

    @Query(
        """
        SELECT * FROM activity
        WHERE familyId = :familyId
          AND (profileId = :profileId OR profileId IS NULL)
          AND deletedAt IS NULL
          AND startTime >= :fromTime AND startTime < :toTime
        ORDER BY startTime DESC
        """,
    )
    fun observeRange(familyId: String, profileId: String, fromTime: Long, toTime: Long): Flow<List<ActivityEntity>>

    @Query(
        """
        SELECT * FROM activity
        WHERE familyId = :familyId
          AND (profileId = :profileId OR profileId IS NULL)
          AND type = :type
          AND deletedAt IS NULL
        ORDER BY startTime DESC
        LIMIT 1
        """,
    )
    fun observeLatest(familyId: String, profileId: String, type: ActivityType): Flow<ActivityEntity?>

    @Query(
        """
        SELECT * FROM activity
        WHERE familyId = :familyId
          AND (profileId = :profileId OR profileId IS NULL)
          AND type IN (:types)
          AND deletedAt IS NULL
        ORDER BY startTime DESC
        LIMIT 1
        """,
    )
    fun observeLatestOf(familyId: String, profileId: String, types: List<ActivityType>): Flow<ActivityEntity?>

    @Query(
        """
        SELECT * FROM activity
        WHERE familyId = :familyId
          AND (profileId = :profileId OR profileId IS NULL)
          AND type IN (:types)
          AND deletedAt IS NULL
        ORDER BY startTime DESC
        """,
    )
    fun observeAllOf(familyId: String, profileId: String, types: List<ActivityType>): Flow<List<ActivityEntity>>

    @Query("SELECT * FROM activity WHERE comboGroupId = :groupId AND deletedAt IS NULL")
    suspend fun getCombo(groupId: String): List<ActivityEntity>

    @Query(
        """
        SELECT * FROM activity
        WHERE familyId = :familyId
          AND (profileId = :profileId OR profileId IS NULL)
          AND type = :type
          AND deletedAt IS NULL
        ORDER BY startTime DESC
        LIMIT 1
        """,
    )
    suspend fun getLatest(familyId: String, profileId: String, type: ActivityType): ActivityEntity?

    /**
     * Daily bottle intake. [utcOffsetMinutes] shifts epoch millis into the local day; callers pass the
     * current offset for the user's time zone.
     */
    @Query(
        """
        SELECT date((startTime / 1000) + :utcOffsetMinutes * 60, 'unixepoch') AS day,
               COALESCE(SUM(bottle_totalMl), 0) AS totalMl,
               COUNT(*) AS count
        FROM activity
        WHERE profileId = :profileId
          AND type = 'BOTTLE_FEED'
          AND deletedAt IS NULL
          AND startTime >= :fromTime AND startTime < :toTime
        GROUP BY day
        ORDER BY day
        """,
    )
    fun observeDailyBottleTotals(
        profileId: String,
        fromTime: Long,
        toTime: Long,
        utcOffsetMinutes: Int,
    ): Flow<List<DailyVolume>>

    @Query(
        """
        SELECT date((startTime / 1000) + :utcOffsetMinutes * 60, 'unixepoch') AS day,
               COALESCE(SUM(pump_totalMl), 0) AS totalMl,
               COUNT(*) AS count
        FROM activity
        WHERE familyId = :familyId
          AND type = 'PUMP'
          AND deletedAt IS NULL
          AND startTime >= :fromTime AND startTime < :toTime
        GROUP BY day
        ORDER BY day
        """,
    )
    fun observeDailyPumpTotals(
        familyId: String,
        fromTime: Long,
        toTime: Long,
        utcOffsetMinutes: Int,
    ): Flow<List<DailyVolume>>

    @Query(
        """
        SELECT * FROM activity
        WHERE familyId = :familyId
          AND (profileId = :profileId OR profileId IS NULL)
          AND type IN (:types)
          AND deletedAt IS NULL
        ORDER BY startTime DESC
        LIMIT 1
        """,
    )
    suspend fun getLatestOf(familyId: String, profileId: String, types: List<ActivityType>): ActivityEntity?

    @Query("SELECT * FROM activity WHERE deletedAt IS NULL ORDER BY startTime")
    suspend fun getAllActive(): List<ActivityEntity>

    @Query("SELECT type, COUNT(*) AS count FROM activity WHERE deletedAt IS NULL GROUP BY type")
    fun observeCountsByType(): Flow<List<TypeCount>>

    @Query("SELECT type, COUNT(*) AS count FROM activity WHERE deletedAt IS NULL GROUP BY type")
    suspend fun countsByType(): List<TypeCount>
}
