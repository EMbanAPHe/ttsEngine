package com.k2fsa.sherpa.onnx.tts.engine

import android.content.Context
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream

object KokoroInstaller {

    private const val BASE = "https://huggingface.co/onnx-community/Kokoro-82M-v1.0-ONNX/resolve/main"
    private const val ONNX_DIR = "$BASE/onnx"
    private const val VOICES_DIR = "$BASE/voices"
    private const val TOKENIZER = "$BASE/tokenizer.json"

    // filenames must match res/values/arrays.xml:koko ro_model_filenames order
    private val filenameToPretty = mapOf(
        "model.onnx" to "Kokoro 82M fp32",
        "model_fp16.onnx" to "Kokoro 82M fp16",
        "model_q4.onnx" to "Kokoro 82M q4",
        "model_q4f16.onnx" to "Kokoro 82M q4f16",
        "model_q8f16.onnx" to "Kokoro 82M q8f16",
        "model_quantized.onnx" to "Kokoro 82M int8 quant",
        "model_uint8.onnx" to "Kokoro 82M uint8",
        "model_uint8f16.onnx" to "Kokoro 82M uint8f16",
    )

    fun install(context: Context, selectedFilename: String) {
        CoroutineScope(Dispatchers.Main).launch {
            Toast.makeText(context, context.getString(R.string.kokoro_installing), Toast.LENGTH_SHORT).show()
            try {
                withContext(Dispatchers.IO) {
                    val dataDir = File(context.filesDir, "kokoro")
                    val modelDir = File(dataDir, "model"); modelDir.mkdirs()
                    val voicesDir = File(dataDir, "voices"); voicesDir.mkdirs()

                    // 1) model onnx
                    download( "$ONNX_DIR/$selectedFilename?download=true", File(modelDir, "model.onnx") )

                    // 2) tokenizer
                    download( TOKENIZER + "?download=true", File(modelDir, "tokenizer.json") )

                    // 3) voices: download all .bin files
                    val voiceList = listOf(
                        "af.bin","af_alloy.bin","af_aoede.bin","af_bella.bin","af_heart.bin","af_jessica.bin","af_kore.bin","af_nicole.bin","af_nova.bin","af_river.bin","af_sarah.bin","af_sky.bin",
                        "am_adam.bin","am_echo.bin","am_eric.bin","am_fenrir.bin","am_liam.bin","am_michael.bin","am_onyx.bin","am_puck.bin","am_santa.bin",
                        "bf_alice.bin","bf_emma.bin","bf_isabella.bin","bf_lily.bin",
                        "bm_daniel.bin","bm_fable.bin","bm_george.bin","bm_lewis.bin",
                        "ef_dora.bin",
                        "em_alex.bin","em_santa.bin",
                        "ff_siwiis.bin",
                        "hf_alpha.bin","hf_beta.bin","hm_omega.bin",
                        "if_sara.bin","im_nicola.bin",
                        "jf_alpha.bin","jf_gongitsune.bin","jf_nezumi.bin","jf_tebukuro.bin",
                        "jm_kumo.bin",
                        "pf_dora.bin","pm_alex.bin","pm_santa.bin",
                        "zf_xiaobei.bin","zf_xiaoni.bin","zf_xiaoxiao.bin"
                    )
                    for (v in voiceList) {
                        val out = File(voicesDir, v)
                        if (!out.exists()) {
                            download("$VOICES_DIR/$v?download=true", out)
                        }
                    }
                }

                // Register in LangDB so it appears with imported voices (implementation is in your project)
                try {
                    LangDB.getInstance(context).addLanguage(selectedFilename, filenameToPretty[selectedFilename] ?: "Kokoro 82M")
                } catch (_: Throwable) { /* no-op if helper not present */ }

                Toast.makeText(context, context.getString(R.string.kokoro_done), Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, context.getString(R.string.kokoro_failed) + ": " + e.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun download(url: String, outFile: File) {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 30000
        conn.readTimeout = 30000
        conn.inputStream.use { input ->
            FileOutputStream(outFile).use { output ->
                val buf = ByteArray(1 shl 16)
                while (true) {
                    val r = input.read(buf)
                    if (r <= 0) break
                    output.write(buf, 0, r)
                }
            }
        }
        conn.disconnect()
    }
}
