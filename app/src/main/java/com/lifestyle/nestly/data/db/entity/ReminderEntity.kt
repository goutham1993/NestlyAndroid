package com.lifestyle.nestly.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class ReminderKind { FEED, PUMP, DIAPER, SLEEP }

@Entity(
    tableName = "reminder",
    foreignKeys = [
        ForeignKey(
            entity = FamilyEntity::class,
            parentColumns = ["id"],
            childColumns = ["familyId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["profileId", "kind"], unique = true),
        Index("familyId"),
    ],
)
data class ReminderEntity(
    @PrimaryKey val id: String,
    val familyId: String,
    /** Null for pump reminders, which are caregiver/family-level. */
    val profileId: String?,
    val kind: ReminderKind,
    val intervalMinutes: Int,
    val enabled: Boolean,
    val updatedAt: Long,
)
