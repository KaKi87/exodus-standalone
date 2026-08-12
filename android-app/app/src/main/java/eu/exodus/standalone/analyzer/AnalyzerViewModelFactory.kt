package eu.exodus.standalone.analyzer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class AnalyzerViewModelFactory(
    private val analyzer: eu.exodus.standalone.analyzer.analysis.ExodusAnalyzer,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AnalyzerViewModel::class.java)) {
            return AnalyzerViewModel(analyzer) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
