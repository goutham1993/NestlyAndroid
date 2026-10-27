package com.lifestyle.nestly.data.importer

import androidx.room.withTransaction
import com.lifestyle.nestly.data.db.NestlyDatabase
import java.io.InputStream

class NestlyCsvImporter(
    private val db: NestlyDatabase,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    suspend fun import(input: InputStream): ImportResult {
        val rows = input.bufferedReader(Charsets.UTF_8).use(CsvParser::parseRecords)
        val now = clock()
        var families = 0
        var caregivers = 0
        var profiles = 0
        var skipped = 0
        val activityCounts = mutableMapOf<com.lifestyle.nestly.data.db.entity.ActivityType, Int>()

        db.withTransaction {
            rows.forEach { row ->
                when (row["record"]?.uppercase()) {
                    "FAMILY" -> {
                        db.familyDao().upsertFamily(NestlyCsv.parseFamily(row, now))
                        families++
                    }
                    "CAREGIVER" -> {
                        db.familyDao().upsertCaregiver(NestlyCsv.parseCaregiver(row, now))
                        caregivers++
                    }
                    "PROFILE" -> {
                        db.profileDao().upsert(NestlyCsv.parseProfile(row, now))
                        profiles++
                    }
                    "ACTIVITY" -> {
                        val activity = NestlyCsv.parseActivity(row, now)
                        db.activityDao().upsert(activity)
                        activityCounts[activity.type] = (activityCounts[activity.type] ?: 0) + 1
                    }
                    else -> skipped++
                }
            }
        }
        return ImportResult(
            activityCounts = activityCounts,
            profiles = profiles,
            caregivers = caregivers,
            skippedRows = skipped,
        )
    }
}
