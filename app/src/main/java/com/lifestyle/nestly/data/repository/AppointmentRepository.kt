package com.lifestyle.nestly.data.repository

import com.lifestyle.nestly.data.model.AppointmentEntity
import com.lifestyle.nestly.data.model.AppointmentQuestionEntity
import com.lifestyle.nestly.data.model.AppointmentReminderLead
import com.lifestyle.nestly.data.model.AppointmentWithQuestions
import com.lifestyle.nestly.data.supabase.AppointmentDto
import com.lifestyle.nestly.data.supabase.AppointmentQuestionDto
import com.lifestyle.nestly.data.supabase.ConflictException
import com.lifestyle.nestly.data.supabase.RefreshBus
import com.lifestyle.nestly.data.supabase.refetchFlow
import com.lifestyle.nestly.data.supabase.toDto
import com.lifestyle.nestly.data.supabase.toIso
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.util.UUID

class AppointmentRepository(
    private val client: SupabaseClient,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    /** Appointments for one child with their questions, soonest first. */
    fun observeForProfile(familyId: String, profileId: String): Flow<List<AppointmentWithQuestions>> =
        combine(
            refetchFlow(RefreshBus.APPOINTMENT) { fetchForProfile(familyId, profileId) },
            refetchFlow(RefreshBus.APPOINTMENT_QUESTION) { fetchQuestions(familyId) },
        ) { appointments, questions ->
            val byAppointment = questions.groupBy { it.appointmentId }
            appointments.map { AppointmentWithQuestions(it, byAppointment[it.id].orEmpty()) }
        }

    fun observeById(id: String): Flow<AppointmentWithQuestions?> =
        combine(
            refetchFlow(RefreshBus.APPOINTMENT) { getById(id) },
            refetchFlow(RefreshBus.APPOINTMENT_QUESTION) { fetchQuestionsForAppointment(id) },
        ) { appointment, questions ->
            appointment?.let { AppointmentWithQuestions(it, questions) }
        }

    suspend fun fetchForProfile(familyId: String, profileId: String): List<AppointmentEntity> =
        client.from("appointment").select {
            filter {
                eq("family_id", familyId)
                eq("profile_id", profileId)
                filter("deleted_at", FilterOperator.IS, null)
            }
            order("starts_at", Order.ASCENDING)
        }.decodeList<AppointmentDto>().map { it.toEntity() }

    suspend fun fetchQuestions(familyId: String): List<AppointmentQuestionEntity> =
        client.from("appointment_question").select {
            filter {
                eq("family_id", familyId)
                filter("deleted_at", FilterOperator.IS, null)
            }
            order("sort_order", Order.ASCENDING)
            order("created_at", Order.ASCENDING)
        }.decodeList<AppointmentQuestionDto>().map { it.toEntity() }

    suspend fun fetchQuestionsForAppointment(appointmentId: String): List<AppointmentQuestionEntity> =
        client.from("appointment_question").select {
            filter {
                eq("appointment_id", appointmentId)
                filter("deleted_at", FilterOperator.IS, null)
            }
            order("sort_order", Order.ASCENDING)
            order("created_at", Order.ASCENDING)
        }.decodeList<AppointmentQuestionDto>().map { it.toEntity() }

    suspend fun getById(id: String): AppointmentEntity? =
        client.from("appointment").select {
            filter { eq("id", id) }
            limit(1)
        }.decodeSingleOrNull<AppointmentDto>()?.toEntity()?.takeIf { it.deletedAt == null }

    /** All future appointments that want a reminder, across the signed-in user's families (RLS-scoped). */
    suspend fun fetchUpcomingWithReminders(now: Long = clock()): List<AppointmentEntity> =
        client.from("appointment").select {
            filter {
                filter("deleted_at", FilterOperator.IS, null)
                gte("starts_at", now.toIso())
            }
            order("starts_at", Order.ASCENDING)
        }.decodeList<AppointmentDto>().map { it.toEntity() }
            .filter { it.reminderLead != AppointmentReminderLead.NONE }

    /** Inserts a new appointment or version-checked-updates an existing one. Returns the saved row. */
    suspend fun save(appointment: AppointmentEntity): AppointmentEntity {
        val now = clock()
        val existing = getById(appointment.id)
        val saved = if (existing == null) {
            val created = appointment.copy(createdAt = now, updatedAt = now)
            client.from("appointment").insert(created.toDto())
            created
        } else {
            val updated = appointment.copy(
                createdAt = existing.createdAt,
                updatedAt = now,
                version = existing.version + 1,
            )
            client.from("appointment").update(updated.toDto()) {
                filter {
                    eq("id", appointment.id)
                    eq("version", existing.version)
                }
            }
            val fresh = getById(appointment.id)
            if (fresh == null || fresh.version != updated.version) throw ConflictException()
            updated
        }
        RefreshBus.invalidate(RefreshBus.APPOINTMENT)
        return saved
    }

    suspend fun delete(id: String) {
        val existing = getById(id) ?: return
        val deleted = existing.copy(deletedAt = clock(), updatedAt = clock(), version = existing.version + 1)
        client.from("appointment").update(deleted.toDto()) {
            filter {
                eq("id", id)
                eq("version", existing.version)
            }
        }
        RefreshBus.invalidate(RefreshBus.APPOINTMENT)
    }

    suspend fun addQuestion(appointment: AppointmentEntity, text: String): AppointmentQuestionEntity {
        val trimmed = text.trim()
        require(trimmed.isNotEmpty()) { "Question text is blank" }
        val now = clock()
        val existing = fetchQuestionsForAppointment(appointment.id)
        val question = AppointmentQuestionEntity(
            id = UUID.randomUUID().toString(),
            appointmentId = appointment.id,
            familyId = appointment.familyId,
            text = trimmed,
            sortOrder = (existing.maxOfOrNull { it.sortOrder } ?: -1) + 1,
            createdAt = now,
            updatedAt = now,
        )
        client.from("appointment_question").insert(question.toDto())
        RefreshBus.invalidate(RefreshBus.APPOINTMENT_QUESTION)
        return question
    }

    suspend fun setQuestionDone(question: AppointmentQuestionEntity, done: Boolean) {
        val updated = question.copy(isDone = done, updatedAt = clock(), version = question.version + 1)
        client.from("appointment_question").update(updated.toDto()) {
            filter {
                eq("id", question.id)
                eq("version", question.version)
            }
        }
        RefreshBus.invalidate(RefreshBus.APPOINTMENT_QUESTION)
    }

    suspend fun deleteQuestion(question: AppointmentQuestionEntity) {
        val deleted = question.copy(deletedAt = clock(), updatedAt = clock(), version = question.version + 1)
        client.from("appointment_question").update(deleted.toDto()) {
            filter {
                eq("id", question.id)
                eq("version", question.version)
            }
        }
        RefreshBus.invalidate(RefreshBus.APPOINTMENT_QUESTION)
    }
}
