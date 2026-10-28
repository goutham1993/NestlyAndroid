package com.lifestyle.nestly.data.repository

import com.lifestyle.nestly.data.model.ActivityEntity
import com.lifestyle.nestly.data.model.ActivityType
import com.lifestyle.nestly.data.model.DailyVolume
import com.lifestyle.nestly.data.model.TypeCount
import com.lifestyle.nestly.data.supabase.ActivityDto
import com.lifestyle.nestly.data.supabase.ConflictException
import com.lifestyle.nestly.data.supabase.PhotoStore
import com.lifestyle.nestly.data.supabase.RefreshBus
import com.lifestyle.nestly.data.supabase.TypeOnlyDto
import com.lifestyle.nestly.data.supabase.refetchFlow
import com.lifestyle.nestly.data.supabase.toDto
import com.lifestyle.nestly.data.supabase.toIso
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.ZoneId

class ActivityRepository(
    private val client: SupabaseClient,
    private val photos: PhotoStore,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    fun timeline(familyId: String, profileId: String, limit: Int = 100): Flow<List<ActivityEntity>> =
        refetchFlow(RefreshBus.ACTIVITY) {
            client.from("activity").select {
                filter {
                    eq("family_id", familyId)
                    filter("deleted_at", io.github.jan.supabase.postgrest.query.filter.FilterOperator.IS, null)
                    or {
                        eq("profile_id", profileId)
                        filter("profile_id", io.github.jan.supabase.postgrest.query.filter.FilterOperator.IS, null)
                    }
                }
                order("start_time", Order.DESCENDING)
                limit(limit.toLong())
            }.decodeList<ActivityDto>().map { it.toEntity() }
        }

    fun range(familyId: String, profileId: String, fromTime: Long, toTime: Long): Flow<List<ActivityEntity>> =
        refetchFlow(RefreshBus.ACTIVITY) {
            fetchRange(familyId, profileId, fromTime, toTime)
        }

    suspend fun fetchRange(familyId: String, profileId: String, fromTime: Long, toTime: Long): List<ActivityEntity> =
        client.from("activity").select {
            filter {
                eq("family_id", familyId)
                filter("deleted_at", io.github.jan.supabase.postgrest.query.filter.FilterOperator.IS, null)
                or {
                    eq("profile_id", profileId)
                    filter("profile_id", io.github.jan.supabase.postgrest.query.filter.FilterOperator.IS, null)
                }
                gte("start_time", fromTime.toIso())
                lt("start_time", toTime.toIso())
            }
            order("start_time", Order.DESCENDING)
        }.decodeList<ActivityDto>().map { it.toEntity() }

    fun latest(familyId: String, profileId: String, type: ActivityType): Flow<ActivityEntity?> =
        refetchFlow(RefreshBus.ACTIVITY) { fetchLatest(familyId, profileId, listOf(type)) }

    fun latestOf(familyId: String, profileId: String, types: List<ActivityType>): Flow<ActivityEntity?> =
        refetchFlow(RefreshBus.ACTIVITY) { fetchLatest(familyId, profileId, types) }

    fun allOf(familyId: String, profileId: String, types: List<ActivityType>): Flow<List<ActivityEntity>> =
        refetchFlow(RefreshBus.ACTIVITY) {
            client.from("activity").select {
                filter {
                    eq("family_id", familyId)
                    filter("deleted_at", io.github.jan.supabase.postgrest.query.filter.FilterOperator.IS, null)
                    or {
                        eq("profile_id", profileId)
                        filter("profile_id", io.github.jan.supabase.postgrest.query.filter.FilterOperator.IS, null)
                    }
                    isIn("type", types.map { it.name })
                }
                order("start_time", Order.DESCENDING)
            }.decodeList<ActivityDto>().map { it.toEntity() }
        }

    /** Client-side aggregation replacing the old Room GROUP BY queries. */
    fun dailyBottleTotals(
        profileId: String,
        fromTime: Long,
        toTime: Long,
        zone: ZoneId = ZoneId.systemDefault(),
    ): Flow<List<DailyVolume>> = refetchFlow(RefreshBus.ACTIVITY) {
        val rows = client.from("activity").select {
            filter {
                eq("profile_id", profileId)
                eq("type", ActivityType.BOTTLE_FEED.name)
                filter("deleted_at", io.github.jan.supabase.postgrest.query.filter.FilterOperator.IS, null)
                gte("start_time", fromTime.toIso())
                lt("start_time", toTime.toIso())
            }
            order("start_time", Order.ASCENDING)
        }.decodeList<ActivityDto>().map { it.toEntity() }
        rows.groupBy { Instant.ofEpochMilli(it.startTime).atZone(zone).toLocalDate().toString() }
            .map { (day, entries) -> DailyVolume(day, entries.sumOf { it.bottle?.totalMl ?: 0.0 }, entries.size) }
            .sortedBy { it.day }
    }

    fun dailyPumpTotals(
        familyId: String,
        fromTime: Long,
        toTime: Long,
        zone: ZoneId = ZoneId.systemDefault(),
    ): Flow<List<DailyVolume>> = refetchFlow(RefreshBus.ACTIVITY) {
        val rows = client.from("activity").select {
            filter {
                eq("family_id", familyId)
                eq("type", ActivityType.PUMP.name)
                filter("deleted_at", io.github.jan.supabase.postgrest.query.filter.FilterOperator.IS, null)
                gte("start_time", fromTime.toIso())
                lt("start_time", toTime.toIso())
            }
            order("start_time", Order.ASCENDING)
        }.decodeList<ActivityDto>().map { it.toEntity() }
        rows.groupBy { Instant.ofEpochMilli(it.startTime).atZone(zone).toLocalDate().toString() }
            .map { (day, entries) -> DailyVolume(day, entries.sumOf { it.pump?.totalMl ?: 0.0 }, entries.size) }
            .sortedBy { it.day }
    }

    fun countsByType(): Flow<List<TypeCount>> = refetchFlow(RefreshBus.ACTIVITY) { fetchCountsByType() }

    suspend fun fetchCountsByType(): List<TypeCount> =
        client.from("activity").select(columns = io.github.jan.supabase.postgrest.query.Columns.list("type")) {
            filter { filter("deleted_at", io.github.jan.supabase.postgrest.query.filter.FilterOperator.IS, null) }
        }.decodeList<TypeOnlyDto>()
            .groupingBy { it.type }.eachCount()
            .map { (type, count) -> TypeCount(type, count) }

    suspend fun getAllActive(familyId: String? = null): List<ActivityEntity> =
        client.from("activity").select {
            filter {
                filter("deleted_at", io.github.jan.supabase.postgrest.query.filter.FilterOperator.IS, null)
                if (familyId != null) eq("family_id", familyId)
            }
            order("start_time", Order.ASCENDING)
        }.decodeList<ActivityDto>().map { it.toEntity() }

    suspend fun get(id: String): ActivityEntity? =
        client.from("activity").select {
            filter { eq("id", id) }
            limit(1)
        }.decodeSingleOrNull<ActivityDto>()?.toEntity()

    suspend fun getLatest(familyId: String, profileId: String, type: ActivityType): ActivityEntity? =
        fetchLatest(familyId, profileId, listOf(type))

    suspend fun getLatestOf(familyId: String, profileId: String, types: List<ActivityType>): ActivityEntity? =
        fetchLatest(familyId, profileId, types)

    private suspend fun fetchLatest(familyId: String, profileId: String, types: List<ActivityType>): ActivityEntity? =
        client.from("activity").select {
            filter {
                eq("family_id", familyId)
                filter("deleted_at", io.github.jan.supabase.postgrest.query.filter.FilterOperator.IS, null)
                if (profileId.isBlank()) {
                    filter("profile_id", io.github.jan.supabase.postgrest.query.filter.FilterOperator.IS, null)
                } else {
                    or {
                        eq("profile_id", profileId)
                        filter("profile_id", io.github.jan.supabase.postgrest.query.filter.FilterOperator.IS, null)
                    }
                }
                isIn("type", types.map { it.name })
            }
            order("start_time", Order.DESCENDING)
            limit(1)
        }.decodeSingleOrNull<ActivityDto>()?.toEntity()

    suspend fun getCombo(groupId: String): List<ActivityEntity> =
        client.from("activity").select {
            filter {
                eq("combo_group_id", groupId)
                filter("deleted_at", io.github.jan.supabase.postgrest.query.filter.FilterOperator.IS, null)
            }
        }.decodeList<ActivityDto>().map { it.toEntity() }

    suspend fun save(activity: ActivityEntity) = saveAll(listOf(activity))

    suspend fun saveAll(activities: List<ActivityEntity>) {
        val now = clock()
        activities.forEach { raw ->
            val withPhoto = raw.copy(photoUri = photos.uploadIfLocal(raw.photoUri, raw.familyId, raw.id))
            val prepared = withPhoto.copy(updatedAt = now)
            val existing = get(prepared.id)
            if (existing == null) {
                client.from("activity").insert(prepared.toDto())
            } else {
                val updated = prepared.copy(version = existing.version + 1)
                client.from("activity").update(updated.toDto()) {
                    filter {
                        eq("id", prepared.id)
                        eq("version", existing.version)
                    }
                }
                val fresh = get(prepared.id)
                if (fresh == null || fresh.version != updated.version) throw ConflictException()
            }
        }
        RefreshBus.invalidate(RefreshBus.ACTIVITY)
    }

    /** Batch insert used by CSV importers; rows are new or re-imported by id. */
    suspend fun importAll(activities: List<ActivityEntity>) {
        activities.chunked(200).forEach { chunk ->
            client.from("activity").upsert(chunk.map { it.toDto() })
        }
        RefreshBus.invalidate(RefreshBus.ACTIVITY)
    }

    suspend fun delete(id: String) {
        val existing = get(id) ?: return
        val deleted = existing.copy(deletedAt = clock(), updatedAt = clock(), version = existing.version + 1)
        client.from("activity").update(deleted.toDto()) {
            filter {
                eq("id", id)
                eq("version", existing.version)
            }
        }
        RefreshBus.invalidate(RefreshBus.ACTIVITY)
    }

    suspend fun resolvePhoto(activity: ActivityEntity): ActivityEntity =
        activity.copy(photoUri = photos.resolveToLocal(activity.photoUri))
}
