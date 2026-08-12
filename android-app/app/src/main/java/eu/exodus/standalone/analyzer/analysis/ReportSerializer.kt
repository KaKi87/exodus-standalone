package eu.exodus.standalone.analyzer.analysis

import eu.exodus.standalone.analyzer.model.AnalysisReport
import org.json.JSONArray
import org.json.JSONObject

object ReportSerializer {
    fun toJson(report: AnalysisReport): String {
        val root = JSONObject()
        root.put(
            "application",
            JSONObject().apply {
                put("handle", report.apk.packageName)
                put("version_name", report.apk.versionName)
                put("version_code", report.apk.versionCode)
                put("name", report.apk.appName)
                put("permissions", JSONArray(report.apk.permissions))
                put("libraries", JSONArray(report.apk.libraries))
            },
        )
        root.put(
            "apk",
            JSONObject().apply {
                put("path", report.apk.path)
                put("checksum", report.apk.checksum)
            },
        )
        root.put(
            "trackers",
            JSONArray().apply {
                report.trackers.forEach { tracker ->
                    put(
                        JSONObject().apply {
                            put("id", tracker.id)
                            put("name", tracker.name)
                        },
                    )
                }
            },
        )
        root.put("embedded_class_count", report.embeddedClassCount)
        return root.toString(2)
    }
}
