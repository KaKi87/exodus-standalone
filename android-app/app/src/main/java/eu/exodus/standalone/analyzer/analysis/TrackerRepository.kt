package eu.exodus.standalone.analyzer.analysis

import android.content.Context
import eu.exodus.standalone.analyzer.model.Tracker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

class TrackerRepository(
    context: Context,
    private val httpClient: OkHttpClient = defaultHttpClient(),
) {
    private val cacheFile = File(context.cacheDir, "exodus_trackers.json")

    suspend fun getTrackers(forceRefresh: Boolean = false): List<Tracker> = withContext(Dispatchers.IO) {
        if (!forceRefresh) {
            loadCachedTrackers()?.let { return@withContext it }
        }

        val response = httpClient.newCall(
            Request.Builder()
                .url(TRACKERS_URL)
                .get()
                .build(),
        ).execute()

        check(response.isSuccessful) {
            "Failed to download tracker signatures (${response.code})"
        }

        val body = response.body?.string()
            ?: error("Empty response from Exodus tracker API")

        cacheFile.writeText(body)
        parseTrackers(body)
    }

    private fun loadCachedTrackers(): List<Tracker>? {
        if (!cacheFile.exists()) {
            return null
        }
        return runCatching { parseTrackers(cacheFile.readText()) }.getOrNull()
    }

    companion object {
        private const val TRACKERS_URL = "https://reports.exodus-privacy.eu.org/api/trackers"

        fun defaultHttpClient(): OkHttpClient {
            return OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build()
        }

        fun parseTrackers(json: String): List<Tracker> {
            val root = JSONObject(json)
            val trackersObject = root.getJSONObject("trackers")
            val trackers = mutableListOf<Tracker>()

            trackersObject.keys().forEach { key ->
                val tracker = trackersObject.getJSONObject(key)
                val categories = tracker.optJSONArray("categories")
                    ?.let { array ->
                        buildList(array.length()) { repeat(array.length()) { add(array.getString(it)) } }
                    }
                    ?: emptyList()

                trackers.add(
                    Tracker(
                        id = key.toInt(),
                        name = tracker.getString("name"),
                        codeSignature = tracker.getString("code_signature"),
                        categories = categories,
                        website = tracker.optString("website").takeIf { it.isNotBlank() },
                    ),
                )
            }

            return trackers.sortedBy { it.name.lowercase() }
        }
    }
}

class TrackerMatcher {
    data class CompiledTracker(
        val tracker: Tracker,
        val pattern: Pattern,
    )

    fun compile(trackers: List<Tracker>): List<CompiledTracker> {
        return trackers.mapNotNull { tracker ->
            if (tracker.codeSignature.length <= 3) {
                return@mapNotNull null
            }
            runCatching {
                CompiledTracker(tracker, Pattern.compile(tracker.codeSignature))
            }.getOrNull()
        }
    }

    fun detectTrackers(
        embeddedClasses: Collection<String>,
        compiledTrackers: List<CompiledTracker>,
    ): List<Tracker> {
        val found = LinkedHashSet<Tracker>()
        for (className in embeddedClasses) {
            for ((tracker, pattern) in compiledTrackers) {
                if (pattern.matcher(className).find()) {
                    found.add(tracker)
                }
            }
        }
        return found.toList()
    }
}
