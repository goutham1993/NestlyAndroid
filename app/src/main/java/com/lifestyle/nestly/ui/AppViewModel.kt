package com.lifestyle.nestly.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifestyle.nestly.AppContainer
import com.lifestyle.nestly.data.db.entity.CaregiverEntity
import com.lifestyle.nestly.data.db.entity.ProfileEntity
import com.lifestyle.nestly.data.db.entity.Sex
import com.lifestyle.nestly.data.prefs.FilterOption
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/** State shared across tabs: the selected child, caregivers and History filters. */
class AppViewModel(private val container: AppContainer) : ViewModel() {
    private val family = container.family

    /** Null while loading; empty list once loaded with no children. */
    val profiles: StateFlow<List<ProfileEntity>?> = family.profiles
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val profile: StateFlow<ProfileEntity?> = family.selectedProfile
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val caregivers: StateFlow<List<CaregiverEntity>> = family.caregivers
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val currentCaregiverId: StateFlow<String?> = family.currentCaregiver.map { it?.id }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val filters: StateFlow<Set<FilterOption>> = container.preferences.filters
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    fun setFilters(filters: Set<FilterOption>) = viewModelScope.launch { container.preferences.setFilters(filters) }

    fun selectCaregiver(id: String) = viewModelScope.launch { family.selectCaregiver(id) }

    fun selectProfile(id: String) = viewModelScope.launch { family.selectProfile(id) }

    fun addChild(name: String, sex: Sex?, birthDate: LocalDate?, caregiverName: String) = viewModelScope.launch {
        family.addChild(name, sex, birthDate, caregiverName)
    }

    fun updateChild(id: String, name: String, sex: Sex?, birthDate: LocalDate?) = viewModelScope.launch {
        family.updateChild(id, name, sex, birthDate)
    }

    fun addCaregiver(name: String) = viewModelScope.launch {
        family.addCaregiver(name)
    }
}
