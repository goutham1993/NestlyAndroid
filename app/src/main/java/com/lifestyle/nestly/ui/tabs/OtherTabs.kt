package com.lifestyle.nestly.ui.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.lifestyle.nestly.ui.theme.NestlyColors
import com.lifestyle.nestly.ui.theme.NestlyType

private val GUIDES = listOf(
    "0-3 Months Sleep Guide" to "An easy-to-read guide to your newborn's sleep needs and patterns.",
    "Guide to Safe Sleep" to "How to set up a safe sleep space and reduce sleep-related risks.",
    "0-6 Month Reading Guide" to "Why reading to your baby matters and how to start.",
    "A Guide to Combo Feeding" to "Combining breast milk and formula with confidence.",
    "Bottle Feeding 101" to "Choosing bottles, paced feeding, and how much your baby needs.",
    "3 to 6 Months Sleep Guide" to "Longer stretches, naps and building a routine.",
    "Infant Gas: Signs, Causes, and Tips" to "What causes gas and how to help your baby feel better.",
    "Sleep Regressions: What You Need to Know" to "Why sleep can change suddenly and how to cope.",
    "6-9 Months: Starting Solids" to "Signs of readiness, first foods and introducing allergens.",
)

@Composable
fun GuidesTab() {
    LazyColumn(
        Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text("Guides", style = NestlyType.Display, modifier = Modifier.padding(start = 20.dp, top = 24.dp, bottom = 8.dp))
        }
        items(GUIDES) { (title, body) ->
            Column(
                Modifier
                    .padding(horizontal = 20.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(NestlyColors.Surface),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .background(NestlyColors.SurfaceRaised),
                )
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(title, style = NestlyType.CardTitle.copy(fontSize = NestlyType.RowLabel.fontSize))
                    Text(body, style = NestlyType.Small, color = NestlyColors.TextSecondary)
                    Row {
                        Text(
                            "Coming soon",
                            style = NestlyType.Small,
                            color = NestlyColors.HeaderText,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(NestlyColors.Feed)
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                        Spacer(Modifier.width(1.dp))
                    }
                }
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}
