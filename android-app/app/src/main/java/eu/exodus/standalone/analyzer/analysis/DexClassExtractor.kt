package eu.exodus.standalone.analyzer.analysis

import com.android.tools.smali.dexlib2.DexFileFactory
import com.android.tools.smali.dexlib2.Opcodes
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

class DexClassExtractor {
    fun extractEmbeddedClasses(apkFile: File, maxDepth: Int = 10): Set<String> {
        return extractFromStream(apkFile.inputStream(), depth = 0, maxDepth = maxDepth)
    }

    private fun extractFromStream(
        inputStream: InputStream,
        depth: Int,
        maxDepth: Int,
    ): Set<String> {
        if (depth > maxDepth) {
            return emptySet()
        }

        val classes = LinkedHashSet<String>()
        ZipInputStream(inputStream).use { zip ->
            var entry: ZipEntry? = zip.nextEntry
            while (entry != null) {
                val name = entry.name
                when {
                    name.endsWith(".apk", ignoreCase = true) -> {
                        val nestedBytes = zip.readEntryBytes()
                        classes += extractFromStream(
                            ByteArrayInputStream(nestedBytes),
                            depth + 1,
                            maxDepth,
                        )
                    }

                    isDexEntry(name) -> {
                        val dexBytes = zip.readEntryBytes()
                        classes += extractClassesFromDex(dexBytes)
                    }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        return classes
    }

    private fun extractClassesFromDex(dexBytes: ByteArray): Set<String> {
        val classes = LinkedHashSet<String>()
        val tempFile = File.createTempFile("exodus_dex_", ".dex")
        try {
            tempFile.writeBytes(dexBytes)
            val dexFile = DexFileFactory.loadDexFile(tempFile, Opcodes.getDefault())
            for (classDef in dexFile.classes) {
                val type = classDef.type
                if (type.startsWith("L") && type.endsWith(";")) {
                    classes.add(type.substring(1, type.length - 1).replace('/', '.'))
                }
            }
        } finally {
            tempFile.delete()
        }
        return classes
    }

    private fun isDexEntry(name: String): Boolean {
        return Regex("""classes.*\.dex$""", RegexOption.IGNORE_CASE).containsMatchIn(name)
    }

    private fun ZipInputStream.readEntryBytes(): ByteArray {
        return readBytes()
    }
}
