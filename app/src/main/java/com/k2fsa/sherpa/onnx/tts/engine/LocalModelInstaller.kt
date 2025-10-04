package com.k2fsa.sherpa.onnx.tts.engine

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import java.io.*
import java.util.zip.ZipInputStream

object LocalModelInstaller {

    data class ImportResult(
        val modelDir: String,
        val modelName: String,
        val lang: String,
        val country: String,
        val modelType: String // "piper" or "coqui"
    )

    private const val TEMP_DIR = "import-temp"

    fun installFromUri(ctx: Context, uri: Uri): Result<ImportResult> = runCatching {
        val appRoot = ctx.getExternalFilesDir(null)!!.absolutePath
        val temp = File(appRoot, TEMP_DIR).apply { deleteRecursively(); mkdirs() }

        ctx.contentResolver.openInputStream(uri)?.use { inStream ->
            if (looksLikeZip(uri)) unzip(inStream, temp)
            else copyStreamTo(File(temp, fileName(ctx, uri)), inStream)
        } ?: error("Could not open selected item")

        installNormalized(ctx, temp)
    }

    fun installFromTree(ctx: Context, tree: Uri): Result<ImportResult> = runCatching {
        val appRoot = ctx.getExternalFilesDir(null)!!.absolutePath
        val temp = File(appRoot, TEMP_DIR).apply { deleteRecursively(); mkdirs() }
        copyDocumentTree(ctx.contentResolver, tree, temp)
        installNormalized(ctx, temp)
    }

    private fun installNormalized(ctx: Context, temp: File): ImportResult {
        val detection = detectVoiceLayout(temp)
            ?: error("No supported voice files found. Expecting Piper (.onnx + .onnx.json) or Coqui (.onnx + config.json).")

        val (type, onnx, config) = detection
        val base = onnx.nameWithoutExtension
        val (lang, country) = deriveLangCountry(base)

        val finalDir = File(ctx.getExternalFilesDir(null)!!.absolutePath, lang + country).apply { mkdirs() }
        onnx.copyTo(File(finalDir, onnx.name), overwrite = true)
        config.copyTo(File(finalDir, config.name), overwrite = true)

        return ImportResult(finalDir.absolutePath, base, lang, country, type)
    }

    // Piper: *.onnx + *.onnx.json
    // Coqui: *.onnx + config.json
    private fun detectVoiceLayout(dir: File): Triple<String, File, File>? {
        val onnx = dir.walkTopDown().firstOrNull { it.isFile && it.extension.equals("onnx", true) } ?: return null

        val piperJson = dir.walkTopDown().firstOrNull { it.isFile && it.name.lowercase().endsWith(".onnx.json") }
        if (piperJson != null) return Triple("piper", onnx, piperJson)

        val coquiJson = dir.walkTopDown().firstOrNull { it.isFile && it.name.lowercase() == "config.json" }
        if (coquiJson != null) return Triple("coqui", onnx, coquiJson)

        return null
    }

    private fun looksLikeZip(uri: Uri): Boolean = uri.lastPathSegment?.lowercase()?.endsWith(".zip") == true

    private fun fileName(ctx: Context, uri: Uri): String {
        val cr: ContentResolver = ctx.contentResolver
        cr.query(uri, arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) {
                val idx = c.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                if (idx != -1) return c.getString(idx)
            }
        }
        return uri.lastPathSegment ?: "import.bin"
    }

    private fun unzip(inputStream: InputStream, outDir: File) {
        ZipInputStream(BufferedInputStream(inputStream)).use { zis ->
            var entry = zis.nextEntry
            val buffer = ByteArray(8192)
            while (entry != null) {
                if (!entry.isDirectory) {
                    val outFile = File(outDir, entry.name)
                    outFile.parentFile?.mkdirs()
                    FileOutputStream(outFile).use { fos ->
                        var read: Int
                        while (zis.read(buffer).also { read = it } != -1) fos.write(buffer, 0, read)
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }

    private fun copyDocumentTree(cr: ContentResolver, treeUri: Uri, outDir: File) {
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
            treeUri, DocumentsContract.getTreeDocumentId(treeUri)
        )
        cr.query(childrenUri, arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE
        ), null, null, null)?.use { cursor ->
            while (cursor.moveToNext()) {
                val docId = cursor.getString(0)
                val name = cursor.getString(1)
                val mime = cursor.getString(2)
                val documentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                if (DocumentsContract.Document.MIME_TYPE_DIR == mime) {
                    val subdir = File(outDir, name); subdir.mkdirs()
                    copyDocumentTree(cr, documentUri, subdir)
                } else {
                    cr.openInputStream(documentUri)?.use { ins ->
                        val outFile = File(outDir, name); outFile.parentFile?.mkdirs()
                        copyStreamTo(outFile, ins)
                    }
                }
            }
        }
    }

    private fun copyStreamTo(outFile: File, ins: InputStream) {
        outFile.parentFile?.mkdirs()
        FileOutputStream(outFile).use { os -> ins.copyTo(os) }
    }

    private fun deriveLangCountry(modelBase: String): Pair<String, String> {
        val prefix = modelBase.split("-").firstOrNull() ?: return "en" to "US"
        val parts = prefix.split("_")
        return if (parts.size == 2) parts[0] to parts[1] else "en" to "US"
    }
}
