package com.lifestyle.nestly.data.repository

import com.lifestyle.nestly.data.model.CaregiverEntity
import com.lifestyle.nestly.data.model.FamilyEntity
import com.lifestyle.nestly.data.model.ProfileEntity
import com.lifestyle.nestly.data.model.ProfileType
import com.lifestyle.nestly.data.model.Sex
import com.lifestyle.nestly.data.prefs.UserPreferences
import com.lifestyle.nestly.data.supabase.AcceptInviteParams
import com.lifestyle.nestly.data.supabase.AcceptInviteResult
import com.lifestyle.nestly.data.supabase.CaregiverDto
import com.lifestyle.nestly.data.supabase.FamilyDto
import com.lifestyle.nestly.data.supabase.InviteDto
import com.lifestyle.nestly.data.supabase.ProfileDto
import com.lifestyle.nestly.data.supabase.RefreshBus
import com.lifestyle.nestly.data.supabase.refetchFlow
import com.lifestyle.nestly.data.supabase.toDto
import com.lifestyle.nestly.data.supabase.toIso
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class FamilyRepository(
    private val client: SupabaseClient,
    private val prefs: UserPreferences,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val authUserId: String? get() = client.auth.currentSessionOrNull()?.user?.id

    /**
     * Family data is only meaningful with a user JWT: before the stored
     * session is restored, PostgREST answers as anon and RLS returns zero
     * rows, which looks exactly like "this account has no family yet". Gate
     * on the session so no fetch runs (and no empty result is ever produced)
     * until authenticated, and refetch fresh the moment it is. While signed
     * out / restoring, these flows emit nothing, so collectors stay in their
     * loading state instead of flashing onboarding on cold start.
     */
    private fun <T> authenticatedOnly(flow: () -> Flow<T>): Flow<T> =
        client.auth.sessionStatus.flatMapLatest { status ->
            if (status is SessionStatus.Authenticated) flow() else emptyFlow()
        }

    val families: Flow<List<FamilyEntity>> = authenticatedOnly {
        refetchFlow(RefreshBus.FAMILY) {
            client.from("family").select {
                filter { filter("deleted_at", io.github.jan.supabase.postgrest.query.filter.FilterOperator.IS, null) }
            }.decodeList<FamilyDto>().map { it.toEntity() }
        }
    }

    val profiles: Flow<List<ProfileEntity>> = authenticatedOnly {
        refetchFlow(RefreshBus.PROFILE) {
            client.from("profile").select {
                filter { filter("deleted_at", io.github.jan.supabase.postgrest.query.filter.FilterOperator.IS, null) }
                order("created_at", Order.ASCENDING)
            }.decodeList<ProfileDto>().map { it.toEntity() }
        }
    }

    /** The selected child, falling back to the first profile when nothing is selected yet. */
    val selectedProfile: Flow<ProfileEntity?> =
        combine(profiles, prefs.selectedProfileId) { all, id -> all.find { it.id == id } ?: all.firstOrNull() }

    val caregivers: Flow<List<CaregiverEntity>> = selectedProfile.flatMapLatest { profile ->
        if (profile == null) flowOf(emptyList())
        else refetchFlow(RefreshBus.CAREGIVER) {
            client.from("caregiver").select {
                filter {
                    eq("family_id", profile.familyId)
                    filter("deleted_at", io.github.jan.supabase.postgrest.query.filter.FilterOperator.IS, null)
                }
                order("name", Order.ASCENDING)
            }.decodeList<CaregiverDto>().map { it.toEntity() }
        }
    }

    /** The caregiver linked to the signed-in account, falling back to the first caregiver. */
    val currentCaregiver: Flow<CaregiverEntity?> =
        combine(caregivers, prefs.currentCaregiverId) { all, storedId ->
            all.find { it.userId != null && it.userId == authUserId }
                ?: all.find { it.id == storedId }
                ?: all.firstOrNull()
        }

    suspend fun currentCaregiverId(): String? = currentCaregiver.first()?.id

    suspend fun selectProfile(id: String) = prefs.setSelectedProfile(id)

    suspend fun selectCaregiver(id: String) = prefs.setCurrentCaregiver(id)

    suspend fun fetchProfiles(): List<ProfileEntity> =
        client.from("profile").select {
            filter { filter("deleted_at", io.github.jan.supabase.postgrest.query.filter.FilterOperator.IS, null) }
            order("created_at", Order.ASCENDING)
        }.decodeList<ProfileDto>().map { it.toEntity() }

    suspend fun fetchCaregivers(familyId: String): List<CaregiverEntity> =
        client.from("caregiver").select {
            filter {
                eq("family_id", familyId)
                filter("deleted_at", io.github.jan.supabase.postgrest.query.filter.FilterOperator.IS, null)
            }
            order("name", Order.ASCENDING)
        }.decodeList<CaregiverDto>().map { it.toEntity() }

    suspend fun fetchFamilies(): List<FamilyEntity> =
        client.from("family").select {
            filter { filter("deleted_at", io.github.jan.supabase.postgrest.query.filter.FilterOperator.IS, null) }
        }.decodeList<FamilyDto>().map { it.toEntity() }

    /** Creates a child, plus a family and an OWNER caregiver for this account if none exist yet. */
    suspend fun addChild(name: String, sex: Sex?, birthDate: LocalDate?, caregiverName: String): ProfileEntity {
        val now = clock()
        val existingProfiles = fetchProfiles()
        val familyId: String
        if (existingProfiles.isNotEmpty()) {
            familyId = existingProfiles.first().familyId
        } else {
            familyId = UUID.randomUUID().toString()
            client.from("family").insert(
                FamilyDto(id = familyId, name = null, createdAt = now.toIso(), updatedAt = now.toIso()),
            )
            val caregiver = CaregiverEntity(
                id = UUID.randomUUID().toString(),
                familyId = familyId,
                name = caregiverName.ifBlank { "Caregiver" },
                userId = authUserId,
                role = "OWNER",
                createdAt = now,
                updatedAt = now,
            )
            client.from("caregiver").insert(caregiver.toDto())
            prefs.setCurrentCaregiver(caregiver.id)
        }
        // Adding another child to an existing family: make sure the account has a caregiver row.
        if (existingProfiles.isNotEmpty() && caregiverName.isNotBlank()) {
            val existing = fetchCaregivers(familyId).find { it.name == caregiverName.trim() }
            if (existing == null) {
                val caregiver = CaregiverEntity(
                    id = UUID.randomUUID().toString(),
                    familyId = familyId,
                    name = caregiverName.trim(),
                    createdAt = now,
                    updatedAt = now,
                )
                client.from("caregiver").insert(caregiver.toDto())
            }
        }
        val profile = ProfileEntity(
            id = UUID.randomUUID().toString(),
            familyId = familyId,
            name = name,
            birthDate = birthDate,
            sex = sex,
            profileType = ProfileType.CHILD,
            createdAt = now,
            updatedAt = now,
        )
        client.from("profile").insert(profile.toDto())
        prefs.setSelectedProfile(profile.id)
        RefreshBus.invalidate(RefreshBus.PROFILE, RefreshBus.CAREGIVER, RefreshBus.FAMILY)
        return profile
    }

    /** Onboarding: create a brand-new family with this account as OWNER, before any child exists. */
    suspend fun createFamily(caregiverName: String): CaregiverEntity {
        val now = clock()
        val familyId = UUID.randomUUID().toString()
        client.from("family").insert(FamilyDto(id = familyId, name = null, createdAt = now.toIso(), updatedAt = now.toIso()))
        val caregiver = CaregiverEntity(
            id = UUID.randomUUID().toString(),
            familyId = familyId,
            name = caregiverName.ifBlank { "Caregiver" },
            userId = authUserId,
            role = "OWNER",
            createdAt = now,
            updatedAt = now,
        )
        client.from("caregiver").insert(caregiver.toDto())
        prefs.setCurrentCaregiver(caregiver.id)
        RefreshBus.invalidate(RefreshBus.CAREGIVER, RefreshBus.FAMILY)
        return caregiver
    }

    /** Onboarding: join an existing family with an invite code. Returns the family id. */
    suspend fun joinWithInvite(code: String, caregiverName: String): String {
        val params = kotlinx.serialization.json.buildJsonObject {
            put("code", kotlinx.serialization.json.JsonPrimitive(code.trim()))
            put("caregiver_name", kotlinx.serialization.json.JsonPrimitive(caregiverName.trim()))
        }
        val results = client.postgrest.rpc("accept_family_invite", params)
            .decodeList<AcceptInviteResult>()
        val result = results.firstOrNull() ?: error("Invite is invalid or expired")
        prefs.setCurrentCaregiver(result.caregiverId)
        RefreshBus.invalidate(RefreshBus.PROFILE, RefreshBus.CAREGIVER, RefreshBus.FAMILY)
        return result.familyId
    }

    suspend fun createInvite(familyId: String): InviteDto {
        val invite = InviteDto(
            id = UUID.randomUUID().toString(),
            familyId = familyId,
            code = "",
        )
        // Code is generated by the database default; insert without it and read the row back.
        client.from("family_invite").insert(
            kotlinx.serialization.json.buildJsonObject {
                put("id", kotlinx.serialization.json.JsonPrimitive(invite.id))
                put("family_id", kotlinx.serialization.json.JsonPrimitive(familyId))
            },
        )
        return client.from("family_invite").select {
            filter { eq("id", invite.id) }
            limit(1)
        }.decodeSingleOrNull<InviteDto>() ?: error("Could not create invite")
    }

    suspend fun fetchInvites(familyId: String): List<InviteDto> =
        client.from("family_invite").select {
            filter {
                eq("family_id", familyId)
                filter("accepted_at", io.github.jan.supabase.postgrest.query.filter.FilterOperator.IS, null)
                filter("revoked_at", io.github.jan.supabase.postgrest.query.filter.FilterOperator.IS, null)
            }
            order("created_at", Order.DESCENDING)
        }.decodeList<InviteDto>()

    suspend fun updateChild(id: String, name: String, sex: Sex?, birthDate: LocalDate?) {
        val existing = client.from("profile").select {
            filter { eq("id", id) }
            limit(1)
        }.decodeSingleOrNull<ProfileDto>()?.toEntity() ?: return
        val updated = existing.copy(
            name = name.trim(),
            sex = sex,
            birthDate = birthDate,
            updatedAt = clock(),
            version = existing.version + 1,
        )
        client.from("profile").update(updated.toDto()) {
            filter {
                eq("id", id)
                eq("version", existing.version)
            }
        }
        RefreshBus.invalidate(RefreshBus.PROFILE)
    }

    suspend fun addCaregiver(name: String): CaregiverEntity? {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return null
        val familyId = selectedProfileFamilyId() ?: return null
        fetchCaregivers(familyId).find { it.name == trimmed }?.let { return it }
        val now = clock()
        val caregiver = CaregiverEntity(
            id = UUID.randomUUID().toString(),
            familyId = familyId,
            name = trimmed,
            createdAt = now,
            updatedAt = now,
        )
        client.from("caregiver").insert(caregiver.toDto())
        RefreshBus.invalidate(RefreshBus.CAREGIVER)
        return caregiver
    }

    private suspend fun selectedProfileFamilyId(): String? = selectedProfile.first()?.familyId

    /**
     * The family imports and other family-scoped writes should target: the
     * selected child's family when there is one, otherwise the first family
     * this account belongs to (e.g. freshly created during onboarding).
     */
    suspend fun currentFamilyId(): String? =
        selectedProfile.first()?.familyId ?: fetchFamilies().firstOrNull()?.id

    /**
     * TEST-ONLY: hard-deletes everything in the current family so testing can
     * restart from a clean slate (e.g. re-import from Nara). Deletes children
     * first, then the family itself when the caller owns it; other members'
     * caregiver rows cascade with the family delete. Remove before release —
     * the Settings entry point is already gated on BuildConfig.DEBUG.
     */
    suspend fun clearCurrentFamilyData() {
        val familyId = currentFamilyId() ?: return
        // Activities/reminders/invites would cascade with the family, but delete
        // them explicitly first so the data is gone even if the family row
        // itself can't be deleted (non-owner caller).
        client.from("activity").delete { filter { eq("family_id", familyId) } }
        client.from("reminder").delete { filter { eq("family_id", familyId) } }
        client.from("appointment_question").delete { filter { eq("family_id", familyId) } }
        client.from("appointment").delete { filter { eq("family_id", familyId) } }
        client.from("family_invite").delete { filter { eq("family_id", familyId) } }
        client.from("profile").delete { filter { eq("family_id", familyId) } }
        client.from("family").delete { filter { eq("id", familyId) } }
        // If the family row survived (non-owner), drop the now-orphaned caregivers too.
        runCatching {
            client.from("caregiver").delete { filter { eq("family_id", familyId) } }
        }
        RefreshBus.invalidate(
            RefreshBus.ACTIVITY, RefreshBus.PROFILE, RefreshBus.CAREGIVER,
            RefreshBus.FAMILY, RefreshBus.REMINDER,
            RefreshBus.APPOINTMENT, RefreshBus.APPOINTMENT_QUESTION,
        )
    }

    // --- Import/export support ---

    suspend fun importFamily(family: FamilyEntity) {
        client.from("family").upsert(family.toDto())
        RefreshBus.invalidate(RefreshBus.FAMILY)
    }

    suspend fun importCaregiver(caregiver: CaregiverEntity) {
        // Imported caregivers are name-only; never carry an account link from a file.
        client.from("caregiver").upsert(caregiver.copy(userId = null).toDto())
        RefreshBus.invalidate(RefreshBus.CAREGIVER)
    }

    suspend fun importProfile(profile: ProfileEntity) {
        client.from("profile").upsert(profile.toDto())
        RefreshBus.invalidate(RefreshBus.PROFILE)
    }

    suspend fun getProfile(id: String): ProfileEntity? =
        client.from("profile").select {
            filter { eq("id", id) }
            limit(1)
        }.decodeSingleOrNull<ProfileDto>()?.toEntity()

    suspend fun getFamily(id: String): FamilyEntity? =
        client.from("family").select {
            filter { eq("id", id) }
            limit(1)
        }.decodeSingleOrNull<FamilyDto>()?.toEntity()

    suspend fun findCaregiverByName(familyId: String, name: String): CaregiverEntity? =
        fetchCaregivers(familyId).find { it.name == name }
}
