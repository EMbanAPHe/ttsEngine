package com.k2fsa.sherpa.onnx.tts.engine

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

object KokoroInstaller {

    suspend fun downloadFirstAvailable(ctx: Context, urls: List<String>): File = withContext(Dispatchers.IO) {
        var lastErr: Exception? = null
        for (u in urls) {
            try {
                return@withContext downloadToCache(ctx, u)
            } catch (e: Exception) {
                lastErr = e
            }
        }
        throw lastErr ?: IOException("No working URL")
    }

    suspend fun downloadToCache(ctx: Context, url: String): File = withContext(Dispatchers.IO) {
        val cache = File(ctx.cacheDir, "kokoro_dl").apply { mkdirs() }
        val name = url.substringAfterLast('/').substringBefore('?').ifBlank { "kokoro-pack" }
        val out = File(cache, name)
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            instanceFollowRedirects = true
            connectTimeout = 30000
            readTimeout = 120000
        }
        conn.inputStream.use { input ->
            FileOutputStream(out).use { output -> input.copyTo(output) }
        }
        out
    }

    suspend fun extractToModelsDir(ctx: Context, archive: File): File = withContext(Dispatchers.IO) {
        val root = ctx.getExternalFilesDir(null) ?: ctx.filesDir
        val lower = archive.name.lowercase()
        return@withContext when {
            lower.endsWith(".zip") -> unzip(root, archive)
            lower.endsWith(".tar.bz2") || lower.endsWith(".tbz2") -> untarbz2(root, archive)
            else -> throw IllegalArgumentException("Unsupported archive: ${archive.name}")
        }
    }

    private fun unzip(root: File, zipFile: File): File {
        var topDir: File? = null
        ZipInputStream(BufferedInputStream(FileInputStream(zipFile))).use { zis ->
            var e: ZipEntry? = zis.nextEntry
            val buffer = ByteArray(8192)
            while (e != null) {
                val outFile = File(root, e.name)
                if (topDir == null) {
                    val first = e.name.substringBefore('/')
                    topDir = File(root, first)
                }
                if (e.isDirectory) {
                    outFile.mkdirs()
                } else {
                    outFile.parentFile?.mkdirs()
                    FileOutputStream(outFile).use { fos ->
                        var read: Int
                        while (zis.read(buffer).also { read = it } != -1) {
                            fos.write(buffer, 0, read)
                        }
                    }
                }
                zis.closeEntry()
                e = zis.nextEntry
            }
        }
        return topDir ?: throw IOException("Empty zip")
    }

    private fun untarbz2(root: File, tbz: File): File {
        var topDir: File? = null
        BZip2CompressorInputStream(BufferedInputStream(FileInputStream(tbz))).use { bzip2 ->
            TarArchiveInputStream(bzip2).use { tis ->
                var entry = tis.nextTarEntry
                val buffer = ByteArray(8192)
                while (entry != null) {
                    val outFile = File(root, entry.name)
                    if (topDir == null) {
                        val first = entry.name.substringBefore('/')
                        topDir = File(root, first)
                    }
                    if (entry.isDirectory) {
                        outFile.mkdirs()
                    } else {
                        outFile.parentFile?.mkdirs()
                        FileOutputStream(outFile).use { fos ->
                            var read: Int
                            while (tis.read(buffer).also { read = it } != -1) {
                                fos.write(buffer, 0, read)
                            }
                        }
                    }
                    entry = tis.nextTarEntry
                }
            }
        }
        return topDir ?: throw IOException("Empty tar.bz2")
    }

    fun looksLikeKokoro(dir: File): Boolean {
        val model = File(dir, "model.onnx").exists()
        val tokens = File(dir, "tokens.txt").exists()
        val voicesBin = File(dir, "voices.bin").exists()
        val voicesDir = File(dir, "voices").isDirectory
        val espeak = File(dir, "espeak-ng-data").isDirectory
        return model && tokens && (voicesBin || voicesDir) && espeak
    }
}
