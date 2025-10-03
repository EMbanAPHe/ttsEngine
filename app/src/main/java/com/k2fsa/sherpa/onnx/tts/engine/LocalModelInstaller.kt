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
        val modelType: String = "piper"
    )

    private const val MODELS_DIR = "models"
    private const val TEMP_DIR = "import-temp"

    fun installFromUri(ctx: Context, uri: Uri): Result<ImportResult> = runCatching {
        val appRoot = ctx.getExternalFilesDir(null)!!.absolutePath
        val temp = File(appRoot, TEMP_DIR).apply { deleteRecursively(); mkdirs() }

        ctx.contentResolver.openInputStream(uri)?.use { inStream ->
            if (looksLikeZip(uri)) {
                unzip(inStream, temp)
            } else {
                // copy single file (expecting .onnx or .onnx.json)
                copyStreamToDir(inStream, File(temp, fileName(ctx, uri)))
            }
        } ?: error("Could not open selected item")

        val normalized = normalizeVoiceLayout(temp) ?: error("Expecting Piper voice files (.onnx + .onnx.json)")

        val (onnx, json) = normalized
        val base = onnx.nameWithoutExtension
        val (lang, country) = deriveLangCountry(base)

        // Final target directory uses TtsEngine convention: <external>/ <lang><country>
        val finalDir = File(appRoot, lang + country).apply { mkdirs() }

        // Copy or move files
        onnx.copyTo(File(finalDir, onnx.name), overwrite = true)
        json.copyTo(File(finalDir, json.name), overwrite = true)

        ImportResult(finalDir.absolutePath, base, lang, country, "piper")
    }

    fun installFromTree(ctx: Context, tree: Uri): Result<ImportResult> = runCatching {
        val appRoot = ctx.getExternalFilesDir(null)!!.absolutePath
        val temp = File(appRoot, TEMP_DIR).apply { deleteRecursively(); mkdirs() }

        copyDocumentTree(ctx.contentResolver, tree, temp)

        val normalized = normalizeVoiceLayout(temp) ?: error("Expecting Piper voice files (.onnx + .onnx.json)")
        val (onnx, json) = normalized
        val base = onnx.nameWithoutExtension
        val (lang, country) = deriveLangCountry(base)

        val finalDir = File(appRoot, lang + country).apply { mkdirs() }
        onnx.copyTo(File(finalDir, onnx.name), overwrite = true)
        json.copyTo(File(finalDir, json.name), overwrite = true)

        ImportResult(finalDir.absolutePath, base, lang, country, "piper")
    }

    private fun looksLikeZip(uri: Uri): Boolean {
        val name = uri.lastPathSegment?.lowercase() ?: return false
        return name.endsWith(".zip")
    }

    private fun fileName(ctx: Context, uri: Uri): String {
        val cr: ContentResolver = ctx.contentResolver
        val c = cr.query(uri, arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME), null, null, null)
        c?.use {
            if (it.moveToFirst()) {
                val idx = it.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                if (idx != -1) return it.getString(idx)
            }
        }
        return uri.lastPathSegment ?: "import.bin"
    }

    private fun unzip(inputStream: InputStream, outDir: File) {
        ZipInputStream(BufferedInputStream(inputStream)).use { zis ->
            var entry = zis.nextEntry
            val buffer = ByteArray(8 * 1024)
            while (entry != null) {
                if (!entry.isDirectory) {
                    val outFile = File(outDir, entry.name)
                    outFile.parentFile?.mkdirs()
                    FileOutputStream(outFile).use { fos ->
                        var read: Int
                        while (zis.read(buffer).also { read = it } != -1) {
                            fos.write(buffer, 0, read)
                        }
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }

    private fun copyStreamToDir(inputStream: InputStream, outFile: File) {
        outFile.parentFile?.mkdirs()
        inputStream.use { ins ->
            FileOutputStream(outFile).use { os ->
                ins.copyTo(os)
            }
        }
    }

    private fun copyDocumentTree(cr: ContentResolver, treeUri: Uri, outDir: File) {
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
            treeUri,
            DocumentsContract.getTreeDocumentId(treeUri)
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
                    val subdir = File(outDir, name)
                    subdir.mkdirs()
                    copyDocumentTree(cr, documentUri, subdir)
                } else {
                    cr.openInputStream(documentUri)?.use { ins ->
                        copyStreamToDir(ins, File(outDir, name))
                    }
                }
            }
        }
    }

    // Return pair (onnx, json)
    private fun normalizeVoiceLayout(dir: File): Pair<File, File>? {
        val onnx = dir.walkTopDown().firstOrNull { it.isFile && it.extension.lowercase() == "onnx" } ?: return null
        val json = dir.walkTopDown().firstOrNull { it.isFile && it.name.lowercase().endsWith(".onnx.json") } ?: return null
        return Pair(onnx, json)
    }

    private fun deriveLangCountry(modelBase: String): Pair<String, String> {
        // Expect prefix like en_GB-xxx; fallback to en-US
        val prefix = modelBase.split("-").firstOrNull() ?: return Pair("en","US")
        val parts = prefix.split("_")
        if (parts.size == 2) {
            return Pair(parts[0], parts[1])
        }
        return Pair("en","US")
    }
}
