package com.k2fsa.sherpa.onnx.tts.engine

import android.content.Context
import android.util.Log

object KokoroInstaller {

    private const val TAG = "KokoroInstaller"

    /**
     * Call this after you have downloaded/unpacked a Kokoro model.
     *
     * @param context    app context
     * @param language   BCP-47 (e.g., "en")
     * @param country    region (e.g., "US")
     * @param speakerId  speaker index or code; 0 is a safe default
     * @param speed      playback speed multiplier
     * @param volume     playback volume multiplier
     * @param modelType  string marker for your LangDB (e.g., "kokoro")
     */
    fun registerInstalledKokoro(
        context: Context,
        language: String,
        country: String = "US",
        speakerId: Int = 0,
        speed: Float = 1.0f,
        volume: Float = 1.0f,
        modelType: String = "kokoro",
    ) {
        // Try a direct call first (if your fork’s LangDB signature matches these names)
        try {
            LangDB.registerKokoro(
                context = context,
                language = language,
                country = country,
                speakerId = speakerId,
                speed = speed,
                volume = volume,
                modelType = modelType,
            )
            Log.i(TAG, "Registered Kokoro via direct call: $language-$country, spk=$speakerId")
            return
        } catch (e: Throwable) {
            Log.w(TAG, "Direct registerKokoro(...) call not available, trying reflection. Reason: ${e.message}")
        }

        // Fallback: reflection for forks where the method has a different order or param names.
        try {
            val clazz = LangDB::class.java
            val m = clazz.methods.firstOrNull { it.name == "registerKokoro" }
                ?: throw NoSuchMethodError("LangDB.registerKokoro not found")

            val params = m.parameterTypes
            val args = ArrayList<Any?>()

            // Best-effort mapping by parameter TYPE (works across forks):
            var nextString = 0
            val stringPool = arrayOf(language, country, modelType)

            for (p in params) {
                when {
                    p == Context::class.java -> args.add(context)
                    p == java.lang.Integer.TYPE -> args.add(speakerId)
                    p == java.lang.Float.TYPE || p == java.lang.Double.TYPE -> args.add(speed)
                    p == java.lang.Boolean.TYPE -> args.add(true)
                    p == java.lang.String::class.java -> {
                        // use language, then country, then modelType, then language again
                        args.add(stringPool.getOrElse(nextString) { language })
                        nextString++
                    }
                    p.isEnum -> {
                        // If an enum (e.g., ModelType), pick first constant
                        val constants = p.enumConstants
                        args.add(constants?.firstOrNull())
                    }
                    else -> {
                        // Last resort (shouldn't be needed on known forks)
                        args.add(null)
                    }
                }
            }

            m.invoke(null, *args.toTypedArray())
            Log.i(TAG, "Registered Kokoro via reflection: $language-$country, spk=$speakerId")
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to register Kokoro: ${e.message}", e)
            throw e
        }
    }
}
