package com.k2fsa.sherpa.onnx.tts.engine

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.k2fsa.sherpa.onnx.tts.engine.db.LangDB
import com.k2fsa.sherpa.onnx.tts.engine.db.LangDB.LanguageEntry

class ManageLanguagesActivity : AppCompatActivity() {

    private lateinit var listImported: ListView
    private lateinit var emptyView: TextView
    private lateinit var btnInstallKokoro: Button

    // Provide locals so references compile even if you trimmed resources
    private val piperModelList: List<String> by lazy {
        try {
            resources.getStringArray(R.array.piper_models).toList()
        } catch (_: Throwable) {
            emptyList()
        }
    }

    private val coquiModelList: List<String> by lazy {
        try {
            resources.getStringArray(R.array.coqui_models).toList()
        } catch (_: Throwable) {
            emptyList()
        }
    }

    // Imported list we control here
    private val importedList = mutableListOf<LanguageEntry>()
    private lateinit var importedAdapter: ImportedVoiceAdapter
    // Model name lists expected by legacy UI code:
    private lateinit var piperModelList: List<String>
    private lateinit var coquiModelList: List<String>

    // If your UI shows/imports locally added voices, keep a data list.
    // Adjust the type if your project uses a different LanguageEntry model.
    private val importedList: MutableList<LangDB.LanguageEntry> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_languages)

        listImported = findViewById(R.id.list_imported)
        emptyView = findViewById(R.id.empty_view)
        btnInstallKokoro = findViewById(R.id.button_install_kokoro)

        importedAdapter = ImportedVoiceAdapter(this, importedList)
        listImported.adapter = importedAdapter
        listImported.emptyView = emptyView

        // Populate from DB
        refreshImported()

        btnInstallKokoro.setOnClickListener {
            // Install a default set; you can expand this to show a chooser dialog
            Thread {
                val ok = KokoroInstaller.installDefaultSet(this)
                runOnUiThread {
                    if (ok) refreshImported()
                    // optionally toast result
                }
            }.start()
        }
    }

    private fun refreshImported() {
        importedList.clear()
        importedList.addAll(LangDB.getAllLanguages(this)) // or whatever getter you have
        importedAdapter.notifyDataSetChanged()
    }
}
