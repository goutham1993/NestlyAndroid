package com.lifestyle.nestly.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lifestyle.nestly.ui.theme.NestlyColors
import com.lifestyle.nestly.ui.theme.NestlyType

/** Secondary screen with a back arrow, sans title and optional text action ("Save", "Log Out"). */
@Composable
fun DetailScreen(
    title: String,
    onBack: () -> Unit,
    action: String? = null,
    onAction: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(NestlyColors.Background)
            .statusBarsPadding(),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(NestlyColors.Surface)
                .heightIn(min = 64.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back", tint = NestlyColors.TextPrimary)
            }
            Text(
                title,
                style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
                color = NestlyColors.TextPrimary,
                modifier = Modifier.weight(1f),
            )
            if (action != null && onAction != null) {
                Text(
                    action,
                    style = NestlyType.BodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = NestlyColors.TextPrimary,
                    modifier = Modifier
                        .clickable(onClick = onAction)
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                )
            }
        }
        Column(Modifier.weight(1f), content = content)
    }
}

/** Plain navigation row on a surface background, as in Account and Settings. */
@Composable
fun MenuRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    value: String? = null,
    serif: Boolean = true,
    color: Color = NestlyColors.TextPrimary,
    chevron: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    Column(modifier.background(NestlyColors.Surface)) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 82.dp)
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = if (serif) NestlyType.RowLabel else NestlyType.BodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = color,
                )
                if (subtitle != null) Text(subtitle, style = NestlyType.Small, color = NestlyColors.TextSecondary)
            }
            if (value != null) Text(value, style = NestlyType.Body, color = NestlyColors.TextPrimary)
            if (chevron && onClick != null) {
                Spacer(Modifier.width(6.dp))
                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = NestlyColors.TextPrimary, modifier = Modifier.size(22.dp))
            }
        }
        RowDivider()
    }
}

@Composable
fun SectionHeading(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 36.dp, bottom = 16.dp),
    ) {
        Text(text, style = NestlyType.SectionTitle, color = NestlyColors.TextPrimary)
    }
    RowDivider()
}
