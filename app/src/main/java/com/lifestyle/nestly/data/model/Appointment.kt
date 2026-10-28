package com.lifestyle.nestly.data.model

/** How long before an appointment its local reminder fires. [leadMinutes] null = no reminder. */
enum class AppointmentReminderLead(val leadMinutes: Int?, val label: String) {
    NONE(null, "No reminder"),
    AT_TIME(0, "At appointment time"),
    ONE_HOUR(60, "1 hour before"),
    ONE_DAY(1440, "1 day before"),
    ;

    companion object {
        fun fromLeadMinutes(minutes: Int?): AppointmentReminderLead =
            entries.find { it.leadMinutes == minutes } ?: NONE
    }
}

data class AppointmentEntity(
    val id: String,
    val familyId: String,
    val profileId: String,
    val title: String,
    val doctorName: String? = null,
    val location: String? = null,
    /** Appointment time as epoch millis; DTO layer converts to/from timestamptz. */
    val startsAt: Long,
    val timeZone: String,
    val notes: String? = null,
    val reminderLead: AppointmentReminderLead = AppointmentReminderLead.NONE,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
    /** Optimistic-locking version; updates must match and increment this. */
    val version: Int = 1,
)

data class AppointmentQuestionEntity(
    val id: String,
    val appointmentId: String,
    val familyId: String,
    val text: String,
    val isDone: Boolean = false,
    val sortOrder: Int = 0,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
    val version: Int = 1,
)

/** An appointment together with its (non-deleted) questions, ordered for display. */
data class AppointmentWithQuestions(
    val appointment: AppointmentEntity,
    val questions: List<AppointmentQuestionEntity> = emptyList(),
) {
    val askedCount: Int get() = questions.count { it.isDone }
}
