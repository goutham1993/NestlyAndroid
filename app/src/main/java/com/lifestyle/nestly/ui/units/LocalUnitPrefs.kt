package com.lifestyle.nestly.ui.units

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.compositionLocalOf
import com.lifestyle.nestly.data.units.UnitPrefs

val LocalUnitPrefs = compositionLocalOf { UnitPrefs.DEFAULT }

@Composable
fun currentUnitPrefs(): UnitPrefs = LocalUnitPrefs.current
