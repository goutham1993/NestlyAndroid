package com.lifestyle.nestly.ui.activity

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lifestyle.nestly.data.db.entity.ActivityEntity
import com.lifestyle.nestly.data.prefs.CardKind
import com.lifestyle.nestly.ui.components.ActivityIcon
import com.lifestyle.nestly.ui.components.Format
import com.lifestyle.nestly.ui.components.IconKind
import com.lifestyle.nestly.ui.components.RowDivider
import com.lifestyle.nestly.ui.theme.NestlyColors
import com.lifestyle.nestly.ui.theme.NestlyType
import com.lifestyle.nestly.ui.theme.blobColor
import com.lifestyle.nestly.ui.theme.headerColor
import com.lifestyle.nestly.ui.units.currentUnitPrefs

private val CardShape = RoundedCornerShape(18.dp)

/** Rounded card with a colored title strip and the blue "+" button overlapping its bottom edge. */
@Composable
fun DashboardCard(
    kind: CardKind,
    onAdd: (() -> Unit)?,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(modifier.padding(horizontal = 24.dp)) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(CardShape)
                .background(NestlyColors.Surface),
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(62.dp)
                    .background(kind.headerColor)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(kind.title, style = NestlyType.CardTitle, color = NestlyColors.HeaderText)
            }
            content()
        }
        if (onAdd != null) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-20).dp, y = 34.dp)
                    .size(58.dp)
                    .shadow(4.dp, CircleShape)
                    .clip(CircleShape)
                    .background(NestlyColors.Accent)
                    .clickable(onClick = onAdd),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.Add, "Add ${kind.title}", tint = Color.White, modifier = Modifier.size(32.dp))
            }
        }
    }
}

/** "Last feeding / 1h 53m ago" with a big serif value and superscript unit on the right. */
@Composable
fun LastEntryRow(
    icon: IconKind,
    title: String,
    subtitle: String?,
    value: String?,
    unit: String? = null,
    onClick: (() -> Unit)? = null,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 160.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(start = 24.dp, end = 20.dp, top = 28.dp, bottom = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ActivityIcon(icon, size = 72.dp)
        Spacer(Modifier.width(22.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = NestlyType.CardLead, color = NestlyColors.TextPrimary)
            if (subtitle != null) Text(subtitle, style = NestlyType.Small, color = NestlyColors.TextPrimary)
        }
        if (value != null) {
            Row(verticalAlignment = Alignment.Top) {
                Text(value, style = NestlyType.BigValue, color = NestlyColors.TextPrimary, maxLines = 1)
                if (unit != null) {
                    Text(unit, style = NestlyType.Small.copy(fontSize = 15.sp), color = NestlyColors.TextPrimary, modifier = Modifier.padding(top = 6.dp, start = 2.dp))
                }
            }
        }
    }
}

/** Empty card body, e.g. "Track a sleep session". */
@Composable
fun EmptyCardRow(icon: IconKind, text: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 140.dp)
            .clickable(onClick = onClick)
            .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ActivityIcon(icon, size = 72.dp)
        Spacer(Modifier.width(22.dp))
        Text(text, style = NestlyType.CardLead, color = NestlyColors.TextPrimary, modifier = Modifier.weight(1f))
    }
}

@Composable
fun ShowMoreRow(expanded: Boolean, onToggle: () -> Unit) {
    RowDivider()
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 20.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(if (expanded) "Show Less" else "Show More", style = NestlyType.BodyMedium, modifier = Modifier.weight(1f))
        Icon(
            if (expanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
            null,
            tint = NestlyColors.TextPrimary,
        )
    }
}

@Composable
fun LinkRow(text: String, onClick: () -> Unit) {
    RowDivider()
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, style = NestlyType.Body, color = NestlyColors.TextSecondary, modifier = Modifier.weight(1f))
        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = NestlyColors.TextSecondary, modifier = Modifier.size(20.dp))
    }
}

/** One of today's entries: time, label, and a bar sized against the day's largest entry. */
@Composable
fun EntryListRow(
    entry: ActivityEntity,
    maxBarValue: Double,
    showIcon: Boolean,
    onClick: () -> Unit,
) {
    RowDivider()
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 20.dp, end = 16.dp, top = 14.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showIcon) {
            ActivityIcon(entry.iconKind, size = 34.dp)
            Spacer(Modifier.width(18.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row {
                Text(Format.time(entry.startTime), style = NestlyType.Body.copy(fontSize = 17.sp), color = NestlyColors.TextPrimary)
                if (entry.label.isNotEmpty()) {
                    Spacer(Modifier.width(8.dp))
                    Text(entry.label, style = NestlyType.Body.copy(fontSize = 17.sp), color = NestlyColors.TextPrimary, maxLines = 1)
                }
            }
            val bar = entry.barValue
            val detail = entry.detailText(currentUnitPrefs())
            if (bar != null || detail != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (bar != null && maxBarValue > 0) {
                        val fraction = (bar / maxBarValue).toFloat().coerceIn(0.08f, 1f)
                        Box(
                            Modifier
                                .width((220 * fraction).dp)
                                .height(7.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(entry.type.card.headerColor),
                        )
                        Spacer(Modifier.width(10.dp))
                    }
                    if (detail != null) Text(detail, style = NestlyType.Small, color = NestlyColors.TextPrimary, maxLines = 1)
                }
            }
        }
        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = NestlyColors.TextSecondary, modifier = Modifier.size(20.dp))
    }
}

/** Compact row inside the Growth and Health cards: icon, title, date and value. */
@Composable
fun MetricRow(icon: IconKind, title: String, subtitle: String?, value: String?, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 20.dp, end = 16.dp, top = 14.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ActivityIcon(icon, size = 44.dp)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = NestlyType.CardLead.copy(fontSize = 19.sp))
            if (subtitle != null) Text(subtitle, style = NestlyType.Small, color = NestlyColors.TextSecondary)
        }
        if (value != null) Text(value, style = NestlyType.Body.copy(fontWeight = FontWeight.Medium))
        Spacer(Modifier.width(6.dp))
        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = NestlyColors.TextSecondary, modifier = Modifier.size(20.dp))
    }
}

/** Pill shown on a card while a timer is running. */
@Composable
fun InProgressRow(icon: IconKind, text: String, elapsed: String, onClick: () -> Unit) {
    RowDivider()
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(icon.card.blobColor.copy(alpha = 0.18f))
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ActivityIcon(icon, size = 34.dp)
        Spacer(Modifier.width(16.dp))
        Text(text, style = NestlyType.BodyMedium, modifier = Modifier.weight(1f))
        Text(elapsed, style = NestlyType.BodyMedium)
        Spacer(Modifier.width(6.dp))
        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = NestlyColors.TextSecondary, modifier = Modifier.size(20.dp))
    }
}
