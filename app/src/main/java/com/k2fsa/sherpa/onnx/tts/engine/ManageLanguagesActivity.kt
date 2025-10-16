package com.k2fsa.sherpa.onnx.tts.engine

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import com.k2fsa.sherpa.onnx.tts.engine.databinding.ActivityManageLanguagesBinding
import com.k2fsa.sherpa.onnx.tts.engine.db.LangDB

/**
 * Lets the user see what models are installed (Piper, Coqui, Kokoro), and remove them.
 * Restores mutable lists and wiring that were missing/renamed causing compile errors.
 */
class ManageLanguagesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityManageLanguagesBinding

    // These must be 'var' so we can reassign the lists after DB queries.
    private var piperModelList: MutableList<String> = mutableListOf()
    private var coquiModelList: MutableList<String> = mutableListOf()
    private var importedList: MutableList<LangDB.LanguageEntry> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageLanguagesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Fill lists from DB
        reloadLists()

        // Show Piper models
        binding.piperModelsLabel.text = getString(R.string.piper_models)
        binding.piperModels.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            piperModelList
        )

        // Show Coqui models
        binding.coquiModelsLabel.text = getString(R.string.coqui_models)
        binding.coquiModels.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            coquiModelList
        )

        // Show “Imported” (all engines) as language entries
        binding.importedLabel.visibility = View.VISIBLE
        binding.imported.adapter = ImportedVoiceAdapter(
            this,
            android.R.layout.simple_list_item_1,
            importedList
        )

        // Delete selected imported entry
        binding.deleteImported.setOnClickListener {
            val pos = binding.imported.checkedItemPosition
            if (pos >= 0 && pos < importedList.size) {
                val toDelete = importedList[pos]
                LangDB.deleteLanguage(this, toDelete)
                reloadLists()
                (binding.imported.adapter as ImportedVoiceAdapter).apply {
                    clear()
                    addAll(importedList)
                    notifyDataSetChanged()
                }
            }
        }
    }

    private fun reloadLists() {
        // Piper / Coqui lists are simple strings for display
        piperModelList = LangDB.getModelDisplayList(this, engine = "piper").toMutableList()
        coquiModelList = LangDB.getModelDisplayList(this, engine = "coqui").toMutableList()

        // The “imported” list shows every installed language entry across engines
        importedList = LangDB.getAllLanguages(this).toMutableList()
    }
}
