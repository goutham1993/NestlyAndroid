package com.lifestyle.nestly.data.importer

import com.lifestyle.nestly.data.db.NestlyDatabase
import java.io.OutputStream
import java.io.OutputStreamWriter

class NestlyCsvExporter(private val db: NestlyDatabase) {
    suspend fun export(output: OutputStream): ExportResult {
        val families = db.familyDao().getAllActiveFamilies()
        val caregivers = db.familyDao().getAllActiveCaregivers()
        val profiles = db.profileDao().getAllActive()
        val activities = db.activityDao().getAllActive()
        OutputStreamWriter(output, Charsets.UTF_8).use { writer ->
            writer.append('\uFEFF')
            writer.appendLine(NestlyCsv.HEADER.joinToString(","))
            families.forEach { writer.appendLine(NestlyCsv.familyRow(it)) }
            caregivers.forEach { writer.appendLine(NestlyCsv.caregiverRow(it)) }
            profiles.forEach { writer.appendLine(NestlyCsv.profileRow(it)) }
            activities.forEach { writer.appendLine(NestlyCsv.activityRow(it)) }
        }
        return ExportResult(
            families = families.size,
            caregivers = caregivers.size,
            profiles = profiles.size,
            activities = activities.size,
        )
    }
}

data class ExportResult(
    val families: Int,
    val caregivers: Int,
    val profiles: Int,
    val activities: Int,
)
