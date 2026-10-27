package com.lifestyle.nestly.data.repository

import com.lifestyle.nestly.data.db.NestlyDatabase
import com.lifestyle.nestly.data.db.entity.CaregiverEntity
import com.lifestyle.nestly.data.db.entity.FamilyEntity
import com.lifestyle.nestly.data.db.entity.ProfileEntity
import com.lifestyle.nestly.data.db.entity.ProfileType
import com.lifestyle.nestly.data.db.entity.Sex
import com.lifestyle.nestly.data.prefs.UserPreferences
import androidx.room.withTransaction
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import java.time.LocalDate
import java.util.UUID

class FamilyRepository(
    private val db: NestlyDatabase,
    private val prefs: UserPreferences,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    val profiles: Flow<List<ProfileEntity>> = db.profileDao().observeAll()

    /** The selected child, falling back to the first profile when nothing is selected yet. */
    val selectedProfile: Flow<ProfileEntity?> =
        combine(profiles, prefs.selectedProfileId) { all, id -> all.find { it.id == id } ?: all.firstOrNull() }

    @OptIn(ExperimentalCoroutinesApi::class)
    val caregivers: Flow<List<CaregiverEntity>> = selectedProfile.flatMapLatest { profile ->
        profile?.let { db.familyDao().observeCaregivers(it.familyId) } ?: flowOf(emptyList())
    }

    /** The caregiver using this device, falling back to the first one in the family. */
    val currentCaregiver: Flow<CaregiverEntity?> =
        combine(caregivers, prefs.currentCaregiverId) { all, id -> all.find { it.id == id } ?: all.firstOrNull() }

    suspend fun currentCaregiverId(): String? = currentCaregiver.first()?.id

    suspend fun selectProfile(id: String) = prefs.setSelectedProfile(id)

    suspend fun selectCaregiver(id: String) = prefs.setCurrentCaregiver(id)

    /** Creates a child, plus a family and a caregiver for this device if none exist yet. */
    suspend fun addChild(name: String, sex: Sex?, birthDate: LocalDate?, caregiverName: String): ProfileEntity {
        val now = clock()
        val familyDao = db.familyDao()
        val profile = db.withTransaction {
            val familyId = profiles.first().firstOrNull()?.familyId
                ?: UUID.randomUUID().toString().also {
                    familyDao.upsertFamily(FamilyEntity(id = it, name = null, createdAt = now, updatedAt = now))
                }
            if (caregiverName.isNotBlank() && familyDao.findCaregiverByName(familyId, caregiverName) == null) {
                val caregiver = CaregiverEntity(
                    id = UUID.randomUUID().toString(),
                    familyId = familyId,
                    name = caregiverName,
                    createdAt = now,
                    updatedAt = now,
                )
                familyDao.upsertCaregiver(caregiver)
                prefs.setCurrentCaregiver(caregiver.id)
            }
            ProfileEntity(
                id = UUID.randomUUID().toString(),
                familyId = familyId,
                name = name,
                birthDate = birthDate,
                sex = sex,
                profileType = ProfileType.CHILD,
                createdAt = now,
                updatedAt = now,
            ).also { db.profileDao().upsert(it) }
        }
        prefs.setSelectedProfile(profile.id)
        return profile
    }

    suspend fun updateChild(id: String, name: String, sex: Sex?, birthDate: LocalDate?) {
        val existing = db.profileDao().get(id) ?: return
        db.profileDao().upsert(
            existing.copy(
                name = name.trim(),
                sex = sex,
                birthDate = birthDate,
                updatedAt = clock(),
            ),
        )
    }

    suspend fun addCaregiver(name: String): CaregiverEntity? {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return null
        val familyId = selectedProfile.first()?.familyId ?: return null
        db.familyDao().findCaregiverByName(familyId, trimmed)?.let { return it }
        val now = clock()
        val caregiver = CaregiverEntity(
            id = UUID.randomUUID().toString(),
            familyId = familyId,
            name = trimmed,
            createdAt = now,
            updatedAt = now,
        )
        db.familyDao().upsertCaregiver(caregiver)
        return caregiver
    }
}
