package com.lifestyle.nestly.data.model

data class ReminderEntity(
    val id: String,
    val familyId: String,
    /** The caregiver who owns this reminder (reminders are personal in Supabase). */
    val caregiverId: String = "",
    /** Null for pump reminders, which are caregiver/family-level. */
    val profileId: String?,
    val kind: ReminderKind,
    val intervalMinutes: Int,
    val enabled: Boolean,
    val updatedAt: Long,
    val version: Int = 1,
)
