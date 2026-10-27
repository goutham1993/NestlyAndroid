package com.lifestyle.nestly.data.importer

import com.lifestyle.nestly.data.repository.ActivityRepository
import com.lifestyle.nestly.data.repository.FamilyRepository
import java.io.OutputStream
import java.io.OutputStreamWriter

class NestlyCsvExporter(
    private val activities: ActivityRepository,
    private val family: FamilyRepository,
) {
    suspend fun export(output: OutputStream): ExportResult {
        val families = family.fetchFamilies()
        val profiles = family.fetchProfiles()
        val caregivers = families.flatMap { family.fetchCaregivers(it.id) }
            .distinctBy { it.id }
        val allActivities = families.flatMap { activities.getAllActive(it.id) }
            .distinctBy { it.id }
            .sortedBy { it.startTime }
        OutputStreamWriter(output, Charsets.UTF_8).use { writer ->
            writer.append('\uFEFF')
            writer.appendLine(NestlyCsv.HEADER.joinToString(","))
            families.forEach { writer.appendLine(NestlyCsv.familyRow(it)) }
            caregivers.forEach { writer.appendLine(NestlyCsv.caregiverRow(it)) }
            profiles.forEach { writer.appendLine(NestlyCsv.profileRow(it)) }
            allActivities.forEach { writer.appendLine(NestlyCsv.activityRow(it)) }
        }
        return ExportResult(
            families = families.size,
            caregivers = caregivers.size,
            profiles = profiles.size,
            activities = allActivities.size,
        )
    }
}

data class ExportResult(
    val families: Int,
    val caregivers: Int,
    val profiles: Int,
    val activities: Int,
)
