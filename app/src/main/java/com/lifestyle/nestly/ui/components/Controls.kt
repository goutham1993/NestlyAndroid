package com.lifestyle.nestly.ui.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lifestyle.nestly.ui.theme.NestlyColors
import com.lifestyle.nestly.ui.theme.NestlyType

@Composable
fun RowDivider(modifier: Modifier = Modifier) = HorizontalDivider(modifier, thickness = 1.dp, color = NestlyColors.Divider)

/**
 * A full-width form row: serif label on the left, then an optional plain value and a bold action
 * ("Today  Add"), and an optional chevron.
 */
@Composable
fun FormRow(
    label: String,
    modifier: Modifier = Modifier,
    value: String? = null,
    action: String? = null,
    chevron: Boolean = false,
    valueColor: Color = NestlyColors.TextPrimary,
    onClick: (() -> Unit)? = null,
) {
    Column(modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 82.dp)
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = NestlyType.RowLabel, color = NestlyColors.TextPrimary, modifier = Modifier.weight(1f))
            if (value != null) Text(value, style = NestlyType.Body, color = valueColor, textAlign = TextAlign.End)
            if (value != null && action != null) Spacer(Modifier.width(8.dp))
            if (action != null) Text(action, style = NestlyType.BodyMedium.copy(fontWeight = FontWeight.Bold), color = NestlyColors.TextPrimary)
            if (chevron) {
                Spacer(Modifier.width(4.dp))
                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = NestlyColors.TextPrimary, modifier = Modifier.size(22.dp))
            }
        }
        RowDivider()
    }
}

@Composable
fun SwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 82.dp)
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = NestlyType.RowLabel, modifier = Modifier.weight(1f))
            NestlySwitch(checked, onCheckedChange)
        }
        RowDivider()
    }
}

@Composable
fun NestlySwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit) = Switch(
    checked = checked,
    onCheckedChange = onCheckedChange,
    colors = SwitchDefaults.colors(
        checkedThumbColor = Color.White,
        checkedTrackColor = NestlyColors.Accent,
        checkedBorderColor = NestlyColors.Accent,
        uncheckedThumbColor = Color.White,
        uncheckedTrackColor = NestlyColors.TextMuted,
        uncheckedBorderColor = NestlyColors.TextMuted,
    ),
)

/** Inline notes editor shaped like a form row. */
@Composable
fun NotesField(value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier) {
        Box(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 82.dp)
                .padding(horizontal = 20.dp, vertical = 26.dp),
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = NestlyType.Body.copy(color = NestlyColors.TextPrimary),
                cursorBrush = SolidColor(NestlyColors.AccentText),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    if (value.isEmpty()) Text("Notes", style = NestlyType.RowLabel, color = NestlyColors.TextPrimary)
                    inner()
                },
            )
        }
        RowDivider()
    }
}

/** The big outlined circles used for wet/dirty/dry and breast milk/formula. */
@Composable
fun CircleToggle(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, size: Dp = 98.dp) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(if (selected) NestlyColors.Accent else Color.Transparent)
            .border(1.5.dp, NestlyColors.Accent, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = NestlyType.BodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = if (selected) Color.White else NestlyColors.AccentText,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun SegmentedToggle(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(NestlyColors.Background)
            .padding(3.dp),
    ) {
        options.forEachIndexed { index, option ->
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .height(38.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (index == selected) NestlyColors.SegmentSelected else Color.Transparent)
                    .clickable { onSelect(index) },
                contentAlignment = Alignment.Center,
            ) {
                Text(option, style = NestlyType.Button, color = NestlyColors.TextPrimary)
            }
        }
    }
}

/** White outlined pill, e.g. "Start Timer", "Add Photo", "Yes". */
@Composable
fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    filled: Boolean = false,
    compact: Boolean = false,
) {
    val shape = RoundedCornerShape(50)
    Box(
        modifier
            .clip(shape)
            .background(if (filled) NestlyColors.SurfaceRaised else Color.Transparent)
            .border(BorderStroke(1.5.dp, if (filled) NestlyColors.Divider else Color.White), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = if (compact) 14.dp else 30.dp, vertical = if (compact) 6.dp else 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = NestlyType.Button, color = NestlyColors.TextPrimary)
    }
}

/** Large stopwatch-style readout: "0H 0M" with small unit letters and a pencil to edit. */
@Composable
fun TimerReadout(parts: List<Pair<Long, String>>, onEdit: (() -> Unit)?, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.Center) {
        parts.forEachIndexed { index, (value, unit) ->
            if (index > 0) Spacer(Modifier.width(10.dp))
            Text(value.toString(), style = NestlyType.TimerDigits, color = NestlyColors.TextPrimary)
            Text(unit, style = NestlyType.TimerUnit, color = NestlyColors.TextPrimary, modifier = Modifier.padding(bottom = 8.dp, start = 2.dp))
        }
        if (onEdit != null) {
            Icon(
                Icons.Outlined.Edit,
                contentDescription = "Edit time",
                tint = NestlyColors.TextPrimary,
                modifier = Modifier
                    .padding(start = 12.dp, bottom = 30.dp)
                    .size(22.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onEdit),
            )
        }
    }
}

/** Centered row of choices, e.g. the wet/dirty/dry circles. */
@Composable
fun ChoiceRow(content: @Composable RowScope.() -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 28.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}
