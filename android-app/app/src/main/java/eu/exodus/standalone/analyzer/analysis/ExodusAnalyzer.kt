package eu.exodus.standalone.analyzer.analysis

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import eu.exodus.standalone.analyzer.model.AnalysisReport
import eu.exodus.standalone.analyzer.model.ApkMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

class ExodusAnalyzer(
    private val context: Context,
    private val trackerRepository: TrackerRepository = TrackerRepository(context),
    private val dexClassExtractor: DexClassExtractor = DexClassExtractor(),
    private val trackerMatcher: TrackerMatcher = TrackerMatcher(),
) {
    suspend fun analyze(apkUri: Uri, displayName: String): AnalysisReport = withContext(Dispatchers.IO) {
        val apkFile = copyApkToCache(apkUri, displayName)
        try {
            val metadata = readApkMetadata(apkFile, displayName)
            val embeddedClasses = dexClassExtractor.extractEmbeddedClasses(apkFile)
            val trackers = trackerRepository.getTrackers()
            val compiled = trackerMatcher.compile(trackers)
            val detected = trackerMatcher.detectTrackers(embeddedClasses, compiled)

            AnalysisReport(
                apk = metadata,
                trackers = detected,
                embeddedClassCount = embeddedClasses.size,
            )
        } finally {
            apkFile.delete()
        }
    }

    private fun copyApkToCache(apkUri: Uri, displayName: String): File {
        val safeName = displayName.replace(Regex("""[^\w.\-]"""), "_")
        val destination = File(context.cacheDir, "selected_$safeName")
        context.contentResolver.openInputStream(apkUri)?.use { input ->
            destination.outputStream().use { output ->
                input.copyTo(output)
            }
        } ?: error("Unable to read selected APK")

        return destination
    }

    private fun readApkMetadata(apkFile: File, displayName: String): ApkMetadata {
        val packageManager = context.packageManager
        val packageInfo = packageManager.getPackageArchiveInfo(
            apkFile.absolutePath,
            PackageManager.GET_PERMISSIONS,
        ) ?: error("Unable to parse APK manifest")

        packageInfo.applicationInfo?.let { applicationInfo ->
            applicationInfo.sourceDir = apkFile.absolutePath
            applicationInfo.publicSourceDir = apkFile.absolutePath
        }

        val applicationInfo = packageInfo.applicationInfo
        val appName = applicationInfo?.loadLabel(packageManager)?.toString()

        return ApkMetadata(
            path = displayName,
            checksum = sha256(apkFile),
            packageName = packageInfo.packageName.orEmpty(),
            appName = appName,
            versionName = packageInfo.versionName,
            versionCode = packageInfo.longVersionCode.toString(),
            permissions = packageInfo.requestedPermissions?.toList().orEmpty(),
            libraries = emptyList(),
        )
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var read = input.read(buffer)
            while (read >= 0) {
                digest.update(buffer, 0, read)
                read = input.read(buffer)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
