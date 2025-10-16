package com.k2fsa.sherpa.onnx.tts.engine

import android.content.Context
import android.util.Log
import androidx.annotation.WorkerThread
import com.k2fsa.sherpa.onnx.tts.engine.db.LangDB
import java.io.File

object KokoroInstaller {

    private const val TAG = "KokoroInstaller"

    // Minimal model descriptor so we can install by type+voice
    data class KokoroModel(
        val modelType: String,         // e.g. "82M", "120M", etc.
        val voice: String,             // voice name, e.g. "af_heart" etc.
        val modelUrl: String,          // direct .onnx URL
        val configUrl: String          // direct .json (or metadata) URL if required
    )

    /**
     * Install a Kokoro model and register it in LangDB.
     *
     * NOTE: LangDB.registerKokoro now requires:
     *   language, country, speakerId, speed, volume, modelType, voice, modelUrl, configUrl
     */
    @WorkerThread
    fun installKokoro(
        context: Context,
        model: KokoroModel,
        // sane defaults so the call site never breaks again
        language: String = "en",
        country: String = "US",
        speakerId: Int = 0,
        speed: Float = 1.0f,
        volume: Float = 1.0f,
    ): Boolean {
        return try {
            // If you download to local files first, do it here.
            // The code below assumes URLs are persisted in DB (same pattern the app uses for piper/coqui).
            val ok = LangDB.registerKokoro(
                context = context,
                language = language,
                country = country,
                speakerId = speakerId,
                speed = speed,
                volume = volume,
                modelType = model.modelType,
                voice = model.voice,
                modelUrl = model.modelUrl,
                configUrl = model.configUrl
            )
            if (!ok) {
                Log.e(TAG, "registerKokoro returned false for ${model.modelType}/${model.voice}")
            }
            ok
        } catch (t: Throwable) {
            Log.e(TAG, "installKokoro failed", t)
            false
        }
    }

    /**
     * Example helper to bulk-install a set the UI offers.
     * Call this from ManageLanguagesActivity when the user taps “Install Kokoro”.
     */
    @WorkerThread
    fun installDefaultSet(context: Context): Boolean {
        // Fill with whatever variants you’re exposing in UI
        val wanted = listOf(
            KokoroModel(
                modelType = "82M",
                voice = "af_heart",
                modelUrl = "https://huggingface.co/onnx-community/Kokoro-82M-v1.0-ONNX/resolve/main/onnx/kokoro-en-v1_0.onnx?download=true",
                configUrl = "https://huggingface.co/onnx-community/Kokoro-82M-v1.0-ONNX/resolve/main/voices/voices.json?download=true"
            ),
            // add more here (all sizes/voices you want)
        )

        var allOk = true
        for (m in wanted) {
            val ok = installKokoro(context, m)
            allOk = allOk && ok
        }
        return allOk
    }
}
