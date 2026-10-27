package com.lifestyle.nestly.data.model

/**
 * One row per logged event. Only the details block matching [type] is non-null.
 * Volumes are stored in ml, weights in kg and lengths in cm.
 * Times are epoch millis in the app; the DTO layer converts to/from timestamptz.
 */
data class ActivityEntity(
    val id: String,
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
    val bottle: BottleDetails? = null,
    val pump: PumpDetails? = null,
    val breastfeed: BreastfeedDetails? = null,
    val diaper: DiaperDetails? = null,
    val growth: GrowthDetails? = null,
    val milestone: MilestoneDetails? = null,
    val solids: SolidsDetails? = null,
    val routine: RoutineDetails? = null,
    val babyFirst: BabyFirstDetails? = null,
    val medical: MedicalDetails? = null,
    val vaccine: VaccineDetails? = null,
    /** Storage path in the activity-photos bucket, or a local file path before upload. */
    val photoUri: String? = null,
    /** Shared by the breastfeed and bottle entries saved together from one Combo Feed. */
    val comboGroupId: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
    /** Optimistic-locking version; updates must match and increment this. */
    val version: Int = 1,
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

data class TypeCount(val type: ActivityType, val count: Int)

data class DailyVolume(val day: String, val totalMl: Double, val count: Int)
