package com.lifestyle.nestly.ui.activity

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.lifestyle.nestly.appContainer
import com.lifestyle.nestly.data.prefs.ActivityLayout
import com.lifestyle.nestly.data.prefs.ActivityToggle
import com.lifestyle.nestly.data.prefs.CardKind
import com.lifestyle.nestly.ui.components.DetailScreen
import com.lifestyle.nestly.ui.components.NestlySwitch
import com.lifestyle.nestly.ui.components.RowDivider
import com.lifestyle.nestly.ui.theme.NestlyColors
import com.lifestyle.nestly.ui.theme.NestlyType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private data class ToggleRow(val toggle: ActivityToggle, val label: String, val indent: Int)

private val GROUP_ROWS: Map<CardKind, List<ToggleRow>> = mapOf(
    CardKind.FEED to listOf(
        ToggleRow(ActivityToggle.BREASTFEEDING, "Breastfeeding", 0),
        ToggleRow(ActivityToggle.BOTTLE_FEEDING, "Bottle Feeding", 1),
        ToggleRow(ActivityToggle.BOTTLE_BREAST_MILK, "Breast Milk", 2),
        ToggleRow(ActivityToggle.BOTTLE_FORMULA, "Formula", 2),
        ToggleRow(ActivityToggle.SOLID_FEEDING, "Solid Feeding", 1),
        ToggleRow(ActivityToggle.COMBO_FEEDING, "Combo Feeding", 1),
    ),
    CardKind.PUMP to listOf(ToggleRow(ActivityToggle.PUMPING, "Pumping", 0)),
    CardKind.DIAPER to listOf(ToggleRow(ActivityToggle.DIAPER_CHANGES, "Diaper Changes", 0)),
    CardKind.SLEEP to listOf(ToggleRow(ActivityToggle.SLEEP, "Sleep", 0)),
    CardKind.ROUTINE to listOf(ToggleRow(ActivityToggle.ROUTINES, "Routines", 0)),
    CardKind.BABY_FIRSTS to listOf(ToggleRow(ActivityToggle.BABY_FIRSTS, "Baby Firsts", 0)),
    CardKind.GROWTH to listOf(ToggleRow(ActivityToggle.GROWTH, "Growth", 0)),
    CardKind.HEALTH to listOf(ToggleRow(ActivityToggle.HEALTH, "Health", 0)),
)

@Composable
fun EditActivitiesScreen(onBack: () -> Unit) {
    val prefs = LocalContext.current.appContainer.preferences
    val scope = rememberCoroutineScope()
    var order by remember { mutableStateOf<List<CardKind>?>(null) }
    var disabled by remember { mutableStateOf(emptySet<ActivityToggle>()) }
    LaunchedEffect(Unit) {
        val saved = prefs.layout.first()
        order = saved.order
        disabled = saved.disabled
    }

    val heights = remember { mutableStateMapOf<CardKind, Int>() }
    var dragging by remember { mutableStateOf<CardKind?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }

    DetailScreen(
        "Edit Activities",
        onBack = onBack,
        action = "Save",
        onAction = {
            val current = order ?: return@DetailScreen
            scope.launch {
                prefs.setLayout(ActivityLayout(current, disabled))
                onBack()
            }
        },
    ) {
        val current = order ?: return@DetailScreen
        Column(Modifier.verticalScroll(rememberScrollState())) {
            current.forEach { card -> key(card) {
                val isDragging = dragging == card
                Column(
                    Modifier
                        .zIndex(if (isDragging) 1f else 0f)
                        .graphicsLayer { translationY = if (isDragging) dragOffset else 0f }
                        .background(if (isDragging) NestlyColors.SurfaceRaised else NestlyColors.Surface)
                        .onSizeChanged { heights[card] = it.height },
                ) {
                    GROUP_ROWS.getValue(card).forEachIndexed { index, row ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .heightIn(min = 76.dp)
                                .padding(start = 20.dp, end = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (index == 0) {
                                Icon(
                                    Icons.Outlined.DragHandle,
                                    contentDescription = "Reorder",
                                    tint = NestlyColors.AccentText,
                                    modifier = Modifier
                                        .size(30.dp)
                                        .pointerInput(card) {
                                            detectDragGestures(
                                                onDragStart = {
                                                    dragging = card
                                                    dragOffset = 0f
                                                },
                                                onDragEnd = { dragging = null; dragOffset = 0f },
                                                onDragCancel = { dragging = null; dragOffset = 0f },
                                            ) { change, amount ->
                                                change.consume()
                                                dragOffset += amount.y
                                                val list = order ?: return@detectDragGestures
                                                val i = list.indexOf(card)
                                                val next = list.getOrNull(i + 1)
                                                val prev = list.getOrNull(i - 1)
                                                if (next != null && dragOffset > (heights[next] ?: 0) / 2f) {
                                                    dragOffset -= heights[next] ?: 0
                                                    order = list.toMutableList().apply { add(i + 1, removeAt(i)) }
                                                } else if (prev != null && dragOffset < -(heights[prev] ?: 0) / 2f) {
                                                    dragOffset += heights[prev] ?: 0
                                                    order = list.toMutableList().apply { add(i - 1, removeAt(i)) }
                                                }
                                            }
                                        },
                                )
                                Spacer(Modifier.width(18.dp))
                            } else {
                                Spacer(Modifier.width((48 + row.indent * 24).dp))
                            }
                            Text(row.label, style = NestlyType.RowLabel, modifier = Modifier.weight(1f))
                            NestlySwitch(
                                checked = row.toggle !in disabled,
                                onCheckedChange = { on -> disabled = if (on) disabled - row.toggle else disabled + row.toggle },
                            )
                        }
                        if (index < GROUP_ROWS.getValue(card).lastIndex) {
                            RowDivider(Modifier.padding(start = (48 + row.indent.coerceAtLeast(1) * 24).dp))
                        }
                    }
                    RowDivider()
                }
            } }
        }
    }
}
