package com.k2fsa.sherpa.onnx.tts.engine

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.k2fsa.sherpa.onnx.tts.engine.db.LangDB
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object KokoroInstaller {

    private const val TAG = "KokoroInstaller"

    private const val KOKORO_DIR = "kokoro"
    private const val MODELS_SUBDIR = "onnx"
    private const val VOICES_SUBDIR = "voices"
    private const val MODEL_EXT = ".onnx"
    private const val VOICE_EXT = ".json"

    private const val ENGINE_ID = "kokoro"
    private const val MODEL_TYPE = "kokoro-onnx"

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

            val appDir = DocumentFile.fromFile(context.filesDir)
            val kokoroRoot = appDir.findFile(KOKORO_DIR) ?: appDir.createDirectory(KOKORO_DIR)!!
            val targetDirName = "${language}_${country}-$speakerId"
            val targetDir = kokoroRoot.findFile(targetDirName) ?: kokoroRoot.createDirectory(targetDirName)!!

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

            // ⬇️ THIS IS THE PART THE COMPILER IS COMPLAINING ABOUT IN YOUR BUILD
            LangDB.registerLanguage(
                context = context,
                engine = ENGINE_ID,
                language = language,
                country = country,          // required
                speakerId = speakerId,      // required
                speedPrefKey = speedPrefKey,// required (speed)
                volumePrefKey = volumePrefKey,// required (volume)
                modelType = MODEL_TYPE,     // required
                displayName = "${language}_${country} · $speakerId",
                installDir = targetDir.uri.toString()
            )

            true
        } catch (e: Throwable) {
            Log.e(TAG, "Kokoro install failed", e)
            false
        }
    }
}
