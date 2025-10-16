package com.k2fsa.sherpa.onnx.tts.engine

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

object KokoroInstaller {

    private const val TAG = "KokoroInstaller"
    private const val ENGINE_NAME = "Kokoro"

    private data class KokoroModel(
        val displayName: String,
        val modelType: String,
        val onnxFile: String,
        val voicesJson: String = "vctk.json"
    )

    // Start with 82M to get you compiling and running; expand later if you want more sizes.
    private val MODELS = listOf(
        KokoroModel(
            displayName = "Kokoro 82M v1.0",
            modelType   = "kokoro-82M",
            onnxFile    = "kokoro-82M.onnx"
        )
    )

    fun installAll(context: Context) {
        Thread {
            try {
                MODELS.forEach { m ->
                    val modelDir = File(context.filesDir, "models/$ENGINE_NAME/${m.modelType}")
                    if (!modelDir.exists()) modelDir.mkdirs()

                    // Download model + voices if missing
                    downloadIfMissing(
                        dest = File(modelDir, m.onnxFile),
                        url  = "https://huggingface.co/onnx-community/Kokoro-82M-v1.0-ONNX/resolve/main/onnx/${m.onnxFile}?download=1"
                    )
                    downloadIfMissing(
                        dest = File(modelDir, m.voicesJson),
                        url  = "https://huggingface.co/onnx-community/Kokoro-82M-v1.0-ONNX/resolve/main/voices/${m.voicesJson}?download=1"
                    )

                    // EXACT named params your CI complained about:
                    LangDB.registerKokoro(
                        context    = context,
                        language   = "en",
                        country    = "US",
                        speakerId  = 0,
                        speed      = 1.0f,
                        volume     = 1.0f,
                        modelType  = m.modelType,
                        modelDir   = modelDir.absolutePath,
                        modelFile  = m.onnxFile,
                        voicesJson = m.voicesJson,
                        displayName= m.displayName
                    )

                    Log.i(TAG, "Installed ${m.displayName}")
                }
            } catch (t: Throwable) {
                Log.e(TAG, "Kokoro install failed", t)
            }
        }.start()
    }

    private fun downloadIfMissing(dest: File, url: String) {
        if (dest.exists() && dest.length() > 0) return
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 30000
            readTimeout = 30000
            requestMethod = "GET"
        }
        conn.inputStream.use { input ->
            FileOutputStream(dest).use { out ->
                val buf = ByteArray(8 * 1024)
                while (true) {
                    val n = input.read(buf)
                    if (n <= 0) break
                    out.write(buf, 0, n)
                }
            }
        }
        conn.disconnect()
    }
}
