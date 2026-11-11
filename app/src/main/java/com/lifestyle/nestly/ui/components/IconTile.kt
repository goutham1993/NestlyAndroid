package com.lifestyle.nestly.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Category icon on a soft tinted rounded square, matching the iOS icon tile. */
@Composable
fun IconTile(
    kind: IconKind,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    filled: Boolean = false,
) {
    Box(
        modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.3f))
            .background(if (filled) tint else tint.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center,
    ) {
        ActivityIcon(
            kind,
            size = size * 0.78f,
            blob = Color.Transparent,
            line = if (filled) Color.White else tint,
        )
    }
}
