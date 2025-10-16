package com.k2fsa.sherpa.onnx.tts.engine

import android.content.Context
import android.util.Log
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class KokoroInstaller(
    private val activity: ManageLanguagesActivity
) {
    private val tag = "KokoroInstaller"

    fun installKokoro(
        language: String,
        modelName: String,
        modelUrl: String,
    ) {
        activity.lifecycleScope.launch {
            try {
                val ctx = activity.applicationContext

                // Download model into app's models dir (same pattern as Piper in the original)
                val localPath = withContext(Dispatchers.IO) {
                    FileDownloader.downloadToModelsDir(
                        context = ctx,
                        url = modelUrl,
                        fileName = "$modelName.onnx"
                    )
                }

                // *** EXACT: pass every required param to LangDB.registerKokoro ***
                LangDB.registerKokoro(
                    context = ctx,
                    language = language,
                    country = "US",
                    speakerId = 0,
                    speed = 1.0f,
                    volume = 1.0f,
                    modelType = "kokoro",
                    modelName = modelName,
                    modelUrl = localPath
                )

                activity.toast(activity.getString(R.string.kokoro_installed, modelName))
                activity.refreshLists()
            } catch (t: Throwable) {
                Log.e(tag, "Kokoro install failed", t)
                activity.toast(activity.getString(R.string.error_download, t.message ?: "unknown"))
            }
        }
    }
}

object FileDownloader {
    fun downloadToModelsDir(context: Context, url: String, fileName: String): String {
        val modelsDir = context.getExternalFilesDir(null)!!.resolve("models").apply { mkdirs() }
        val outFile = modelsDir.resolve(fileName)
        Http.download(url, outFile)  // same tiny helper used elsewhere in the project
        return outFile.absolutePath
    }
}
