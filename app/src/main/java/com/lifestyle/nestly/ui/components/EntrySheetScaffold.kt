package com.lifestyle.nestly.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lifestyle.nestly.ui.theme.NestlyColors
import com.lifestyle.nestly.ui.theme.NestlyType

private val SheetShape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)

/** Full-height bottom sheet with Nara's colored header: X, serif title, and Save. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntrySheet(
    title: String,
    headerColor: Color,
    onDismiss: () -> Unit,
    onSave: (() -> Unit)?,
    saveEnabled: Boolean = true,
    onDelete: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier.statusBarsPadding(),
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = SheetShape,
        containerColor = NestlyColors.Surface,
        dragHandle = null,
        contentWindowInsets = { androidx.compose.foundation.layout.WindowInsets(0) },
    ) {
        Column(Modifier.fillMaxSize()) {
            SheetHeader(title, headerColor, onDismiss, onSave, saveEnabled)
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .navigationBarsPadding(),
            ) {
                content()
                if (onDelete != null) {
                    Spacer(Modifier.height(24.dp))
                    Text(
                        "Delete",
                        style = NestlyType.BodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = NestlyColors.Danger,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .clickable(onClick = onDelete)
                            .padding(16.dp),
                    )
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun SheetHeader(
    title: String,
    color: Color,
    onClose: () -> Unit,
    onSave: (() -> Unit)?,
    saveEnabled: Boolean = true,
    saveLabel: String = "Save",
) {
    val fg = if (color == Color.Transparent) NestlyColors.TextPrimary else NestlyColors.HeaderText
    Box(
        Modifier
            .fillMaxWidth()
            .background(color)
            .height(80.dp),
    ) {
        IconButton(onClick = onClose, modifier = Modifier.align(Alignment.CenterStart).padding(start = 6.dp)) {
            Icon(Icons.Outlined.Close, contentDescription = "Close", tint = fg)
        }
        Text(title, style = NestlyType.SheetTitle, color = fg, modifier = Modifier.align(Alignment.Center))
        if (onSave != null) {
            Text(
                saveLabel,
                style = NestlyType.BodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = fg.copy(alpha = if (saveEnabled) 1f else 0.4f),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .clickable(enabled = saveEnabled, onClick = onSave)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
            )
        }
    }
}
