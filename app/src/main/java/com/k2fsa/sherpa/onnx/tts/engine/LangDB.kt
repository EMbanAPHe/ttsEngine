package com.k2fsa.sherpa.onnx.tts.engine

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject

data class LanguageEntry(
    val name: String,
    val lang: String,
    val country: String,
    var speakerId: Int,
    var speed: Float,
    var volume: Float,
    val modelType: String = "piper",
    val folderName: String = lang + country
)

class LangDB private constructor(ctx: Context) {
    private val prefs = ctx.getSharedPreferences("lang_db", Context.MODE_PRIVATE)

    val allInstalledLanguages: List<LanguageEntry>
        get() = loadAll()

    fun updateLang(lang: String, speakerId: Int, speed: Float, volume: Float) {
        val list = loadAll().toMutableList()
        val idx = list.indexOfFirst { it.lang == lang }
        if (idx >= 0) {
            val e = list[idx]
            e.speakerId = speakerId
            e.speed = speed
            e.volume = volume
            saveAll(list)
        }
    }

    fun removeLang(lang: String?) {
        if (lang == null) return
        val list = loadAll().filterNot { it.lang == lang }
        saveAll(list)
    }

    fun deleteLanguage(lang: String) = removeLang(lang)

    fun addLanguage(name: String, lang: String, country: String, speakerId: Int, speed: Float, volume: Float, modelType: String) {
        val list = loadAll().toMutableList()
        if (list.none { it.lang == lang }) {
            list.add(LanguageEntry(name, lang, country, speakerId, speed, volume, modelType))
            saveAll(list)
        }
    }

    private fun loadAll(): List<LanguageEntry> {
        val raw = prefs.getString("lang_db_json", "[]") ?: "[]"
        val arr = JSONArray(raw)
        val out = ArrayList<LanguageEntry>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            out.add(
                LanguageEntry(
                    name = o.optString("name", "voice"),
                    lang = o.optString("lang", "en"),
                    country = o.optString("country", "US"),
                    speakerId = o.optInt("speakerId", 0),
                    speed = o.optDouble("speed", 1.0).toFloat(),
                    volume = o.optDouble("volume", 1.0).toFloat(),
                    modelType = o.optString("modelType", "piper"),
                    folderName = o.optString("folderName", o.optString("lang","en") + o.optString("country","US"))
                )
            )
        }
        return out
    }

    private fun saveAll(list: List<LanguageEntry>) {
        val arr = JSONArray()
        list.forEach { e ->
            val o = JSONObject()
            o.put("name", e.name)
            o.put("lang", e.lang)
            o.put("country", e.country)
            o.put("speakerId", e.speakerId)
            o.put("speed", e.speed.toDouble())
            o.put("volume", e.volume.toDouble())
            o.put("modelType", e.modelType)
            o.put("folderName", e.folderName)
            arr.put(o)
        }
        prefs.edit().putString("lang_db_json", arr.toString()).apply()
    }

    companion object {
        @Volatile private var instance: LangDB? = null
        fun getInstance(ctx: Context): LangDB {
            return instance ?: synchronized(this) {
                instance ?: LangDB(ctx.applicationContext).also { instance = it }
            }
        }
    }
}
