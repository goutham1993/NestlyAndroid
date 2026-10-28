package com.lifestyle.nestly.data.repository

import com.lifestyle.nestly.data.model.ReminderEntity
import com.lifestyle.nestly.data.model.ReminderKind
import com.lifestyle.nestly.data.reminder.ReminderLogic
import com.lifestyle.nestly.data.supabase.RefreshBus
import com.lifestyle.nestly.data.supabase.ReminderDto
import com.lifestyle.nestly.data.supabase.refetchFlow
import com.lifestyle.nestly.data.supabase.toDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.UUID

class ReminderRepository(
    private val client: SupabaseClient,
    private val family: FamilyRepository,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    fun observeForProfile(familyId: String, profileId: String): Flow<List<ReminderEntity>> =
        refetchFlow(RefreshBus.REMINDER) { fetchForProfile(familyId, profileId) }

    suspend fun fetchForProfile(familyId: String, profileId: String): List<ReminderEntity> =
        fetchAll().filter { reminder ->
            reminder.familyId == familyId &&
                if (reminder.kind == ReminderKind.PUMP) reminder.profileId == null
                else reminder.profileId == profileId
        }

    suspend fun fetchAll(): List<ReminderEntity> =
        client.from("reminder").select {
            filter { filter("deleted_at", io.github.jan.supabase.postgrest.query.filter.FilterOperator.IS, null) }
            order("kind", Order.ASCENDING)
        }.decodeList<ReminderDto>().map { it.toEntity() }

    suspend fun getEnabled(): List<ReminderEntity> = fetchAll().filter { it.enabled }

    suspend fun getById(id: String): ReminderEntity? =
        client.from("reminder").select {
            filter { eq("id", id) }
            limit(1)
        }.decodeSingleOrNull<ReminderDto>()?.toEntity()

    private suspend fun myCaregiverId(familyId: String): String? {
        val userId = client.auth.currentSessionOrNull()?.user?.id
        val caregivers = family.fetchCaregivers(familyId)
        return caregivers.find { it.userId != null && it.userId == userId }?.id
            ?: family.currentCaregiver.first()?.id
            ?: caregivers.firstOrNull()?.id
    }

    suspend fun getOrCreate(
        familyId: String,
        profileId: String?,
        kind: ReminderKind,
    ): ReminderEntity {
        val scopedProfile = if (kind == ReminderKind.PUMP) null else profileId
        fetchAll().find { it.kind == kind && it.profileId == scopedProfile && it.familyId == familyId }
            ?.let { return it }
        val caregiverId = myCaregiverId(familyId) ?: error("No caregiver for this account yet")
        val created = ReminderEntity(
            id = UUID.randomUUID().toString(),
            familyId = familyId,
            caregiverId = caregiverId,
            profileId = scopedProfile,
            kind = kind,
            intervalMinutes = ReminderLogic.defaultIntervalMinutes(kind),
            enabled = false,
            updatedAt = clock(),
        )
        client.from("reminder").insert(created.toDto(createdAt = created.updatedAt))
        RefreshBus.invalidate(RefreshBus.REMINDER)
        return created
    }

    suspend fun save(reminder: ReminderEntity) {
        val updated = reminder.copy(updatedAt = clock(), version = reminder.version + 1)
        client.from("reminder").update(updated.toDto()) {
            filter {
                eq("id", reminder.id)
                eq("version", reminder.version)
            }
        }
        RefreshBus.invalidate(RefreshBus.REMINDER)
    }

    suspend fun setEnabled(kind: ReminderKind, familyId: String, profileId: String?, enabled: Boolean): ReminderEntity {
        val current = getOrCreate(familyId, profileId, kind)
        val updated = current.copy(enabled = enabled, updatedAt = clock(), version = current.version + 1)
        client.from("reminder").update(updated.toDto()) {
            filter {
                eq("id", current.id)
                eq("version", current.version)
            }
        }
        RefreshBus.invalidate(RefreshBus.REMINDER)
        return updated
    }

    suspend fun setInterval(kind: ReminderKind, familyId: String, profileId: String?, minutes: Int): ReminderEntity {
        val current = getOrCreate(familyId, profileId, kind)
        val updated = current.copy(intervalMinutes = minutes.coerceAtLeast(1), updatedAt = clock(), version = current.version + 1)
        client.from("reminder").update(updated.toDto()) {
            filter {
                eq("id", current.id)
                eq("version", current.version)
            }
        }
        RefreshBus.invalidate(RefreshBus.REMINDER)
        return updated
    }
}
