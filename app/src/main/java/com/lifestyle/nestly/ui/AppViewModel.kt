package com.lifestyle.nestly.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifestyle.nestly.AppContainer
import com.lifestyle.nestly.data.model.CaregiverEntity
import com.lifestyle.nestly.data.model.ProfileEntity
import com.lifestyle.nestly.data.model.Sex
import com.lifestyle.nestly.data.prefs.FilterOption
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    /** Caregiver row linked to this account. Used to decide who can be removed. */
    val signedInCaregiver: StateFlow<CaregiverEntity?> = family.signedInCaregiver
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val filters: StateFlow<Set<FilterOption>> = container.preferences.filters
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    /** Add Photo hides the age prompt until the next launch, without counting as Not Now. */
    private val _photoPromptSuppressed = MutableStateFlow(false)
    val photoPromptSuppressed: StateFlow<Boolean> = _photoPromptSuppressed.asStateFlow()

    fun hidePhotoPromptForSession() {
        _photoPromptSuppressed.value = true
    }

    fun setFilters(filters: Set<FilterOption>) = viewModelScope.launch { container.preferences.setFilters(filters) }

    fun selectCaregiver(id: String) = viewModelScope.launch { family.selectCaregiver(id) }

    fun selectProfile(id: String) = viewModelScope.launch { family.selectProfile(id) }

    fun addChild(name: String, sex: Sex?, birthDate: LocalDate?, caregiverName: String, photoPath: String? = null) =
        viewModelScope.launch {
            family.addChild(name, sex, birthDate, caregiverName, photoPath)
        }

    fun updateChild(id: String, name: String, sex: Sex?, birthDate: LocalDate?, photoPath: String?) =
        viewModelScope.launch {
            family.updateChild(id, name, sex, birthDate, photoPath)
        }

    fun addCaregiver(name: String) = viewModelScope.launch {
        family.addCaregiver(name)
    }

    fun removeCaregiver(id: String, onDone: () -> Unit, onError: (String) -> Unit) = viewModelScope.launch {
        try {
            family.removeCaregiver(id)
            onDone()
        } catch (e: Exception) {
            onError(caregiverRemovalError(e.message))
        }
    }
}

private fun caregiverRemovalError(message: String?): String {
    val text = message.orEmpty()
    return when {
        text.contains("only owners can remove", ignoreCase = true) ->
            "Only the family owner can remove another signed-in caregiver."
        text.contains("needs to keep one owner") -> text
        text.isBlank() -> "Couldn't remove that caregiver. Check your connection and try again."
        else -> text
    }
}
