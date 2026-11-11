package com.lifestyle.nestly.ui.account

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lifestyle.nestly.data.model.ProfileEntity
import com.lifestyle.nestly.data.model.Sex
import com.lifestyle.nestly.ui.components.DateTimePickerDialog
import com.lifestyle.nestly.ui.components.DetailScreen
import com.lifestyle.nestly.ui.components.Format
import com.lifestyle.nestly.ui.components.FormRow
import com.lifestyle.nestly.ui.components.MonogramAvatar
import com.lifestyle.nestly.ui.components.SegmentedToggle
import com.lifestyle.nestly.ui.photo.PhotoCropScreen
import com.lifestyle.nestly.ui.photo.PhotoImages
import com.lifestyle.nestly.ui.theme.NestlyColors
import com.lifestyle.nestly.ui.theme.NestlyType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Full-screen add/edit child form, including change, remove, and crop for the shared photo. */
@Composable
fun ChildEditorScreen(
    title: String,
    initial: ProfileEntity?,
    askCaregiverName: Boolean,
    onBack: () -> Unit,
    onSave: (name: String, sex: Sex?, birthDate: LocalDate?, caregiverName: String, photoPath: String?) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var caregiver by remember { mutableStateOf("") }
    var sex by remember { mutableStateOf(initial?.sex) }
    var birthDate by remember { mutableStateOf(initial?.birthDate) }
    var photoPath by remember { mutableStateOf(initial?.photoPath) }
    var pickingDate by remember { mutableStateOf(false) }
    var readingPhoto by remember { mutableStateOf(false) }
    var cropping by remember { mutableStateOf<Bitmap?>(null) }
    var applyingCrop by remember { mutableStateOf(false) }
    var photoError by remember { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        readingPhoto = true
        photoError = null
        scope.launch {
            val bitmap = withContext(Dispatchers.IO) { PhotoImages.decodeUri(context, uri) }
            readingPhoto = false
            if (bitmap == null) photoError = "Couldn't read that photo."
            else cropping = bitmap
        }
    }
    val valid = name.isNotBlank() && (!askCaregiverName || caregiver.isNotBlank())
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = NestlyColors.AccentText,
        unfocusedBorderColor = NestlyColors.Divider,
        cursorColor = NestlyColors.AccentText,
        focusedLabelColor = NestlyColors.AccentText,
        focusedTextColor = NestlyColors.TextPrimary,
        unfocusedTextColor = NestlyColors.TextPrimary,
    )

    Box(Modifier.fillMaxSize()) {
        DetailScreen(
            title = title,
            onBack = onBack,
            action = if (valid) if (initial == null) "Add" else "Save" else null,
            onAction = { onSave(name.trim(), sex, birthDate, caregiver.trim(), photoPath) },
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(12.dp))
                MonogramAvatar(name.ifBlank { " " }, size = 96.dp, photoPath = photoPath)
                Spacer(Modifier.height(12.dp))
                Text(
                    name.ifBlank { "Your little one" },
                    style = NestlyType.CardTitle,
                    color = if (name.isBlank()) NestlyColors.TextSecondary else NestlyColors.TextPrimary,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(8.dp))
                if (readingPhoto) {
                    CircularProgressIndicator(color = NestlyColors.Accent, modifier = Modifier.padding(8.dp))
                } else if (photoPath == null) {
                    TextButton(onClick = {
                        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }) {
                        Text("Add Photo", color = NestlyColors.AccentText, style = NestlyType.Button)
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = {
                            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }) {
                            Text("Change Photo", color = NestlyColors.AccentText, style = NestlyType.Button)
                        }
                        TextButton(onClick = { photoPath = null }) {
                            Text("Remove Photo", color = NestlyColors.Danger, style = NestlyType.Button)
                        }
                    }
                }
                Text(
                    "Optional. Anyone in the family can change this later.",
                    style = NestlyType.Small,
                    color = NestlyColors.TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp),
                )
                photoError?.let {
                    Text(
                        it,
                        style = NestlyType.Small,
                        color = NestlyColors.Danger,
                        modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp),
                    )
                }
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    name,
                    { name = it },
                    label = { Text("First Name") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    colors = fieldColors,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                )
                Spacer(Modifier.height(16.dp))
                SegmentedToggle(
                    options = listOf("Boy", "Girl"),
                    selected = when (sex) {
                        Sex.MALE -> 0
                        Sex.FEMALE -> 1
                        else -> -1
                    },
                    onSelect = { sex = if (it == 0) Sex.MALE else Sex.FEMALE },
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
                FormRow(
                    "Birthdate",
                    value = birthDate?.let(Format::date),
                    action = if (birthDate == null) "Add" else null,
                    onClick = { pickingDate = true },
                )
                if (askCaregiverName) {
                    OutlinedTextField(
                        caregiver,
                        { caregiver = it },
                        label = { Text("Your Name") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        colors = fieldColors,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                    )
                    Text(
                        "Shown to other caregivers next to the entries you log.",
                        style = NestlyType.Small,
                        color = NestlyColors.TextSecondary,
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )
                }
            }
        }

        cropping?.let { source ->
            PhotoCropScreen(
                source = source,
                onCancel = {
                    if (applyingCrop) return@PhotoCropScreen
                    source.recycle()
                    cropping = null
                },
                onUse = { viewSize, scale, offsetX, offsetY ->
                    if (applyingCrop) return@PhotoCropScreen
                    applyingCrop = true
                    scope.launch {
                        val path = runCatching {
                            withContext(Dispatchers.IO) {
                                try {
                                    val cropped = PhotoImages.cropSquare(source, viewSize, scale, offsetX, offsetY)
                                    try {
                                        PhotoImages.stageJpeg(context, cropped)
                                    } finally {
                                        if (cropped !== source) cropped.recycle()
                                    }
                                } finally {
                                    source.recycle()
                                }
                            }
                        }
                        applyingCrop = false
                        path.onSuccess {
                            photoPath = it
                            photoError = null
                            cropping = null
                        }.onFailure {
                            photoError = "Couldn't read that photo."
                            cropping = null
                        }
                    }
                },
            )
        }
    }

    if (pickingDate) {
        val zone = ZoneId.systemDefault()
        DateTimePickerDialog(
            initial = (birthDate ?: LocalDate.now()).atStartOfDay(zone).toInstant().toEpochMilli(),
            dateOnly = true,
            onDismiss = { pickingDate = false },
            onConfirm = {
                birthDate = Instant.ofEpochMilli(it).atZone(zone).toLocalDate()
                pickingDate = false
            },
        )
    }
}
