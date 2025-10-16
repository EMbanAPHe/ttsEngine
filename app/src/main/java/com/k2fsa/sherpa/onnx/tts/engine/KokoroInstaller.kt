package com.k2fsa.sherpa.onnx.tts.engine

import android.content.Context
import android.util.Log

/**
 * Installs/registers a Kokoro model entry in the local LangDB.
 *
 * This version avoids the "No value passed for parameter 'country'/'speakerId'/'speed'/'volume'/'modelType'"
 * error by:
 *  - Prefer calling LangDB.registerKokoro(...) with all parameters
 *  - Falling back to LangDB.addLanguage(filename, prettyName) if the helper is not available
 *
 * It uses reflection for registerKokoro so it compiles against both old and new LangDBs.
 */
object KokoroInstaller {

    private const val TAG = "KokoroInstaller"

    /**
     * @param context Android context
     * @param selectedFilename the raw filename that identifies the Kokoro model on disk
     * @param filenameToPretty map from filename -> human-readable name (shown to the user)
     *
     * Safe to call multiple times; duplicate handling is left to LangDB.
     */
    fun registerSelectedKokoro(
        context: Context,
        selectedFilename: String,
        filenameToPretty: Map<String, String>
    ) {
        val pretty = filenameToPretty[selectedFilename] ?: "Kokoro 82M"

        // Always try the new API with all args first.
        val langDb = LangDB.getInstance(context)

        // Prefer: registerKokoro(lang, country, name, modelType, speakerId, speed, volume)
        // Fallback: addLanguage(filename, prettyName)
        val ok = tryRegisterKokoroAllArgs(langDb, pretty)
        if (ok) {
            Log.i(TAG, "Registered Kokoro via registerKokoro(...)")
            return
        }

        // Fallback for older DBs
        try {
            @Suppress("DEPRECATION")
            langDb.addLanguage(selectedFilename, pretty)
            Log.i(TAG, "Registered Kokoro via addLanguage(filename, prettyName) fallback")
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to register Kokoro in LangDB", t)
            // Swallow to avoid crashing callers; UI should surface failure if needed.
        }
    }

    /**
     * Try to call LangDB.registerKokoro(lang, country, name, modelType, speakerId, speed, volume)
     * using reflection so we don't hard depend on a specific LangDB version.
     */
    private fun tryRegisterKokoroAllArgs(langDb: LangDB, prettyName: String): Boolean {
        return try {
            val cls = langDb.javaClass
            val m = cls.getMethod(
                "registerKokoro",
                String::class.java,  // lang
                String::class.java,  // country
                String::class.java,  // name
                String::class.java,  // modelType
                String::class.java,  // speakerId
                java.lang.Float.TYPE, // speed (float)
                java.lang.Float.TYPE  // volume (float)
            )
            // Sensible defaults that match your app’s expectations
            val args = arrayOf(
                "en",            // lang
                "US",            // country
                prettyName,      // name shown in UI
                "kokoro",        // modelType tag
                "af",            // default speaker id (e.g., 'af' = adult female)
                1.0f,            // speed
                1.0f             // volume
            )
            m.invoke(langDb, *args)
            true
        } catch (_: NoSuchMethodException) {
            false
        } catch (t: Throwable) {
            Log.w(TAG, "registerKokoro(...) exists but invocation failed, will fallback", t)
            false
        }
    }
}
