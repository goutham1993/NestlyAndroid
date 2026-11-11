package com.lifestyle.nestly.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChildCare
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lifestyle.nestly.ui.theme.NestlyColors
import com.lifestyle.nestly.ui.theme.NestlyType

@Composable
fun BabyAvatar(modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(NestlyColors.Accent.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Outlined.ChildCare, null, tint = NestlyColors.Accent, modifier = Modifier.size(36.dp))
    }
}

/** Letter monogram on a soft accent circle, matching the iOS child avatar. */
@Composable
fun MonogramAvatar(name: String, modifier: Modifier = Modifier, size: Dp = 56.dp) {
    val initial = name.trim().firstOrNull()?.uppercaseChar()?.toString()
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(NestlyColors.Accent.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center,
    ) {
        if (initial != null) {
            Text(
                initial,
                color = NestlyColors.Accent,
                fontSize = (size.value * 0.42f).sp,
                fontWeight = FontWeight.Bold,
                fontFamily = com.lifestyle.nestly.ui.theme.NestlyRounded,
            )
        } else {
            Icon(Icons.Outlined.ChildCare, null, tint = NestlyColors.Accent, modifier = Modifier.size(size * 0.5f))
        }
    }
}

/** Avatar, child name, age (when known) and date, with small outlined action buttons on the right. */
@Composable
fun ProfileHeader(
    name: String,
    dateText: String,
    ageText: String? = null,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 20.dp, top = 16.dp, bottom = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MonogramAvatar(name, size = 56.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(name, style = NestlyType.Display, color = NestlyColors.TextPrimary, maxLines = 1)
            val subtitle = listOfNotNull(ageText?.takeIf { it.isNotBlank() }, dateText).joinToString(" · ")
            if (subtitle.isNotBlank()) {
                Text(subtitle, style = NestlyType.Small, color = NestlyColors.TextSecondary, maxLines = 1)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically, content = actions)
    }
}

@Composable
fun HeaderIconButton(icon: ImageVector, contentDescription: String, onClick: () -> Unit, badge: Int = 0) {
    Box {
        Box(
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(NestlyColors.Accent.copy(alpha = 0.15f))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription, tint = NestlyColors.Accent, modifier = Modifier.size(22.dp))
        }
        if (badge > 0) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 4.dp, y = (-8).dp)
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(NestlyColors.Accent),
                contentAlignment = Alignment.Center,
            ) {
                Text(badge.toString(), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
