package com.lifestyle.nestly.ui.components

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.unit.dp
import com.lifestyle.nestly.appContainer
import com.lifestyle.nestly.ui.theme.NestlyColors
import com.lifestyle.nestly.ui.theme.NestlyType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/** Shared child photo. A new pick is a local file; an existing photo stays a storage path until save. */
@Composable
fun ChildPhotoField(photoPath: String?, onChange: (String?) -> Unit) {
    val context = LocalContext.current
    val photos = context.appContainer.photos
    val scope = rememberCoroutineScope()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch { copyChildPhoto(context, uri)?.let(onChange) }
    }
    var bitmap by remember(photoPath) { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    LaunchedEffect(photoPath) {
        bitmap = null
        val path = photoPath ?: return@LaunchedEffect
        val local = if (File(path).exists()) path else runCatching { photos.resolveProfileToLocal(path) }.getOrNull()
        bitmap = local?.let { file ->
            withContext(Dispatchers.IO) { BitmapFactory.decodeFile(file)?.asImageBitmap() }
        }
    }

    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        val image = bitmap
        if (image != null) {
            Image(
                image,
                contentDescription = "Child photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(64.dp).clip(CircleShape),
            )
        } else {
            MonogramAvatar(name = "", size = 64.dp)
        }
        if (photoPath == null) {
            TextButton(onClick = {
                picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }) {
                Text("Add Photo", color = NestlyColors.AccentText, style = NestlyType.BodyMedium)
            }
        } else {
            TextButton(onClick = { onChange(null) }) {
                Text("Remove Photo", color = NestlyColors.AccentText, style = NestlyType.BodyMedium)
            }
        }
    }
}

private suspend fun copyChildPhoto(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
    val dir = File(context.filesDir, "photos").apply { mkdirs() }
    val file = File(dir, "${UUID.randomUUID()}.jpg")
    context.contentResolver.openInputStream(uri)?.use { input ->
        file.outputStream().use { input.copyTo(it) }
        file.absolutePath
    }
}
