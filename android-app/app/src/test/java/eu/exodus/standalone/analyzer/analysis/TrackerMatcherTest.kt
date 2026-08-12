package eu.exodus.standalone.analyzer.analysis

import eu.exodus.standalone.analyzer.model.Tracker
import org.junit.Assert.assertEquals
import org.junit.Test

class TrackerMatcherTest {

    private val matcher = TrackerMatcher()

    @Test
    fun detectTrackers_matchesCodeSignature() {
        val trackers = listOf(
            Tracker(id = 1, name = "Google Analytics", codeSignature = "com.google.android.gms.analytics"),
            Tracker(id = 2, name = "Flurry", codeSignature = "com.flurry."),
        )
        val classes = listOf(
            "com.example.app.MainActivity",
            "com.google.android.gms.analytics.GoogleAnalytics",
            "com.flurry.android.FlurryAgent",
        )

        val detected = matcher.detectTrackers(classes, matcher.compile(trackers))

        assertEquals(listOf("Google Analytics", "Flurry"), detected.map { it.name })
    }

    @Test
    fun compile_skipsShortSignatures() {
        val trackers = listOf(
            Tracker(id = 1, name = "Short", codeSignature = "ab"),
            Tracker(id = 2, name = "Valid", codeSignature = "com.valid.tracker"),
        )

        val compiled = matcher.compile(trackers)

        assertEquals(1, compiled.size)
        assertEquals("Valid", compiled.first().tracker.name)
    }
}
