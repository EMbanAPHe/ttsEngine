package com.k2fsa.sherpa.onnx.tts.engine

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.ArrayAdapter
import com.k2fsa.sherpa.onnx.tts.engine.databinding.ActivityManageLanguagesBinding

/**
 * Refactored to use ViewBinding everywhere.
 * Removed bare references like `piperModelList`, `coquiModelList`, `importedList`.
 * Use `binding.piperModelList` etc. so the compiler finds the views.
 */
class ManageLanguagesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityManageLanguagesBinding

    private lateinit var piperAdapter: ArrayAdapter<String>
    private lateinit var coquiAdapter: ArrayAdapter<String>
    private lateinit var importedAdapter: ImportedVoiceAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageLanguagesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Piper built-in list
        val piperItems = LangDB.getBuiltinPiperDisplayList(this)
        piperAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, piperItems)
        binding.piperModelList.adapter = piperAdapter

        // Coqui built-in list
        val coquiItems = LangDB.getBuiltinCoquiDisplayList(this)
        coquiAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, coquiItems)
        binding.coquiModelList.adapter = coquiAdapter

        // Imported (user) voices
        val imported = LangDB.getImportedVoices(this)  // returns List<LangDB.LanguageEntry>
        importedAdapter = ImportedVoiceAdapter(this, imported)
        binding.importedList.adapter = importedAdapter

        // Optional: item clicks
        binding.piperModelList.setOnItemClickListener { _, _, position, _ ->
            LangDB.installBuiltinPiperByIndex(this, position)
        }
        binding.coquiModelList.setOnItemClickListener { _, _, position, _ ->
            LangDB.installBuiltinCoquiByIndex(this, position)
        }
        binding.importedList.setOnItemClickListener { _, _, position, _ ->
            val entry = importedAdapter.getItem(position)
            if (entry != null) LangDB.selectLanguage(this, entry.languageId)
        }
    }
}
