package eu.exodus.standalone.analyzer

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eu.exodus.standalone.analyzer.analysis.ExodusAnalyzer
import eu.exodus.standalone.analyzer.analysis.ReportSerializer
import eu.exodus.standalone.analyzer.model.AnalysisReport
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AnalyzerViewModel(
    private val analyzer: ExodusAnalyzer,
) : ViewModel() {

    private val _uiState = MutableStateFlow<AnalyzerUiState>(AnalyzerUiState.Idle)
    val uiState: StateFlow<AnalyzerUiState> = _uiState.asStateFlow()

    fun onApkSelected(uri: Uri, displayName: String) {
        _uiState.value = AnalyzerUiState.Selected(uri, displayName)
    }

    fun analyzeSelectedApk() {
        val selected = _uiState.value as? AnalyzerUiState.Selected ?: return
        _uiState.value = AnalyzerUiState.Loading(selected.displayName)

        viewModelScope.launch {
            runCatching {
                analyzer.analyze(selected.uri, selected.displayName)
            }.onSuccess { report ->
                _uiState.value = AnalyzerUiState.Success(
                    displayName = selected.displayName,
                    report = report,
                    jsonReport = ReportSerializer.toJson(report),
                )
            }.onFailure { error ->
                _uiState.value = AnalyzerUiState.Error(
                    displayName = selected.displayName,
                    message = error.message ?: "Unknown error",
                )
            }
        }
    }

    fun reset() {
        _uiState.value = AnalyzerUiState.Idle
    }
}

sealed interface AnalyzerUiState {
    data object Idle : AnalyzerUiState

    data class Selected(
        val uri: Uri,
        val displayName: String,
    ) : AnalyzerUiState

    data class Loading(val displayName: String) : AnalyzerUiState

    data class Success(
        val displayName: String,
        val report: AnalysisReport,
        val jsonReport: String,
    ) : AnalyzerUiState

    data class Error(
        val displayName: String,
        val message: String,
    ) : AnalyzerUiState
}
