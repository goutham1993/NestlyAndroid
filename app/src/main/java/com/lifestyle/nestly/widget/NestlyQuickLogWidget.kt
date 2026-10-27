package com.lifestyle.nestly.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.lifestyle.nestly.ui.entry.EntryKind

class NestlyQuickLogWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                QuickLogContent(context)
            }
        }
    }
}

@Composable
private fun QuickLogContent(context: Context) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(day = Bg, night = Bg))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "Nestly",
            style = TextStyle(
                color = ColorProvider(day = TextPrimary, night = TextPrimary),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            ),
            modifier = GlanceModifier.padding(bottom = 6.dp),
        )
        Row(modifier = GlanceModifier.fillMaxWidth()) {
            QuickButton(context, "Bottle", EntryKind.BOTTLE, Feed, GlanceModifier.defaultWeight())
            Spacer(GlanceModifier.width(6.dp))
            QuickButton(context, "Diaper", EntryKind.DIAPER, Diaper, GlanceModifier.defaultWeight())
        }
        Spacer(GlanceModifier.height(6.dp))
        Row(modifier = GlanceModifier.fillMaxWidth()) {
            QuickButton(context, "Sleep", EntryKind.SLEEP, Sleep, GlanceModifier.defaultWeight())
            Spacer(GlanceModifier.width(6.dp))
            QuickButton(context, "Pump", EntryKind.PUMP, Pump, GlanceModifier.defaultWeight())
        }
    }
}

@Composable
private fun QuickButton(
    context: Context,
    label: String,
    kind: EntryKind,
    tint: Color,
    modifier: GlanceModifier,
) {
    Text(
        text = label,
        style = TextStyle(
            color = ColorProvider(day = HeaderText, night = HeaderText),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
        ),
        modifier = modifier
            .height(40.dp)
            .background(ColorProvider(day = tint, night = tint))
            .padding(horizontal = 4.dp, vertical = 10.dp)
            .clickable(actionStartActivity(WidgetActions.openEntryIntent(context, kind))),
    )
}

class NestlyQuickLogWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NestlyQuickLogWidget()
}

private val Bg = Color(0xFF222536)
private val TextPrimary = Color(0xFFF1F1F4)
private val HeaderText = Color(0xFF23263A)
private val Feed = Color(0xFFFFC94D)
private val Diaper = Color(0xFFEDE6D6)
private val Sleep = Color(0xFFB7E0EE)
private val Pump = Color(0xFFF4A99A)
