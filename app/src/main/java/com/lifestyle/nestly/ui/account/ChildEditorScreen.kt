package com.lifestyle.nestly.ui.account

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lifestyle.nestly.data.model.ProfileEntity
import com.lifestyle.nestly.data.model.Sex
import com.lifestyle.nestly.ui.components.DateTimePickerDialog
import com.lifestyle.nestly.ui.components.DetailScreen
import com.lifestyle.nestly.ui.components.Format
import com.lifestyle.nestly.ui.components.MonogramAvatar
import com.lifestyle.nestly.ui.components.NestlySwitch
import com.lifestyle.nestly.ui.components.RowDivider
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
    var hasBirthDate by remember { mutableStateOf(initial == null || initial.birthDate != null) }
    var birthDate by remember { mutableStateOf(initial?.birthDate ?: LocalDate.now()) }
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

    Box(Modifier.fillMaxSize()) {
        DetailScreen(
            title = title,
            onBack = onBack,
            action = if (valid) if (initial == null) "Add" else "Save" else null,
            onAction = {
                onSave(name.trim(), sex, birthDate.takeIf { hasBirthDate }, caregiver.trim(), photoPath)
            },
            centerTitle = true,
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(8.dp))
                MonogramAvatar(name.ifBlank { " " }, size = 72.dp, photoPath = photoPath)
                Spacer(Modifier.height(12.dp))
                Text(
                    name.ifBlank { "Your little one" },
                    style = NestlyType.CardTitle,
                    color = if (name.isBlank()) NestlyColors.TextSecondary else NestlyColors.TextPrimary,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(4.dp))
                if (readingPhoto) {
                    CircularProgressIndicator(color = NestlyColors.Accent, modifier = Modifier.padding(8.dp))
                } else if (photoPath == null) {
                    PhotoAction("Add Photo", Icons.Outlined.PhotoCamera, NestlyColors.AccentText) {
                        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        PhotoAction("Change Photo", Icons.Outlined.PhotoCamera, NestlyColors.AccentText) {
                            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }
                        PhotoAction("Remove Photo", Icons.Outlined.Delete, NestlyColors.Danger) {
                            photoPath = null
                        }
                    }
                }
                Text(
                    "Optional. Anyone in the family can change this later.",
                    style = NestlyType.Small,
                    color = NestlyColors.TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp, vertical = 4.dp),
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
                InsetGroup {
                    GroupedField(name, { name = it }, "First Name")
                    RowDivider(Modifier.padding(start = 16.dp))
                    SegmentedToggle(
                        options = listOf("Boy", "Girl"),
                        selected = when (sex) {
                            Sex.MALE -> 0
                            Sex.FEMALE -> 1
                            else -> -1
                        },
                        onSelect = { sex = if (it == 0) Sex.MALE else Sex.FEMALE },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    )
                }
                Spacer(Modifier.height(20.dp))
                InsetGroup {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 44.dp)
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Birthdate", style = NestlyType.Body, color = NestlyColors.TextPrimary, modifier = Modifier.weight(1f))
                        NestlySwitch(hasBirthDate, { hasBirthDate = it })
                    }
                    if (hasBirthDate) {
                        RowDivider(Modifier.padding(start = 16.dp))
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .heightIn(min = 44.dp)
                                .clickable { pickingDate = true }
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("Birthdate", style = NestlyType.Body, color = NestlyColors.TextPrimary, modifier = Modifier.weight(1f))
                            Text(
                                Format.date(birthDate),
                                style = NestlyType.Body,
                                color = NestlyColors.TextPrimary,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(NestlyColors.Tertiary)
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                            )
                        }
                    }
                }
                if (askCaregiverName) {
                    Spacer(Modifier.height(20.dp))
                    InsetGroup {
                        GroupedField(caregiver, { caregiver = it }, "Your Name")
                    }
                    Text(
                        "Shown to other caregivers next to the entries you log.",
                        style = NestlyType.Small,
                        color = NestlyColors.TextSecondary,
                        modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp),
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
            initial = birthDate.atStartOfDay(zone).toInstant().toEpochMilli(),
            dateOnly = true,
            onDismiss = { pickingDate = false },
            onConfirm = {
                birthDate = Instant.ofEpochMilli(it).atZone(zone).toLocalDate()
                pickingDate = false
            },
        )
    }
}

/** Inset grouped card, matching an iOS Form section. */
@Composable
private fun InsetGroup(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(NestlyColors.Surface),
        content = content,
    )
}

@Composable
private fun GroupedField(value: String, onValueChange: (String) -> Unit, placeholder: String) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = NestlyType.Body.copy(color = NestlyColors.TextPrimary),
        cursorBrush = SolidColor(NestlyColors.AccentText),
        singleLine = true,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .padding(horizontal = 16.dp, vertical = 11.dp),
        decorationBox = { inner ->
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty()) {
                    Text(placeholder, style = NestlyType.Body, color = NestlyColors.TextMuted)
                }
                inner()
            }
        },
    )
}

@Composable
private fun PhotoAction(text: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    Row(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        Text(text, color = color, style = NestlyType.Button)
    }
}
