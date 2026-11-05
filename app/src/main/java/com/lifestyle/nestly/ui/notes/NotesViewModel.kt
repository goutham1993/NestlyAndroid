package com.lifestyle.nestly.ui.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifestyle.nestly.AppContainer
import com.lifestyle.nestly.data.model.NoteEntity
import com.lifestyle.nestly.data.supabase.ConflictException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class NotesViewModel(private val container: AppContainer) : ViewModel() {

    val notes: StateFlow<List<NoteEntity>?> =
        container.family.selectedProfile.flatMapLatest { profile ->
            if (profile == null) flowOf(emptyList())
            else container.notes.observeForProfile(profile.familyId, profile.id)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun note(id: String): Flow<NoteEntity?> = container.notes.observeById(id)

    /**
     * Saves [title] / [body] onto [loaded] (the version the editor opened), or
     * creates a new note for the selected child when [loaded] is null. If
     * someone else saved in between, [onConflict] receives their latest version.
     */
    fun save(
        loaded: NoteEntity?,
        title: String,
        body: String,
        onDone: () -> Unit,
        onConflict: (latest: NoteEntity?, message: String) -> Unit,
        onError: (String) -> Unit,
    ) = viewModelScope.launch {
        try {
            if (loaded != null) {
                container.notes.save(loaded.copy(title = title.trim(), body = body.trimEnd()))
            } else {
                val profile = container.family.selectedProfile.first()
                    ?: error("Add a child first")
                val now = System.currentTimeMillis()
                container.notes.save(
                    NoteEntity(
                        id = UUID.randomUUID().toString(),
                        familyId = profile.familyId,
                        profileId = profile.id,
                        title = title.trim(),
                        body = body.trimEnd(),
                        createdAt = now,
                        updatedAt = now,
                    ),
                )
            }
            onDone()
        } catch (e: ConflictException) {
            val latest = loaded?.let { runCatching { container.notes.getById(it.id) }.getOrNull() }
            onConflict(latest, e.message ?: "This note was changed by someone else.")
        } catch (e: Exception) {
            onError(e.message ?: "Couldn't save the note.")
        }
    }

    fun delete(note: NoteEntity, onDone: () -> Unit, onError: (String) -> Unit) =
        viewModelScope.launch {
            try {
                container.notes.delete(note.id)
                onDone()
            } catch (e: Exception) {
                onError(e.message ?: "Couldn't delete the note.")
            }
        }
}
