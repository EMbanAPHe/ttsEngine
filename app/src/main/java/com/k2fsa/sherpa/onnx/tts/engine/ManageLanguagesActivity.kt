package com.k2fsa.sherpa.onnx.tts.engine

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.k2fsa.sherpa.onnx.tts.engine.databinding.ActivityManageLanguagesBinding

class ManageLanguagesActivity : AppCompatActivity() {
    private var binding: ActivityManageLanguagesBinding? = null
    private var selectedModelId: String? = null
    private lateinit var importedAdapter: ImportedVoiceAdapter

    private val importFileLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? -> uri?.let { installFromUri(it) } }

    private val importFolderLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? -> uri?.let { installFromTree(it) } }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageLanguagesBinding.inflate(layoutInflater)
        setContentView(binding!!.root)

        setupModelSpinners()
        setupImportedVoicesSection()

        binding?.buttonInstallModel?.setOnClickListener {
            val id = selectedModelId
            if (id.isNullOrEmpty()) {
                Toast.makeText(this, "Pick a model first", Toast.LENGTH_SHORT).show()
            } else {
                Downloader.startDownload(this, id)
            }
        }
        binding?.button_import_local?.setOnClickListener { importFileLauncher.launch(arrayOf("*/*")) }
        binding?.button_import_local?.setOnLongClickListener { importFolderLauncher.launch(null); true }
    }

    private fun setupModelSpinners() {
        // Piper
        val piper = resources.getStringArray(R.array.piper_models).toMutableList()
        binding?.spinnerPiper?.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, piper)
        binding?.spinnerPiper?.setOnItemSelectedListenerCompat { pos ->
            selectedModelId = piper[pos]
        }

        // Coqui
        val coqui = resources.getStringArray(R.array.coqui_models).toMutableList()
        binding?.spinnerCoqui?.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, coqui)
        binding?.spinnerCoqui?.setOnItemSelectedListenerCompat { pos ->
            selectedModelId = coqui[pos]
        }

        // Kokoro
        val kokoro = resources.getStringArray(R.array.kokoro_models).toMutableList()
        binding?.spinnerKokoro?.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, kokoro)
        binding?.spinnerKokoro?.setOnItemSelectedListenerCompat { pos ->
            selectedModelId = kokoro[pos]
        }
    }

    private fun setupImportedVoicesSection() {
        importedAdapter = ImportedVoiceAdapter(emptyList(),
            onClick = { entry ->
                PreferenceHelper(this).setCurrentLanguage(entry.lang)
                Toast.makeText(this, "Active voice → ${entry.lang}", Toast.LENGTH_SHORT).show()
            },
            onLongClick = { entry ->
                AlertDialog.Builder(this)
                    .setTitle("Delete ${entry.name}?")
                    .setMessage("Remove this voice and its files?")
                    .setPositiveButton("Delete") { _, _ ->
                        deleteVoice(entry.lang, entry.country)
                        refreshImportedList()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            })

        binding?.recyclerImported?.apply {
            layoutManager = LinearLayoutManager(this@ManageLanguagesActivity)
            adapter = importedAdapter
            isNestedScrollingEnabled = false
        }

        refreshImportedList()
    }

    private fun refreshImportedList() {
        val db = LangDB.getInstance(this)
        importedAdapter.submit(db.allInstalledLanguages)
    }

    private fun deleteVoice(lang: String, country: String) {
        // remove folder
        val dir = getExternalFilesDir(null)
        java.io.File(dir, lang + country).deleteRecursively()
        // remove DB row
        val db = LangDB.getInstance(this)
        try { db.deleteLanguage(lang) } catch (_: Throwable) {}
        if (PreferenceHelper(this).getCurrentLanguage() == lang) {
            PreferenceHelper(this).setCurrentLanguage("")
        }
    }

    private fun installFromUri(uri: Uri) {
        val res = LocalModelInstaller.installFromUri(this, uri)
        handleImportResult(res)
    }

    private fun installFromTree(uri: Uri) {
        val res = LocalModelInstaller.installFromTree(this, uri)
        handleImportResult(res)
    }

    private fun handleImportResult(res: Result<LocalModelInstaller.ImportResult>) {
        res.onSuccess { r ->
            val db = LangDB.getInstance(this)
            val existing = db.allInstalledLanguages.firstOrNull { it.lang == r.lang }
            if (existing == null) {
                db.addLanguage(r.modelName, r.lang, r.country, 0, 1.0f, 1.0f, r.modelType)
            }
            PreferenceHelper(this).setCurrentLanguage(r.lang)
            Toast.makeText(this, "Imported ${r.modelName} (${r.lang}_${r.country})", Toast.LENGTH_LONG).show()
            refreshImportedList()
        }.onFailure { e ->
            Toast.makeText(this, "Import failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    fun startMain(view: View) {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finishAffinity()
    }

    fun testVoices(view: View) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://huggingface.co/spaces/k2-fsa/text-to-speech/")))
    }
}

// Small helper to make Spinner selection listener concise
import android.widget.AdapterView
import android.widget.Spinner

private fun Spinner.setOnItemSelectedListenerCompat(onSelected: (pos: Int) -> Unit) {
    this.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
            onSelected(position)
        }
        override fun onNothingSelected(parent: AdapterView<*>) {}
    }
}