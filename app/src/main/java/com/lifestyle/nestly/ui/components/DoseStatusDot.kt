package com.lifestyle.nestly.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import com.lifestyle.nestly.ui.theme.NestlyColors

/** Solid green once today's dose is in; a red dot with an expanding halo until then. */
@Composable
fun DoseStatusDot(given: Boolean, size: Dp, modifier: Modifier = Modifier) {
    Box(modifier.size(size * 2), contentAlignment = Alignment.Center) {
        if (given) {
            Box(
                Modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(NestlyColors.DoseGiven),
            )
        } else {
            val transition = rememberInfiniteTransition(label = "dosePulse")
            val progress by transition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Restart),
                label = "dosePulseProgress",
            )
            Box(
                Modifier
                    .size(size)
                    .graphicsLayer {
                        val scale = 1f + progress
                        scaleX = scale
                        scaleY = scale
                        alpha = 0.6f * (1f - progress)
                    }
                    .clip(CircleShape)
                    .background(NestlyColors.DoseMissing),
            )
            Box(
                Modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(NestlyColors.DoseMissing),
            )
        }
    }
}
