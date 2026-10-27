package com.lifestyle.nestly.ui.account

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifestyle.nestly.appContainer
import com.lifestyle.nestly.data.units.LengthUnit
import com.lifestyle.nestly.data.units.TempUnit
import com.lifestyle.nestly.data.units.UnitPrefs
import com.lifestyle.nestly.data.units.Units
import com.lifestyle.nestly.data.units.VolumeUnit
import com.lifestyle.nestly.data.units.WeightUnit
import com.lifestyle.nestly.ui.components.DetailScreen
import com.lifestyle.nestly.ui.components.RowDivider
import com.lifestyle.nestly.ui.components.SegmentedToggle
import com.lifestyle.nestly.ui.theme.NestlyColors
import com.lifestyle.nestly.ui.theme.NestlyType
import kotlinx.coroutines.launch

@Composable
fun UnitsScreen(onBack: () -> Unit) {
    val container = LocalContext.current.appContainer
    val prefs by container.preferences.units.collectAsStateWithLifecycle(initialValue = UnitPrefs.DEFAULT)
    val scope = rememberCoroutineScope()

    fun update(transform: (UnitPrefs) -> UnitPrefs) {
        scope.launch { container.preferences.setUnits(transform(prefs)) }
    }

    DetailScreen("Units", onBack) {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            Text(
                "Storage stays metric. These settings only change what you see and enter.",
                style = NestlyType.Small,
                color = NestlyColors.TextSecondary,
                modifier = Modifier.padding(20.dp),
            )
            UnitPicker(
                title = "Volume",
                options = listOf(Units.volumeLabel(VolumeUnit.ML), Units.volumeLabel(VolumeUnit.OZ)),
                selected = if (prefs.volume == VolumeUnit.ML) 0 else 1,
                onSelect = { index -> update { it.copy(volume = if (index == 0) VolumeUnit.ML else VolumeUnit.OZ) } },
            )
            UnitPicker(
                title = "Weight",
                options = listOf(Units.weightLabel(WeightUnit.KG), Units.weightLabel(WeightUnit.LB)),
                selected = if (prefs.weight == WeightUnit.KG) 0 else 1,
                onSelect = { index -> update { it.copy(weight = if (index == 0) WeightUnit.KG else WeightUnit.LB) } },
            )
            UnitPicker(
                title = "Length",
                options = listOf(Units.lengthLabel(LengthUnit.CM), Units.lengthLabel(LengthUnit.IN)),
                selected = if (prefs.length == LengthUnit.CM) 0 else 1,
                onSelect = { index -> update { it.copy(length = if (index == 0) LengthUnit.CM else LengthUnit.IN) } },
            )
            UnitPicker(
                title = "Temperature",
                options = listOf(Units.tempLabel(TempUnit.C), Units.tempLabel(TempUnit.F)),
                selected = if (prefs.temp == TempUnit.C) 0 else 1,
                onSelect = { index -> update { it.copy(temp = if (index == 0) TempUnit.C else TempUnit.F) } },
            )
        }
    }
}

@Composable
private fun UnitPicker(title: String, options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Column(Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
        Text(title, style = NestlyType.RowLabel, modifier = Modifier.padding(bottom = 10.dp))
        SegmentedToggle(options, selected, onSelect)
    }
    RowDivider()
}
