package com.lifestyle.nestly.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.lifestyle.nestly.data.units.LengthUnit
import com.lifestyle.nestly.data.units.TempUnit
import com.lifestyle.nestly.data.units.UnitPrefs
import com.lifestyle.nestly.data.units.VolumeUnit
import com.lifestyle.nestly.data.units.WeightUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Dashboard cards, in default order. */
enum class CardKind { FEED, PUMP, DIAPER, SLEEP, ROUTINE, BABY_FIRSTS, GROWTH, HEALTH }

/** Individual switches on the Edit Activities screen. */
enum class ActivityToggle(val card: CardKind) {
    BREASTFEEDING(CardKind.FEED),
    BOTTLE_FEEDING(CardKind.FEED),
    BOTTLE_BREAST_MILK(CardKind.FEED),
    BOTTLE_FORMULA(CardKind.FEED),
    SOLID_FEEDING(CardKind.FEED),
    COMBO_FEEDING(CardKind.FEED),
    PUMPING(CardKind.PUMP),
    DIAPER_CHANGES(CardKind.DIAPER),
    SLEEP(CardKind.SLEEP),
    ROUTINES(CardKind.ROUTINE),
    BABY_FIRSTS(CardKind.BABY_FIRSTS),
    GROWTH(CardKind.GROWTH),
    HEALTH(CardKind.HEALTH),
}

/** Categories on the Filters sheet. */
enum class FilterOption { NOTES, PHOTOS, FEED, PUMP, DIAPER, SLEEP, ROUTINE, HEALTH }

data class ActivityLayout(
    val order: List<CardKind>,
    val disabled: Set<ActivityToggle>,
) {
    fun isEnabled(toggle: ActivityToggle) = toggle !in disabled

    /** A card is shown when at least one of its switches is on. */
    fun isCardVisible(card: CardKind) =
        ActivityToggle.entries.any { it.card == card && it !in disabled && it.isTopLevel }

    companion object {
        val DEFAULT = ActivityLayout(CardKind.entries, emptySet())
    }
}

private val ActivityToggle.isTopLevel: Boolean
    get() = this != ActivityToggle.BOTTLE_BREAST_MILK && this != ActivityToggle.BOTTLE_FORMULA

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

class UserPreferences(context: Context) {
    private val store = context.applicationContext.dataStore

    val selectedProfileId: Flow<String?> = store.data.map { it[SELECTED_PROFILE] }
    val currentCaregiverId: Flow<String?> = store.data.map { it[CURRENT_CAREGIVER] }

    val layout: Flow<ActivityLayout> = store.data.map { prefs ->
        val saved = prefs[CARD_ORDER]?.split(',')?.mapNotNull { name -> CardKind.entries.find { it.name == name } }
        val order = (saved.orEmpty() + CardKind.entries).distinct()
        val disabled = prefs[DISABLED].orEmpty()
            .mapNotNull { name -> ActivityToggle.entries.find { it.name == name } }
            .toSet()
        ActivityLayout(order, disabled)
    }

    val filters: Flow<Set<FilterOption>> = store.data.map { prefs ->
        prefs[FILTERS].orEmpty().mapNotNull { name -> FilterOption.entries.find { it.name == name } }.toSet()
    }

    val units: Flow<UnitPrefs> = store.data.map { prefs ->
        UnitPrefs(
            volume = prefs[VOLUME_UNIT]?.let { runCatching { VolumeUnit.valueOf(it) }.getOrNull() } ?: VolumeUnit.ML,
            weight = prefs[WEIGHT_UNIT]?.let { runCatching { WeightUnit.valueOf(it) }.getOrNull() } ?: WeightUnit.KG,
            length = prefs[LENGTH_UNIT]?.let { runCatching { LengthUnit.valueOf(it) }.getOrNull() } ?: LengthUnit.CM,
            temp = prefs[TEMP_UNIT]?.let { runCatching { TempUnit.valueOf(it) }.getOrNull() } ?: TempUnit.F,
        )
    }

    suspend fun setSelectedProfile(id: String) = store.edit { it[SELECTED_PROFILE] = id }

    suspend fun setCurrentCaregiver(id: String) = store.edit { it[CURRENT_CAREGIVER] = id }

    suspend fun setLayout(layout: ActivityLayout) = store.edit { prefs ->
        prefs[CARD_ORDER] = layout.order.joinToString(",") { it.name }
        prefs[DISABLED] = layout.disabled.map { it.name }.toSet()
    }

    suspend fun setFilters(filters: Set<FilterOption>) = store.edit { prefs ->
        prefs[FILTERS] = filters.map { it.name }.toSet()
    }

    suspend fun setUnits(units: UnitPrefs) = store.edit { prefs ->
        prefs[VOLUME_UNIT] = units.volume.name
        prefs[WEIGHT_UNIT] = units.weight.name
        prefs[LENGTH_UNIT] = units.length.name
        prefs[TEMP_UNIT] = units.temp.name
    }

    private companion object {
        val SELECTED_PROFILE = stringPreferencesKey("selected_profile")
        val CURRENT_CAREGIVER = stringPreferencesKey("current_caregiver")
        val CARD_ORDER = stringPreferencesKey("card_order")
        val DISABLED = stringSetPreferencesKey("disabled_toggles")
        val FILTERS = stringSetPreferencesKey("filters")
        val VOLUME_UNIT = stringPreferencesKey("volume_unit")
        val WEIGHT_UNIT = stringPreferencesKey("weight_unit")
        val LENGTH_UNIT = stringPreferencesKey("length_unit")
        val TEMP_UNIT = stringPreferencesKey("temp_unit")
    }
}
