package com.lifestyle.nestly.data.repository

import com.lifestyle.nestly.data.model.NoteEntity
import com.lifestyle.nestly.data.supabase.ConflictException
import com.lifestyle.nestly.data.supabase.NoteDto
import com.lifestyle.nestly.data.supabase.RefreshBus
import com.lifestyle.nestly.data.supabase.refetchFlow
import com.lifestyle.nestly.data.supabase.toDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import kotlinx.coroutines.flow.Flow

class NoteRepository(
    private val client: SupabaseClient,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    /** Notes for one child, most recently edited first. */
    fun observeForProfile(familyId: String, profileId: String): Flow<List<NoteEntity>> =
        refetchFlow(RefreshBus.NOTE) { fetchForProfile(familyId, profileId) }

    fun observeById(id: String): Flow<NoteEntity?> =
        refetchFlow(RefreshBus.NOTE) { getById(id) }

    suspend fun fetchForProfile(familyId: String, profileId: String): List<NoteEntity> =
        client.from("note").select {
            filter {
                eq("family_id", familyId)
                eq("profile_id", profileId)
                filter("deleted_at", FilterOperator.IS, null)
            }
            order("updated_at", Order.DESCENDING)
        }.decodeList<NoteDto>().map { it.toEntity() }

    suspend fun getById(id: String): NoteEntity? =
        client.from("note").select {
            filter { eq("id", id) }
            limit(1)
        }.decodeSingleOrNull<NoteDto>()?.toEntity()?.takeIf { it.deletedAt == null }

    /** Inserts a new note or version-checked-updates an existing one. Returns the saved row. */
    suspend fun save(note: NoteEntity): NoteEntity {
        val now = clock()
        val existing = getById(note.id)
        val saved = if (existing == null) {
            val created = note.copy(createdAt = now, updatedAt = now)
            client.from("note").insert(created.toDto())
            created
        } else {
            val updated = note.copy(
                createdAt = existing.createdAt,
                updatedAt = now,
                version = existing.version + 1,
            )
            client.from("note").update(updated.toDto()) {
                filter {
                    eq("id", note.id)
                    eq("version", note.version)
                }
            }
            val fresh = getById(note.id)
            if (fresh == null || fresh.version != updated.version) throw ConflictException()
            updated
        }
        RefreshBus.invalidate(RefreshBus.NOTE)
        return saved
    }

    suspend fun delete(id: String) {
        val existing = getById(id) ?: return
        val deleted = existing.copy(deletedAt = clock(), updatedAt = clock(), version = existing.version + 1)
        client.from("note").update(deleted.toDto()) {
            filter {
                eq("id", id)
                eq("version", existing.version)
            }
        }
        RefreshBus.invalidate(RefreshBus.NOTE)
    }
}
