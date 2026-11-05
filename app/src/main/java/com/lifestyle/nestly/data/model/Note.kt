package com.lifestyle.nestly.data.model

/**
 * A free-form parent note for one child. [body] is plain text with inline list
 * markers (see AppointmentNotesFormat), so checklist state lives in the text.
 */
data class NoteEntity(
    val id: String,
    val familyId: String,
    val profileId: String,
    val title: String = "",
    val body: String = "",
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
    /** Optimistic-locking version; updates must match and increment this. */
    val version: Int = 1,
)
