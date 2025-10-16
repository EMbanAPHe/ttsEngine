package com.k2fsa.sherpa.onnx.tts.engine

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import com.k2fsa.sherpa.onnx.tts.engine.databinding.ActivityManageLanguagesBinding
import com.k2fsa.sherpa.onnx.tts.engine.db.LangDB

class ManageLanguagesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityManageLanguagesBinding

    // Must be var (we reassign lists after DB reloads)
    private var piperModelList: MutableList<String> = mutableListOf()
    private var coquiModelList: MutableList<String> = mutableListOf()
    private var importedList: MutableList<LangDB.LanguageEntry> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageLanguagesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        reloadLists()

        // Piper
        binding.piperModelsLabel.text = getString(R.string.piper_models)
        binding.piperModels.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            piperModelList
        )

        // Coqui
        binding.coquiModelsLabel.text = getString(R.string.coqui_models)
        binding.coquiModels.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            coquiModelList
        )

        // Imported (all engines) — uses our adapter with LanguageEntry
        binding.importedLabel.visibility = View.VISIBLE
        binding.imported.adapter = ImportedVoiceAdapter(
            this,
            android.R.layout.simple_list_item_1,
            importedList
        )

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
        piperModelList = LangDB.getModelDisplayList(this, engine = "piper").toMutableList()
        coquiModelList = LangDB.getModelDisplayList(this, engine = "coqui").toMutableList()
        importedList = LangDB.getAllLanguages(this).toMutableList()
    }
}
