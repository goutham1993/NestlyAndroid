package com.lifestyle.nestly.ui.appointments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifestyle.nestly.AppContainer
import com.lifestyle.nestly.data.model.AppointmentEntity
import com.lifestyle.nestly.data.model.AppointmentReminderLead
import com.lifestyle.nestly.data.model.AppointmentWithQuestions
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class AppointmentsViewModel(private val container: AppContainer) : ViewModel() {

    val appointments: StateFlow<List<AppointmentWithQuestions>> =
        container.family.selectedProfile.flatMapLatest { profile ->
            if (profile == null) flowOf(emptyList())
            else container.appointments.observeForProfile(profile.familyId, profile.id)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun appointment(id: String): Flow<AppointmentWithQuestions?> =
        container.appointments.observeById(id)

    fun save(
        id: String?,
        title: String,
        doctorName: String?,
        location: String?,
        startsAt: Long,
        reminderLead: AppointmentReminderLead,
        notes: String?,
        onDone: () -> Unit,
        onError: (String) -> Unit,
    ) = viewModelScope.launch {
        try {
            val existing = id?.let { container.appointments.getById(it) }
            val saved = if (existing != null) {
                container.appointments.save(
                    existing.copy(
                        title = title.trim(),
                        doctorName = doctorName?.trim()?.ifBlank { null },
                        location = location?.trim()?.ifBlank { null },
                        startsAt = startsAt,
                        timeZone = ZoneId.systemDefault().id,
                        notes = notes?.trim()?.ifBlank { null },
                        reminderLead = reminderLead,
                    ),
                )
            } else {
                val profile = container.family.selectedProfile.first()
                    ?: error("Add a child first")
                container.appointments.save(
                    AppointmentEntity(
                        id = UUID.randomUUID().toString(),
                        familyId = profile.familyId,
                        profileId = profile.id,
                        title = title.trim(),
                        doctorName = doctorName?.trim()?.ifBlank { null },
                        location = location?.trim()?.ifBlank { null },
                        startsAt = startsAt,
                        timeZone = ZoneId.systemDefault().id,
                        notes = notes?.trim()?.ifBlank { null },
                        reminderLead = reminderLead,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis(),
                    ),
                )
            }
            // scheduleAppointment cancels instead when the lead is NONE or the time has passed.
            container.scheduler.scheduleAppointment(saved)
            onDone()
        } catch (e: Exception) {
            onError(e.message ?: "Couldn't save the appointment.")
        }
    }

    fun delete(appointment: AppointmentEntity, onDone: () -> Unit, onError: (String) -> Unit) =
        viewModelScope.launch {
            try {
                container.appointments.delete(appointment.id)
                container.scheduler.cancelAppointment(appointment.id)
                onDone()
            } catch (e: Exception) {
                onError(e.message ?: "Couldn't delete the appointment.")
            }
        }
}
