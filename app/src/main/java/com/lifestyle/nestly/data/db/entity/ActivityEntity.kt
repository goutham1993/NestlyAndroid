package com.lifestyle.nestly.data.db.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One row per logged event. Only the details block matching [type] is non-null.
 * Volumes are stored in ml, weights in kg and lengths in cm.
 */
@Entity(
    tableName = "activity",
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
        ForeignKey(
            entity = CaregiverEntity::class,
            parentColumns = ["id"],
            childColumns = ["createdById"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = CaregiverEntity::class,
            parentColumns = ["id"],
            childColumns = ["updatedById"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index(value = ["profileId", "type", "startTime"]),
        Index(value = ["familyId", "startTime"]),
        Index("createdById"),
        Index("updatedById"),
    ],
)
data class ActivityEntity(
    @PrimaryKey val id: String,
    val familyId: String,
    /** Null for activities that belong to the caregiver rather than a child, such as pumping. */
    val profileId: String?,
    val type: ActivityType,
    val startTime: Long,
    val endTime: Long? = null,
    val timeZone: String,
    val note: String? = null,
    val createdById: String? = null,
    val updatedById: String? = null,
    @Embedded(prefix = "bottle_") val bottle: BottleDetails? = null,
    @Embedded(prefix = "pump_") val pump: PumpDetails? = null,
    @Embedded(prefix = "breast_") val breastfeed: BreastfeedDetails? = null,
    @Embedded(prefix = "diaper_") val diaper: DiaperDetails? = null,
    @Embedded(prefix = "growth_") val growth: GrowthDetails? = null,
    @Embedded(prefix = "milestone_") val milestone: MilestoneDetails? = null,
    @Embedded(prefix = "solids_") val solids: SolidsDetails? = null,
    @Embedded(prefix = "routine_") val routine: RoutineDetails? = null,
    @Embedded(prefix = "babyFirst_") val babyFirst: BabyFirstDetails? = null,
    @Embedded(prefix = "medical_") val medical: MedicalDetails? = null,
    @Embedded(prefix = "vaccine_") val vaccine: VaccineDetails? = null,
    /** Path of a photo copied into app storage. */
    val photoUri: String? = null,
    /** Shared by the breastfeed and bottle entries saved together from one Combo Feed. */
    val comboGroupId: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
    val syncState: SyncState = SyncState.PENDING_UPLOAD,
)

data class BottleDetails(
    /** Null when the source did not say what was in the bottle. */
    val milkType: MilkType?,
    val breastMilkMl: Double? = null,
    val formulaMl: Double? = null,
    val formulaName: String? = null,
    val totalMl: Double? = null,
)

data class PumpDetails(
    val durationSec: Long,
    val leftMl: Double? = null,
    val rightMl: Double? = null,
    val totalMl: Double? = null,
)

data class BreastfeedDetails(
    val beginSide: BreastSide?,
    val endSide: BreastSide?,
    val leftDurationSec: Long,
    val rightDurationSec: Long,
    val isManualEntry: Boolean,
)

data class DiaperDetails(
    val diaperType: DiaperType,
    val color: StoolColor? = null,
    val texture: StoolTexture? = null,
    val detail: String? = null,
    val hasRash: Boolean? = null,
)

data class GrowthDetails(
    val weightKg: Double? = null,
    val heightCm: Double? = null,
    val headCm: Double? = null,
)

data class MilestoneDetails(
    val title: String,
    val milestoneKey: String? = null,
)

data class SolidsDetails(
    val mealType: MealType? = null,
    val foods: List<String> = emptyList(),
)

data class RoutineDetails(val name: String)

data class BabyFirstDetails(val title: String)

data class MedicalDetails(
    val temperatureC: Double? = null,
    val medicationName: String? = null,
    val dose: Double? = null,
    val doseUnit: String? = null,
)

data class VaccineDetails(val vaccines: List<String>)
