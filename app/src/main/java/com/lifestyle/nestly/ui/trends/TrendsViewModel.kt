package com.lifestyle.nestly.ui.trends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifestyle.nestly.AppContainer
import com.lifestyle.nestly.data.db.entity.ProfileEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.ZoneId

enum class TrendsWindow(val days: Int, val label: String) {
    ONE(1, "1 Day"),
    SEVEN(7, "7 Days"),
    FOURTEEN(14, "14 Days"),
}

data class TrendsData(
    val profile: ProfileEntity,
    val window: TrendsWindow,
    val cards: List<TrendCard>,
    val hasAnyData: Boolean,
)

@OptIn(ExperimentalCoroutinesApi::class)
class TrendsViewModel(private val container: AppContainer) : ViewModel() {
    private val zone: ZoneId get() = ZoneId.systemDefault()
    private val window = MutableStateFlow(TrendsWindow.SEVEN)

    val data: StateFlow<TrendsData?> = window.flatMapLatest { selected ->
        container.family.selectedProfile.flatMapLatest { profile ->
            if (profile == null) flowOf(null)
            else {
                val today = LocalDate.now(zone)
                val from = today.minusDays((selected.days - 1).toLong())
                val fromMs = from.atStartOfDay(zone).toInstant().toEpochMilli()
                val toMs = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
                container.activities.range(profile.familyId, profile.id, fromMs, toMs).map { entries ->
                    val cards = TrendsAggregator.aggregate(entries, from, selected.days, zone)
                    TrendsData(
                        profile = profile,
                        window = selected,
                        cards = cards,
                        hasAnyData = cards.any { it.count > 0 },
                    )
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setWindow(window: TrendsWindow) {
        this.window.value = window
    }
}
