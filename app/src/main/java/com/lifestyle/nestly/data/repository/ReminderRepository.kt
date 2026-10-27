package com.lifestyle.nestly.data.repository

import com.lifestyle.nestly.data.db.NestlyDatabase
import com.lifestyle.nestly.data.db.entity.ReminderEntity
import com.lifestyle.nestly.data.db.entity.ReminderKind
import com.lifestyle.nestly.data.reminder.ReminderLogic
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class ReminderRepository(
    private val db: NestlyDatabase,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val dao = db.reminderDao()

    fun observeForProfile(familyId: String, profileId: String): Flow<List<ReminderEntity>> =
        dao.observeForProfile(familyId, profileId)

    suspend fun getEnabled(): List<ReminderEntity> = dao.getEnabled()

    suspend fun getById(id: String): ReminderEntity? = dao.getById(id)

    suspend fun getOrCreate(
        familyId: String,
        profileId: String?,
        kind: ReminderKind,
    ): ReminderEntity {
        val scopedProfile = if (kind == ReminderKind.PUMP) null else profileId
        dao.get(kind, scopedProfile)?.let { return it }
        val created = ReminderEntity(
            id = UUID.randomUUID().toString(),
            familyId = familyId,
            profileId = scopedProfile,
            kind = kind,
            intervalMinutes = ReminderLogic.defaultIntervalMinutes(kind),
            enabled = false,
            updatedAt = clock(),
        )
        dao.upsert(created)
        return created
    }

    suspend fun save(reminder: ReminderEntity) {
        dao.upsert(reminder.copy(updatedAt = clock()))
    }

    suspend fun setEnabled(kind: ReminderKind, familyId: String, profileId: String?, enabled: Boolean): ReminderEntity {
        val current = getOrCreate(familyId, profileId, kind)
        val updated = current.copy(enabled = enabled, updatedAt = clock())
        dao.upsert(updated)
        return updated
    }

    suspend fun setInterval(kind: ReminderKind, familyId: String, profileId: String?, minutes: Int): ReminderEntity {
        val current = getOrCreate(familyId, profileId, kind)
        val updated = current.copy(intervalMinutes = minutes.coerceAtLeast(1), updatedAt = clock())
        dao.upsert(updated)
        return updated
    }
}
