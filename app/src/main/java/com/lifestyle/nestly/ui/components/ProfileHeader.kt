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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lifestyle.nestly.ui.theme.NestlyColors
import com.lifestyle.nestly.ui.theme.NestlyType

@Composable
fun BabyAvatar(modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(80.dp)
            .clip(CircleShape)
            .background(Color(0xFFF4F4F6)),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(46.dp)
                .offset(x = 6.dp, y = 4.dp)
                .clip(CircleShape)
                .background(Color(0xFFA9C7E8)),
        )
        Icon(Icons.Outlined.ChildCare, null, tint = Color(0xFF2E4A72), modifier = Modifier.size(48.dp))
    }
}

/** Avatar, child name and date, with small outlined action buttons on the right. */
@Composable
fun ProfileHeader(
    name: String,
    dateText: String,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 20.dp, top = 16.dp, bottom = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BabyAvatar()
        Spacer(Modifier.width(18.dp))
        Column(Modifier.weight(1f)) {
            Text(name, style = NestlyType.Display, color = NestlyColors.TextPrimary, maxLines = 1)
            Text(dateText, style = NestlyType.Small, color = NestlyColors.TextSecondary)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically, content = actions)
    }
}

@Composable
fun HeaderIconButton(icon: ImageVector, contentDescription: String, onClick: () -> Unit, badge: Int = 0) {
    Box {
        Box(
            Modifier
                .size(width = 56.dp, height = 32.dp)
                .clip(RoundedCornerShape(6.dp))
                .border(1.dp, NestlyColors.TextMuted, RoundedCornerShape(6.dp))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription, tint = NestlyColors.TextPrimary, modifier = Modifier.size(20.dp))
        }
        if (badge > 0) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 4.dp, y = (-8).dp)
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(NestlyColors.Badge),
                contentAlignment = Alignment.Center,
            ) {
                Text(badge.toString(), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
