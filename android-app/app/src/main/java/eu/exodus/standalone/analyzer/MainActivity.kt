package eu.exodus.standalone.analyzer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import eu.exodus.standalone.analyzer.analysis.ExodusAnalyzer
import eu.exodus.standalone.analyzer.model.AnalysisReport
import eu.exodus.standalone.analyzer.ui.theme.ExodusAnalyzerTheme

class MainActivity : ComponentActivity() {

    private val viewModel: AnalyzerViewModel by viewModels {
        AnalyzerViewModelFactory(ExodusAnalyzer(applicationContext))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ExodusAnalyzerTheme {
                AnalyzerScreen(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyzerScreen(viewModel: AnalyzerViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            val displayName = uri.lastPathSegment?.substringAfterLast('/')
                ?: "selected.apk"
            viewModel.onApkSelected(uri, displayName)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.app_name))
                        Text(
                            text = stringResource(R.string.subtitle),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ActionButtons(
                uiState = uiState,
                onSelectApk = { launcher.launch(arrayOf("application/vnd.android.package-archive", "application/octet-stream", "*/*")) },
                onAnalyze = viewModel::analyzeSelectedApk,
                onReset = viewModel::reset,
            )

            when (val state = uiState) {
                AnalyzerUiState.Idle -> EmptyState()
                is AnalyzerUiState.Selected -> SelectedState(state.displayName)
                is AnalyzerUiState.Loading -> LoadingState(state.displayName)
                is AnalyzerUiState.Error -> ErrorState(state.message) { viewModel.analyzeSelectedApk() }
                is AnalyzerUiState.Success -> SuccessState(state.report, state.jsonReport)
            }
        }
    }
}

@Composable
private fun ActionButtons(
    uiState: AnalyzerUiState,
    onSelectApk: () -> Unit,
    onAnalyze: () -> Unit,
    onReset: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Button(
            onClick = onSelectApk,
            modifier = Modifier.weight(1f),
        ) {
            Icon(Icons.Default.Description, contentDescription = null)
            Text(stringResource(R.string.select_apk), modifier = Modifier.padding(start = 8.dp))
        }

        FilledTonalButton(
            onClick = onAnalyze,
            enabled = uiState is AnalyzerUiState.Selected || uiState is AnalyzerUiState.Error,
            modifier = Modifier.weight(1f),
        ) {
            Icon(Icons.Default.Analytics, contentDescription = null)
            Text(stringResource(R.string.analyze), modifier = Modifier.padding(start = 8.dp))
        }
    }

    if (uiState !is AnalyzerUiState.Idle) {
        OutlinedButton(onClick = onReset) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Text(stringResource(R.string.retry), modifier = Modifier.padding(start = 8.dp))
        }
    }
}

@Composable
private fun EmptyState() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.no_apk_selected),
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun SelectedState(displayName: String) {
    InfoCard(title = displayName, lines = listOf("Ready for Exodus static analysis."))
}

@Composable
private fun LoadingState(displayName: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CircularProgressIndicator()
            Column {
                Text(stringResource(R.string.analyzing), fontWeight = FontWeight.SemiBold)
                Text(displayName, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.error_title), fontWeight = FontWeight.Bold)
            Text(message)
            Button(onClick = onRetry) {
                Text(stringResource(R.string.retry))
            }
        }
    }
}

@Composable
private fun SuccessState(report: AnalysisReport, jsonReport: String) {
    val context = androidx.compose.ui.platform.LocalContext.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            InfoCard(
                title = stringResource(R.string.app_info),
                lines = listOf(
                    report.apk.appName ?: report.apk.path,
                    "${stringResource(R.string.package_name)}: ${report.apk.packageName}",
                    "${stringResource(R.string.version)}: ${report.apk.versionName ?: "?"} (${report.apk.versionCode ?: "?"})",
                    "${stringResource(R.string.checksum)}: ${report.apk.checksum}",
                    stringResource(R.string.permissions_count, report.apk.permissions.size),
                    "Embedded classes: ${report.embeddedClassCount}",
                ),
            )
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (report.trackers.isEmpty()) {
                            stringResource(R.string.no_trackers_found)
                        } else {
                            stringResource(R.string.trackers_found, report.trackers.size)
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (report.trackers.isEmpty()) {
                        Text("This APK does not match any known Exodus tracker signatures.")
                    }
                }
            }
        }

        if (report.trackers.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.trackers_section),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            items(report.trackers) { tracker ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(tracker.name, fontWeight = FontWeight.SemiBold)
                        Text("ID: ${tracker.id}", style = MaterialTheme.typography.bodySmall)
                        if (tracker.categories.isNotEmpty()) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                tracker.categories.forEach { category ->
                                    SuggestionChip(
                                        onClick = {},
                                        label = { Text(category) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (report.apk.permissions.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.permissions_section),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            items(report.apk.permissions) { permission ->
                Text(
                    text = permission,
                    modifier = Modifier.padding(horizontal = 4.dp),
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        item {
            Button(
                onClick = {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, "Exodus analysis: ${report.apk.packageName}")
                        putExtra(Intent.EXTRA_TEXT, jsonReport)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.share_report)))
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.Share, contentDescription = null)
                Text(stringResource(R.string.share_report), modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

@Composable
private fun InfoCard(title: String, lines: List<String>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            lines.forEach { line ->
                Text(line, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
