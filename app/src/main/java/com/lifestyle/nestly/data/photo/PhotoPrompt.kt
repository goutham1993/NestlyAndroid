package com.lifestyle.nestly.data.photo

import java.time.LocalDate

/**
 * When to ask for a new shared child photo. Every month of the first year,
 * then each birthday. Twelve months is the first birthday, so it is not asked twice.
 */
object PhotoPrompt {
    data class Milestone(
        val id: String,
        val date: LocalDate,
        /** "6 months" or "2 years", for "Rithan is 6 months old". */
        val phrase: String,
    )

    data class Child(
        val id: String,
        val name: String,
        val birthDate: LocalDate?,
        val photoPath: String? = null,
        /** Calendar day of photo_updated_at. Null when the photo has never been saved. */
        val photoUpdatedOn: LocalDate? = null,
        /** Milestone id this device already skipped. */
        val dismissedMilestoneId: String? = null,
    )

    data class Due(
        val profileId: String,
        val name: String,
        val photoPath: String?,
        val milestone: Milestone,
    ) {
        val hasPhoto: Boolean get() = photoPath != null
        val title: String get() = "$name is ${milestone.phrase} old"
        val message: String
            get() = if (hasPhoto) {
                "Add a new photo so the family can see the latest $name."
            } else {
                "Add a photo of $name for the family."
            }
    }

    private val monthMarks = 1..12

    /** Latest milestone on or before [today]. Null before the first month, and null without a usable birth date. */
    fun current(birth: LocalDate, today: LocalDate): Milestone? {
        var best: Milestone? = null
        for (months in monthMarks) {
            val date = birth.plusMonths(months.toLong())
            if (date.isAfter(today)) continue
            best = Milestone(
                id = "m$months",
                date = date,
                phrase = if (months == 1) "1 month" else "$months months",
            )
        }
        var year = 2
        while (year < 150) {
            val date = birth.plusYears(year.toLong())
            if (date.isAfter(today)) break
            best = Milestone(id = "y$year", date = date, phrase = "$year years")
            year += 1
        }
        return best
    }

    /** The milestone to ask about, or null when it is too early, already dismissed, or the photo is current. */
    fun due(
        birth: LocalDate,
        photoUpdatedOn: LocalDate?,
        dismissedMilestoneId: String?,
        today: LocalDate,
    ): Milestone? {
        val milestone = current(birth, today) ?: return null
        if (dismissedMilestoneId == milestone.id) return null
        if (photoUpdatedOn != null && !photoUpdatedOn.isBefore(milestone.date)) return null
        return milestone
    }

    /** One child. The selected child wins when they are due; otherwise the most recent milestone. */
    fun choose(children: List<Child>, selectedId: String?, today: LocalDate): Due? {
        val ready = children.mapNotNull { child ->
            val birth = child.birthDate ?: return@mapNotNull null
            val milestone = due(birth, child.photoUpdatedOn, child.dismissedMilestoneId, today) ?: return@mapNotNull null
            Due(child.id, child.name, child.photoPath, milestone)
        }
        ready.firstOrNull { it.profileId == selectedId }?.let { return it }
        return ready.maxByOrNull { it.milestone.date }
    }
}
