package eu.exodus.standalone.analyzer.model

data class Tracker(
    val id: Int,
    val name: String,
    val codeSignature: String,
    val categories: List<String> = emptyList(),
    val website: String? = null,
)

data class ApkMetadata(
    val path: String,
    val checksum: String,
    val packageName: String,
    val appName: String?,
    val versionName: String?,
    val versionCode: String?,
    val permissions: List<String>,
    val libraries: List<String>,
)

data class AnalysisReport(
    val apk: ApkMetadata,
    val trackers: List<Tracker>,
    val embeddedClassCount: Int,
)
