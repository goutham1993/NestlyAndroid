package com.lifestyle.nestly.ui.importer

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifestyle.nestly.ui.activity.displayName
import com.lifestyle.nestly.ui.components.PillButton
import com.lifestyle.nestly.ui.components.RowDivider
import com.lifestyle.nestly.ui.theme.NestlyColors
import com.lifestyle.nestly.ui.theme.NestlyType
import java.time.LocalDate

private val CSV_MIME_TYPES = arrayOf("text/csv", "text/comma-separated-values", "application/csv", "text/plain")

@Composable
fun ImportScreen(viewModel: ImportViewModel, modifier: Modifier = Modifier) {
    val status by viewModel.status.collectAsStateWithLifecycle()
    val exportStatus by viewModel.exportStatus.collectAsStateWithLifecycle()
    val counts by viewModel.counts.collectAsStateWithLifecycle()

    val naraPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(viewModel::importNara)
    }
    val nestlyPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(viewModel::importNestly)
    }
    val exportPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri ->
        uri?.let(viewModel::exportNestly)
    }

    Column(
        modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
    ) {
        Column(
            Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "Import a Nara Baby export, or back up Nestly to a CSV you can re-import later.",
                style = NestlyType.Body,
                color = NestlyColors.TextSecondary,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                PillButton("Import from Nara", onClick = {
                    if (status != ImportStatus.Running) naraPicker.launch(CSV_MIME_TYPES)
                })
                if (status == ImportStatus.Running) {
                    Spacer(Modifier.width(16.dp))
                    CircularProgressIndicator(Modifier.size(24.dp), color = NestlyColors.AccentText)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PillButton("Export Nestly CSV", onClick = {
                    if (exportStatus != ExportStatus.Running) {
                        exportPicker.launch("nestly_export_${LocalDate.now()}.csv")
                    }
                })
                PillButton("Import Nestly CSV", onClick = {
                    if (status != ImportStatus.Running) nestlyPicker.launch(CSV_MIME_TYPES)
                })
            }
            if (exportStatus == ExportStatus.Running) {
                CircularProgressIndicator(Modifier.size(24.dp), color = NestlyColors.AccentText)
            }
            when (val s = status) {
                is ImportStatus.Done -> Text(
                    "Imported ${s.result.totalActivities} activities, ${s.result.profiles} profile(s), " +
                        "${s.result.caregivers} caregiver(s) from ${s.source}. Skipped ${s.result.skippedRows} row(s).",
                    style = NestlyType.Body,
                )
                is ImportStatus.Failed -> Text("Import failed: ${s.message}", color = NestlyColors.Danger)
                else -> Unit
            }
            when (val s = exportStatus) {
                is ExportStatus.Done -> Text(
                    "Exported ${s.result.activities} activities, ${s.result.profiles} profile(s), " +
                        "${s.result.caregivers} caregiver(s).",
                    style = NestlyType.Body,
                )
                is ExportStatus.Failed -> Text("Export failed: ${s.message}", color = NestlyColors.Danger)
                else -> Unit
            }
        }
        Text(
            "Activities in Nestly",
            style = NestlyType.SectionTitle,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
        )
        RowDivider()
        if (counts.isEmpty()) {
            Text("None yet", style = NestlyType.Body, color = NestlyColors.TextSecondary, modifier = Modifier.padding(20.dp))
        }
        counts.sortedByDescending { it.count }.forEach { c ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(NestlyColors.Surface)
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(c.type.displayName, style = NestlyType.RowLabel)
                Text(c.count.toString(), style = NestlyType.Body)
            }
            RowDivider()
        }
    }
}
