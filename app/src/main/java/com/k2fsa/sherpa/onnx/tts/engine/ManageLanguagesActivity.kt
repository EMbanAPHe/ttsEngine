package com.k2fsa.sherpa.onnx.tts.engine

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.k2fsa.sherpa.onnx.tts.engine.databinding.ActivityManageLanguagesBinding
import java.io.File

class ManageLanguagesActivity  : AppCompatActivity() {
    private var binding: ActivityManageLanguagesBinding? = null

    private val importFileLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? -> uri?.let { installFromUri(it) } }

    private val importFolderLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? -> uri?.let { installFromTree(it) } }

    private var selectedModelId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageLanguagesBinding.inflate(layoutInflater)
        setContentView(binding!!.root)

        setupDownloadSpinners()
        setupImportedVoicesSection()

        binding!!.buttonImportLocal.setOnClickListener { importFileLauncher.launch(arrayOf("*/*")) }
        binding!!.buttonImportLocal.setOnLongClickListener { importFolderLauncher.launch(null); true }

        binding!!.buttonInstallModel.setOnClickListener {
            val id = selectedModelId
            if (id.isNullOrBlank()) {
                Toast.makeText(this, R.string.select_a_model_first, Toast.LENGTH_SHORT).show()
            } else {
                try {
                    Downloader.startDownload(this, id)
                } catch (e: Throwable) {
                    Toast.makeText(this, "Downloader error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun setupDownloadSpinners() {
        val piper = resources.getStringArray(R.array.piper_models).toMutableList()
        val coqui = resources.getStringArray(R.array.coqui_models).toMutableList()
        val kokoro = resources.getStringArray(R.array.kokoro_models).toMutableList()

        binding!!.spinnerPiper.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, piper)
        binding!!.spinnerCoqui.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, coqui)
        binding!!.spinnerKokoro.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, kokoro)

        val listener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedModelId = parent.getItemAtPosition(position)?.toString()
            }
            override fun onNothingSelected(parent: AdapterView<*>) { /* no-op */ }
        }
        binding!!.spinnerPiper.onItemSelectedListener = listener
        binding!!.spinnerCoqui.onItemSelectedListener = listener
        binding!!.spinnerKokoro.onItemSelectedListener = listener
    }

    private fun setupImportedVoicesSection() {
        binding!!.importedRecycler.layoutManager = LinearLayoutManager(this)
        val adapter = ImportedVoiceAdapter(emptyList(),
            onClick = { row ->
                PreferenceHelper(this).setCurrentLanguage(row.lang)
                Toast.makeText(this, getString(R.string.active_voice_fmt, row.lang), Toast.LENGTH_SHORT).show()
            },
            onLongClick = { row ->
                AlertDialog.Builder(this)
                    .setTitle(getString(R.string.delete_voice_title_fmt, row.name))
                    .setMessage(R.string.delete_voice_msg)
                    .setPositiveButton(R.string.delete) { _, _ ->
                        deleteVoice(row.lang, row.country)
                        refreshImportedList()
                    }
                    .setNegativeButton(R.string.cancel, null)
                    .show()
            }
        )
        binding!!.importedRecycler.adapter = adapter
        refreshImportedList()
    }

    private fun refreshImportedList() {
        val db = LangDB.getInstance(this)
        val installed = db.allInstalledLanguages
        val rows = installed.map { LangRow(it.name, it.lang, it.country) }
        val adapter = binding!!.importedRecycler.adapter as ImportedVoiceAdapter
        adapter.submitList(rows)
    }

    private fun deleteVoice(lang: String, country: String) {
        File(getExternalFilesDir(null), lang + country).deleteRecursively()
        val db = LangDB.getInstance(this)
        try { db.deleteLanguage(lang) } catch (_: Throwable) { }
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