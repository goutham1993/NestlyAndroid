package com.lifestyle.nestly.data.importer

import com.lifestyle.nestly.data.model.ActivityType
import com.lifestyle.nestly.data.repository.ActivityRepository
import com.lifestyle.nestly.data.repository.FamilyRepository
import java.io.InputStream

class NestlyCsvImporter(
    private val activities: ActivityRepository,
    private val family: FamilyRepository,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    suspend fun import(input: InputStream): ImportResult {
        val rows = input.bufferedReader(Charsets.UTF_8).use(CsvParser::parseRecords)
        val now = clock()
        var profiles = 0
        var caregivers = 0
        var skipped = 0
        val activityCounts = mutableMapOf<ActivityType, Int>()
        val parsedActivities = mutableListOf<com.lifestyle.nestly.data.model.ActivityEntity>()

        rows.forEach { row ->
            when (row["record"]?.uppercase()) {
                "FAMILY" -> family.importFamily(NestlyCsv.parseFamily(row, now))
                "CAREGIVER" -> {
                    family.importCaregiver(NestlyCsv.parseCaregiver(row, now))
                    caregivers++
                }
                "PROFILE" -> {
                    family.importProfile(NestlyCsv.parseProfile(row, now))
                    profiles++
                }
                "ACTIVITY" -> {
                    val activity = NestlyCsv.parseActivity(row, now)
                    parsedActivities += activity
                    activityCounts[activity.type] = (activityCounts[activity.type] ?: 0) + 1
                }
                else -> skipped++
            }
        }
        activities.importAll(parsedActivities)
        return ImportResult(
            activityCounts = activityCounts,
            profiles = profiles,
            caregivers = caregivers,
            skippedRows = skipped,
        )
    }
}
