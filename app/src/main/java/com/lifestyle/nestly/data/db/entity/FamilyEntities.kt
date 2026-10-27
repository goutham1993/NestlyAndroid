package com.lifestyle.nestly.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "family")
data class FamilyEntity(
    @PrimaryKey val id: String,
    val name: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
    val syncState: SyncState = SyncState.PENDING_UPLOAD,
)

@Entity(
    tableName = "caregiver",
    foreignKeys = [
        ForeignKey(
            entity = FamilyEntity::class,
            parentColumns = ["id"],
            childColumns = ["familyId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["familyId", "name"], unique = true)],
)
data class CaregiverEntity(
    @PrimaryKey val id: String,
    val familyId: String,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
    val syncState: SyncState = SyncState.PENDING_UPLOAD,
)

@Entity(
    tableName = "profile",
    foreignKeys = [
        ForeignKey(
            entity = FamilyEntity::class,
            parentColumns = ["id"],
            childColumns = ["familyId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("familyId")],
)
data class ProfileEntity(
    @PrimaryKey val id: String,
    val familyId: String,
    val name: String,
    val birthDate: LocalDate?,
    val adjustedBirthDate: LocalDate? = null,
    val sex: Sex?,
    val profileType: ProfileType = ProfileType.CHILD,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
    val syncState: SyncState = SyncState.PENDING_UPLOAD,
)
