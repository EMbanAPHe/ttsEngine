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
        val name = url.substringAfterLast('/').ifBlank { "kokoro-pack" }
        val out = File(cache, name)
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 30000
            readTimeout = 60000
        }
        conn.inputStream.use { input ->
            FileOutputStream(out).use { output -> input.copyTo(output) }
        }
        out
    }

    suspend fun extractToModelsDir(ctx: Context, archive: File): File = withContext(Dispatchers.IO) {
        val modelsRoot = ctx.getExternalFilesDir(null) ?: ctx.filesDir
        val lower = archive.name.lowercase()
        when {
            lower.endsWith(".zip") -> unzip(modelsRoot, archive)
            lower.endsWith(".tar.bz2") || lower.endsWith(".tbz2") -> untarbz2(modelsRoot, archive)
            else -> throw IllegalArgumentException("Unsupported archive: ${archive.name}")
        }
    }

    private fun unzip(root: File, zipFile: File): File {
        var topDir: File? = null
        ZipInputStream(BufferedInputStream(FileInputStream(zipFile))).use { zis ->
            var e: ZipEntry? = zis.nextEntry
            while (e != null) {
                val outFile = File(root, e.name)
                if (topDir == null) topDir = File(root, e.name.substringBefore('/'))
                if (e.isDirectory) outFile.mkdirs() else {
                    outFile.parentFile?.mkdirs()
                    FileOutputStream(outFile).use { fos -> zis.copyTo(fos) }
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
                var e = tis.nextTarEntry
                while (e != null) {
                    val outFile = File(root, e.name)
                    if (topDir == null) topDir = File(root, e.name.substringBefore('/'))
                    if (e.isDirectory) outFile.mkdirs() else {
                        outFile.parentFile?.mkdirs()
                        FileOutputStream(outFile).use { fos -> tis.copyTo(fos) }
                    }
                    e = tis.nextTarEntry
                }
            }
        }
        return topDir ?: throw IOException("Empty tar.bz2")
    }

    fun looksLikeKokoro(dir: File): Boolean {
        return File(dir, "model.onnx").exists() and                (File(dir, "voices.bin").exists() || File(dir, "voices").isDirectory) and                File(dir, "tokens.txt").exists() and                File(dir, "espeak-ng-data").isDirectory
    }
}
