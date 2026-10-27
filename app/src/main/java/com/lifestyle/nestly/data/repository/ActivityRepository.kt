package com.lifestyle.nestly.data.repository

import com.lifestyle.nestly.data.db.NestlyDatabase
import com.lifestyle.nestly.data.db.dao.DailyVolume
import com.lifestyle.nestly.data.db.dao.TypeCount
import com.lifestyle.nestly.data.db.entity.ActivityEntity
import com.lifestyle.nestly.data.db.entity.ActivityType
import com.lifestyle.nestly.data.db.entity.SyncState
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.ZoneId

class ActivityRepository(
    private val db: NestlyDatabase,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val activityDao = db.activityDao()

    fun timeline(familyId: String, profileId: String, limit: Int = 100): Flow<List<ActivityEntity>> =
        activityDao.observeTimeline(familyId, profileId, limit = limit)

    fun range(familyId: String, profileId: String, fromTime: Long, toTime: Long): Flow<List<ActivityEntity>> =
        activityDao.observeRange(familyId, profileId, fromTime, toTime)

    fun latest(familyId: String, profileId: String, type: ActivityType): Flow<ActivityEntity?> =
        activityDao.observeLatest(familyId, profileId, type)

    fun dailyBottleTotals(
        profileId: String,
        fromTime: Long,
        toTime: Long,
        zone: ZoneId = ZoneId.systemDefault(),
    ): Flow<List<DailyVolume>> =
        activityDao.observeDailyBottleTotals(profileId, fromTime, toTime, offsetMinutes(zone))

    fun dailyPumpTotals(
        familyId: String,
        fromTime: Long,
        toTime: Long,
        zone: ZoneId = ZoneId.systemDefault(),
    ): Flow<List<DailyVolume>> =
        activityDao.observeDailyPumpTotals(familyId, fromTime, toTime, offsetMinutes(zone))

    fun latestOf(familyId: String, profileId: String, types: List<ActivityType>): Flow<ActivityEntity?> =
        activityDao.observeLatestOf(familyId, profileId, types)

    fun allOf(familyId: String, profileId: String, types: List<ActivityType>): Flow<List<ActivityEntity>> =
        activityDao.observeAllOf(familyId, profileId, types)

    fun countsByType(): Flow<List<TypeCount>> = activityDao.observeCountsByType()

    suspend fun get(id: String): ActivityEntity? = activityDao.get(id)

    suspend fun getLatest(familyId: String, profileId: String, type: ActivityType): ActivityEntity? =
        activityDao.getLatest(familyId, profileId, type)

    suspend fun getLatestOf(familyId: String, profileId: String, types: List<ActivityType>): ActivityEntity? =
        activityDao.getLatestOf(familyId, profileId, types)

    suspend fun getCombo(groupId: String): List<ActivityEntity> = activityDao.getCombo(groupId)

    suspend fun save(activity: ActivityEntity) = saveAll(listOf(activity))

    suspend fun saveAll(activities: List<ActivityEntity>) {
        val now = clock()
        activityDao.upsertAll(activities.map { it.copy(updatedAt = now, syncState = SyncState.PENDING_UPLOAD) })
    }

    suspend fun delete(id: String) = activityDao.softDelete(id, clock())

    private fun offsetMinutes(zone: ZoneId): Int =
        zone.rules.getOffset(Instant.ofEpochMilli(clock())).totalSeconds / 60
}
