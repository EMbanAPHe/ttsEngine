package com.k2fsa.sherpa.onnx.tts.engine

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import java.io.File

/**
 * Minimal Kokoro installer that:
 * 1) Parses a packed URL string (pipe/comma/space separated).
 * 2) Downloads all parts into app's external files dir.
 * 3) Calls registerKokoro with all required params.
 *
 * Expected packed string examples (any of these separators are accepted):
 *   "https://.../kokoro.onnx|https://.../voices/en-us/af.json"
 *   "https://.../kokoro.onnx, https://.../af.json"
 */
object KokoroInstaller {

    fun install(context: Context, packedUrls: String) {
        Toast.makeText(context, context.getString(R.string.kokoro_installing), Toast.LENGTH_SHORT).show()

        val parts = packedUrls
            .split('|', ',', ' ')
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        if (parts.isEmpty()) {
            Toast.makeText(context, context.getString(R.string.kokoro_failed), Toast.LENGTH_LONG).show()
            return
        }

        // Destination folder: …/Android/data/<pkg>/files/kokoro/enUS/
        val lang = "en"              // adjust if you later expose multiple
        val country = "US"
        val modelType = "kokoro"
        val destDir = File(context.getExternalFilesDir(null), "kokoro/${lang}${country}")
        if (!destDir.exists()) destDir.mkdirs()

        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

        // Download every part
        parts.forEach { url ->
            val fileName = Uri.parse(url).lastPathSegment ?: "part.onnx"
            val req = DownloadManager.Request(Uri.parse(url))
                .setTitle("Downloading $fileName")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, "kokoro/${lang}${country}/$fileName")
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)

            dm.enqueue(req)
        }

        // Register in local DB so it shows up in Imported voices
        // Supply ALL required parameters to avoid the "No value passed for parameter ..." error.
        val name = "Kokoro (82M)"    // Display name in your list
        val speakerId = "af"         // Default voice; change if you expose more
        val speed = 1.0f
        val volume = 1.0f

        // If your DB API differs, adjust the call accordingly.
        // The compile error you saw was because the older call didn't pass these parameters.
        try {
            LangDB.getInstance(context).registerKokoro(
                lang = lang,
                country = country,
                name = name,
                modelType = modelType,
                speakerId = speakerId,
                speed = speed,
                volume = volume,
                // Add more named args here if your signature requires them (e.g., paths)
            )
            Toast.makeText(context, context.getString(R.string.kokoro_done), Toast.LENGTH_SHORT).show()
        } catch (t: Throwable) {
            Toast.makeText(context, context.getString(R.string.kokoro_failed), Toast.LENGTH_LONG).show()
        }
    }
}
