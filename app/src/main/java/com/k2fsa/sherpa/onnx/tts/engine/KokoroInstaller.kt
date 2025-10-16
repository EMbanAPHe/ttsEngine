package com.k2fsa.sherpa.onnx.tts.engine

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.k2fsa.sherpa.onnx.tts.engine.db.LangDB
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Installs Kokoro voice/model assets into the app's private storage and registers them
 * so the rest of the app can discover/select them like Piper & Coqui.
 */
object KokoroInstaller {

    private const val TAG = "KokoroInstaller"

    // Folder names & file extensions used by Kokoro ONNX packs
    private const val KOKORO_DIR = "kokoro"
    private const val MODELS_SUBDIR = "onnx"
    private const val VOICES_SUBDIR = "voices"
    private const val MODEL_EXT = ".onnx"
    private const val VOICE_EXT = ".json"

    // App’s notion of “engine”/“model type” label for Kokoro (consistent with DB)
    private const val ENGINE_ID = "kokoro"
    private const val MODEL_TYPE = "kokoro-onnx"

    /**
     * Install one Kokoro package (model + voice) from a SAF URI (folder or zip extracted).
     * The caller provides language, country, and speakerId that should be shown in UI.
     *
     * @param context Android context
     * @param sourceFolderUri SAF Uri pointing to a folder that contains /onnx and /voices
     * @param language Two-letter language code (e.g. "en")
     * @param country  Country/locale code (e.g. "US")
     * @param speakerId Displayable speaker key/name (e.g. "emma" or "vctk_p225")
     * @param speedPrefKey  String key for speed preference (reuses app’s existing key)
     * @param volumePrefKey String key for volume preference (reuses app’s existing key)
     */
    suspend fun install(
        context: Context,
        sourceFolderUri: Uri,
        language: String,
        country: String,
        speakerId: String,
        speedPrefKey: String = "speed",
        volumePrefKey: String = "volume",
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val picked = DocumentFile.fromTreeUri(context, sourceFolderUri)
            if (picked == null || !picked.isDirectory) {
                Log.e(TAG, "Invalid Kokoro source folder: $sourceFolderUri")
                return@withContext false
            }

            // Find /onnx/*.onnx and /voices/*.json
            val modelsDir = picked.findFile(MODELS_SUBDIR)
            val voicesDir = picked.findFile(VOICES_SUBDIR)

            if (modelsDir == null || voicesDir == null) {
                Log.e(TAG, "Kokoro source must contain /onnx and /voices subfolders")
                return@withContext false
            }

            val modelFile = modelsDir.listFiles().firstOrNull { it.name?.endsWith(MODEL_EXT, true) == true }
            val voiceFile = voicesDir.listFiles().firstOrNull { it.name?.endsWith(VOICE_EXT, true) == true }

            if (modelFile == null || voiceFile == null) {
                Log.e(TAG, "Missing Kokoro .onnx model or voice .json")
                return@withContext false
            }

            // Copy into app storage: /files/kokoro/<language>-<speaker>/{model.onnx, voice.json}
            val appDir = DocumentFile.fromFile(context.filesDir)
            val kokoroRoot = appDir.findFile(KOKORO_DIR) ?: appDir.createDirectory(KOKORO_DIR)!!
            val targetDirName = "${language}_${country}-$speakerId"
            val targetDir = kokoroRoot.findFile(targetDirName) ?: kokoroRoot.createDirectory(targetDirName)!!

            // Copy model
            val targetModel = targetDir.findFile(modelFile.name!!) ?: targetDir.createFile(
                "application/octet-stream",
                modelFile.name!!
            )!!
            context.contentResolver.openInputStream(modelFile.uri).use { input ->
                context.contentResolver.openOutputStream(targetModel.uri, "rwt").use { out ->
                    if (input == null || out == null) throw IllegalStateException("Stream open failed")
                    input.copyTo(out)
                }
            }

            // Copy voice
            val targetVoice = targetDir.findFile(voiceFile.name!!) ?: targetDir.createFile(
                "application/json",
                voiceFile.name!!
            )!!
            context.contentResolver.openInputStream(voiceFile.uri).use { input ->
                context.contentResolver.openOutputStream(targetVoice.uri, "rwt").use { out ->
                    if (input == null || out == null) throw IllegalStateException("Stream open failed")
                    input.copyTo(out)
                }
            }

            // Register in LangDB so UI can list/select this voice like Piper/Coqui
            // NOTE: The signature below matches the current DB helper (requires country, speakerId, speed, volume, and modelType).
            LangDB.registerLanguage(
                context = context,
                engine = ENGINE_ID,
                language = language,
                country = country,
                speakerId = speakerId,
                speedPrefKey = speedPrefKey,
                volumePrefKey = volumePrefKey,
                modelType = MODEL_TYPE,
                // Optional display label shown in pickers: "<language>-<country> · <speaker>"
                displayName = "${language}_${country} · $speakerId",
                // Optional: absolute directory the assets were copied to (for deletions)
                installDir = targetDir.uri.toString()
            )

            true
        } catch (e: Throwable) {
            Log.e(TAG, "Kokoro install failed", e)
            false
        }
    }
}
