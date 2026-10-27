package com.lifestyle.nestly.ui.reminders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifestyle.nestly.AppContainer
import com.lifestyle.nestly.data.db.entity.ReminderEntity
import com.lifestyle.nestly.data.db.entity.ReminderKind
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class RemindersViewModel(private val container: AppContainer) : ViewModel() {
    val rows: StateFlow<Map<ReminderKind, ReminderEntity>> = container.family.selectedProfile
        .flatMapLatest { profile ->
            if (profile == null) flowOf(emptyMap())
            else flow {
                ReminderKind.entries.forEach { kind ->
                    container.reminders.getOrCreate(profile.familyId, profile.id, kind)
                }
                emit(Unit)
            }.flatMapLatest {
                container.reminders.observeForProfile(profile.familyId, profile.id).map { list ->
                    list.associateBy { it.kind }
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    fun setEnabled(kind: ReminderKind, enabled: Boolean) = viewModelScope.launch {
        val profile = container.family.selectedProfile.first() ?: return@launch
        val saved = container.reminders.setEnabled(kind, profile.familyId, profile.id, enabled)
        container.scheduler.schedule(saved)
    }

    fun setInterval(kind: ReminderKind, minutes: Int) = viewModelScope.launch {
        val profile = container.family.selectedProfile.first() ?: return@launch
        val saved = container.reminders.setInterval(kind, profile.familyId, profile.id, minutes)
        if (saved.enabled) container.scheduler.schedule(saved)
    }
}
