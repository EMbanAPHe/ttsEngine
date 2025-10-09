package com.k2fsa.sherpa.onnx.tts.engine

import android.content.Context
import java.io.File

object VoiceStorage {
    fun deleteVoiceFolder(context: Context, lang: String, country: String) {
        val root = File(context.getExternalFilesDir(null), "voices")
        File(root, "${lang}_${country}").deleteRecursively()
    }
}
