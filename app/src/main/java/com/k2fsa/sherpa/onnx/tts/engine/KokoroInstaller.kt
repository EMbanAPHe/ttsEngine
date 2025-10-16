package com.k2fsa.sherpa.onnx.tts.engine

import android.content.Context
import android.util.Log
import androidx.annotation.WorkerThread
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Installs Kokoro models and registers them in LangDB.
 *
 * The previous build failed with:
 * "No value passed for parameter 'country'/'speakerId'/'speed'/'volume'/'modelType'"
 * so this file passes **all** required named parameters explicitly.
 */
object KokoroInstaller {

    private const val TAG = "KokoroInstaller"

    // Example model metadata. Keep/extend as you need. ModelType is whatever your LangDB expects.
    data class KokoroModel(
        val displayName: String,   // e.g. "Kokoro 82M (ONNX)"
        val modelUrl: String,      // full URL to the ONNX model
        val modelType: String      // e.g. "kokoro-onnx"
    )

    /**
     * Public entry to kick off installation+registration.
     */
    fun installKokoro(
        context: Context,
        model: KokoroModel,
        language: String = "English",
        country: String = "US",
        speakerId: String = "af",   // change default if your app expects different initial speaker
        speed: Float = 1.0f,
        volume: Float = 1.0f
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val localModelPath = downloadModelIfNeeded(context, model)
                registerKokoro(
                    context = context,
                    language = language,
                    country = country,
                    speakerId = speakerId,
                    speed = speed,
                    volume = volume,
                    modelType = model.modelType,
                    modelName = model.displayName,
                    modelPath = localModelPath,
                    modelUrl = model.modelUrl
                )
                Log.i(TAG, "Kokoro installed and registered: ${model.displayName}")
            } catch (t: Throwable) {
                Log.e(TAG, "Failed to install Kokoro: ${model.displayName}", t)
            }
        }
    }

    @WorkerThread
    private fun downloadModelIfNeeded(context: Context, model: KokoroModel): String {
        // TODO: Replace this stub with your real downloader that returns a **local absolute path**.
        // For now, assume your existing downloader puts the file in app files dir under /kokoro/
        // and returns that full path.
        // e.g., val path = Downloader.download(context, model.modelUrl, "kokoro/${model.displayName}.onnx")
        // return path
        return "file://${context.filesDir.absolutePath}/kokoro/${model.displayName}.onnx"
    }

    /**
     * This is the call site that previously failed. It now uses **named args** for everything
     * your LangDB API requires (as proven by the compiler errors).
     *
     * If your LangDB has additional params, add them here with named args.
     */
    @WorkerThread
    private fun registerKokoro(
        context: Context,
        language: String,
        country: String,
        speakerId: String,
        speed: Float,
        volume: Float,
        modelType: String,
        modelName: String,
        modelPath: String,
        modelUrl: String
    ) {
        LangDB.registerKokoro(
            context = context,
            language = language,
            country = country,
            speakerId = speakerId,
            speed = speed,
            volume = volume,
            modelType = modelType,
            modelName = modelName,
            modelPath = modelPath,
            modelUrl = modelUrl
        )
    }
}
