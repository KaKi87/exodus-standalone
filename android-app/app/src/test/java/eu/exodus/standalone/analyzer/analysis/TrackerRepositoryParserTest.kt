package eu.exodus.standalone.analyzer.analysis

import org.junit.Assert.assertEquals
import org.junit.Test

class TrackerRepositoryParserTest {

    @Test
    fun parseTrackers_readsExodusApiShape() {
        val json = """
            {
              "trackers": {
                "70": {
                  "name": "Facebook Share",
                  "code_signature": "com.facebook.share",
                  "categories": ["Analytics"],
                  "website": "https://example.com"
                }
              }
            }
        """.trimIndent()

        val trackers = TrackerRepository.parseTrackers(json)

        assertEquals(1, trackers.size)
        assertEquals(70, trackers.first().id)
        assertEquals("Facebook Share", trackers.first().name)
        assertEquals("com.facebook.share", trackers.first().codeSignature)
        assertEquals(listOf("Analytics"), trackers.first().categories)
    }
}
