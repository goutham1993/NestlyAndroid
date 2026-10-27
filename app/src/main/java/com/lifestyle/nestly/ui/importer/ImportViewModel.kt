package com.lifestyle.nestly.ui.importer

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifestyle.nestly.AppContainer
import com.lifestyle.nestly.data.model.TypeCount
import com.lifestyle.nestly.data.supabase.RefreshBus
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import com.lifestyle.nestly.data.importer.ExportResult
import com.lifestyle.nestly.data.importer.ImportResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface ImportStatus {
    data object Idle : ImportStatus
    data object Running : ImportStatus
    data class Done(val result: ImportResult, val source: String) : ImportStatus
    data class Failed(val message: String) : ImportStatus
}

sealed interface ExportStatus {
    data object Idle : ExportStatus
    data object Running : ExportStatus
    data class Done(val result: ExportResult) : ExportStatus
    data class Failed(val message: String) : ExportStatus
}

class ImportViewModel(
    private val container: AppContainer,
    private val contentResolver: ContentResolver,
) : ViewModel() {
    private val _status = MutableStateFlow<ImportStatus>(ImportStatus.Idle)
    val status: StateFlow<ImportStatus> = _status.asStateFlow()

    private val _exportStatus = MutableStateFlow<ExportStatus>(ExportStatus.Idle)
    val exportStatus: StateFlow<ExportStatus> = _exportStatus.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val counts: StateFlow<List<TypeCount>> = RefreshBus.tick(RefreshBus.ACTIVITY)
        .flatMapLatest {
            flow {
                emit(runCatching { container.activities.fetchCountsByType() }.getOrDefault(emptyList()))
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun importNara(uri: Uri) = import(uri, "Nara") { stream ->
        container.importer.import(stream)
    }

    fun importNestly(uri: Uri) = import(uri, "Nestly") { stream ->
        container.nestlyImporter.import(stream)
    }

    fun exportNestly(uri: Uri) {
        if (_exportStatus.value == ExportStatus.Running) return
        _exportStatus.value = ExportStatus.Running
        viewModelScope.launch {
            val outcome = runCatching {
                withContext(Dispatchers.IO) {
                    val stream = contentResolver.openOutputStream(uri)
                        ?: error("Could not open the selected file")
                    stream.use { container.exporter.export(it) }
                }
            }
            _exportStatus.update {
                outcome.fold(
                    onSuccess = { ExportStatus.Done(it) },
                    onFailure = { ExportStatus.Failed(it.message ?: it.javaClass.simpleName) },
                )
            }
        }
    }

    private fun import(uri: Uri, source: String, block: suspend (java.io.InputStream) -> ImportResult) {
        if (_status.value == ImportStatus.Running) return
        _status.value = ImportStatus.Running
        viewModelScope.launch {
            val outcome = runCatching {
                withContext(Dispatchers.IO) {
                    val stream = contentResolver.openInputStream(uri)
                        ?: error("Could not open the selected file")
                    stream.use { block(it) }
                }
            }
            _status.update {
                outcome.fold(
                    onSuccess = { ImportStatus.Done(it, source) },
                    onFailure = { ImportStatus.Failed(it.message ?: it.javaClass.simpleName) },
                )
            }
        }
    }
}
