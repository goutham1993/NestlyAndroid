package com.lifestyle.nestly.data.importer

import androidx.room.withTransaction
import com.lifestyle.nestly.data.db.NestlyDatabase
import com.lifestyle.nestly.data.db.entity.ActivityEntity
import com.lifestyle.nestly.data.db.entity.ActivityType
import com.lifestyle.nestly.data.db.entity.CaregiverEntity
import com.lifestyle.nestly.data.db.entity.FamilyEntity
import com.lifestyle.nestly.data.db.entity.ProfileEntity
import com.lifestyle.nestly.data.db.entity.ProfileType
import java.io.InputStream
import java.util.UUID

data class ImportResult(
    val activityCounts: Map<ActivityType, Int>,
    val profiles: Int,
    val caregivers: Int,
    val skippedRows: Int,
) {
    val totalActivities: Int get() = activityCounts.values.sum()
}

/**
 * Imports a Nara Baby CSV export. Nara keys are reused as primary keys, so importing the same
 * file again updates rows in place instead of duplicating them.
 */
class NaraCsvImporter(
    private val db: NestlyDatabase,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    suspend fun import(input: InputStream): ImportResult {
        val rows = input.bufferedReader(Charsets.UTF_8).use(CsvParser::parseRecords)
        val records = rows.mapNotNull(NaraRowMapper::map)
        val profiles = records.filterIsInstance<NaraRecord.Profile>()
        val activities = records.filterIsInstance<NaraRecord.Activity>()
        val now = clock()

        val familyDao = db.familyDao()
        val profileDao = db.profileDao()
        val activityDao = db.activityDao()

        var caregiverCount = 0
        db.withTransaction {
            records.map { it.familyKey }.distinct().forEach { familyKey ->
                val existing = familyDao.getFamily(familyKey)
                familyDao.upsertFamily(
                    existing?.copy(updatedAt = now)
                        ?: FamilyEntity(id = familyKey, name = null, createdAt = now, updatedAt = now),
                )
            }

            profiles.forEach { p ->
                val existing = profileDao.get(p.profileKey)
                profileDao.upsert(
                    ProfileEntity(
                        id = p.profileKey,
                        familyId = p.familyKey,
                        name = p.name,
                        birthDate = p.birthDate,
                        adjustedBirthDate = p.adjustedBirthDate,
                        sex = p.sex,
                        profileType = p.profileType,
                        createdAt = existing?.createdAt ?: now,
                        updatedAt = now,
                    ),
                )
            }

            val knownProfiles = profiles.map { it.profileKey }.toMutableSet()
            activities.filter { it.profileKey != null && it.profileKey !in knownProfiles }
                .distinctBy { it.profileKey }
                .forEach { a ->
                    val key = a.profileKey!!
                    if (profileDao.get(key) == null) {
                        profileDao.upsert(
                            ProfileEntity(
                                id = key,
                                familyId = a.familyKey,
                                name = a.profileName ?: "Baby",
                                birthDate = null,
                                sex = null,
                                profileType = ProfileType.CHILD,
                                createdAt = now,
                                updatedAt = now,
                            ),
                        )
                    }
                    knownProfiles += key
                }

            val caregiverIds = mutableMapOf<Pair<String, String>, String>()
            activities.flatMap { a -> listOfNotNull(a.createdBy, a.updatedBy).map { a.familyKey to it } }
                .distinct()
                .forEach { (familyKey, name) ->
                    val existing = familyDao.findCaregiverByName(familyKey, name)
                    val id = existing?.id ?: UUID.randomUUID().toString().also {
                        familyDao.upsertCaregiver(
                            CaregiverEntity(id = it, familyId = familyKey, name = name, createdAt = now, updatedAt = now),
                        )
                    }
                    caregiverIds[familyKey to name] = id
                }
            caregiverCount = caregiverIds.size

            activities.chunked(500).forEach { chunk ->
                activityDao.upsertAll(
                    chunk.map { a ->
                        ActivityEntity(
                            id = a.activityKey,
                            familyId = a.familyKey,
                            profileId = a.profileKey,
                            type = a.type,
                            startTime = a.startTime,
                            endTime = a.endTime,
                            timeZone = a.timeZone,
                            note = a.note,
                            createdById = a.createdBy?.let { caregiverIds[a.familyKey to it] },
                            updatedById = a.updatedBy?.let { caregiverIds[a.familyKey to it] },
                            bottle = a.bottle,
                            pump = a.pump,
                            breastfeed = a.breastfeed,
                            diaper = a.diaper,
                            growth = a.growth,
                            milestone = a.milestone,
                            createdAt = a.startTime,
                            updatedAt = now,
                        )
                    },
                )
            }
        }

        return ImportResult(
            activityCounts = activities.groupingBy { it.type }.eachCount(),
            profiles = profiles.size,
            caregivers = caregiverCount,
            skippedRows = rows.size - records.size,
        )
    }
}
