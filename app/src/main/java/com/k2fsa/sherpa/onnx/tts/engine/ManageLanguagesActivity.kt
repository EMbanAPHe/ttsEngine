package com.k2fsa.sherpa.onnx.tts.engine

import android.app.AlertDialog
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.k2fsa.sherpa.onnx.tts.engine.databinding.ActivityManageLanguagesBinding
import java.io.File

class ManageLanguagesActivity : AppCompatActivity() {
    private var binding: ActivityManageLanguagesBinding? = null

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

        setupDownloadLists()
        setupImportedVoicesSection()
        setupKokoroSpinner()
        setupImportButtons()
    }

    private fun setupImportButtons() {
        binding?.buttonImportLocal?.setOnClickListener {
            importFileLauncher.launch(arrayOf("*/*"))
        }
        binding?.buttonImportLocal?.setOnLongClickListener {
            importFolderLauncher.launch(null); true
        }
    }

    private fun setupDownloadLists() {
        // Wire piper/coqui arrays from resources to simple lists
        val piper = resources.getStringArray(R.array.piper_models).toMutableList()
        val coqui = resources.getStringArray(R.array.coqui_models).toMutableList()
        binding?.piperModelList?.adapter = ArrayAdapter(this, R.layout.list_item, piper)
        binding?.coquiModelList?.adapter = ArrayAdapter(this, R.layout.list_item, coqui)
        binding?.piperModelList?.onItemClickListener = AdapterView.OnItemClickListener { _, _, pos, _ ->
            Toast.makeText(this, "Selected Piper: ${'$'}{piper[pos]}", Toast.LENGTH_SHORT).show()
        }
        binding?.coquiModelList?.onItemClickListener = AdapterView.OnItemClickListener { _, _, pos, _ ->
            Toast.makeText(this, "Selected Coqui: ${'$'}{coqui[pos]}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupKokoroSpinner() {
        val kokoroNames = resources.getStringArray(R.array.kokoro_models)
        val kokoroUrls = resources.getStringArray(R.array.kokoro_model_urls)

        val spinner = binding?.kokoroModels
        spinner?.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, kokoroNames.toList())
        spinner?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                // no-op; use Install button
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        binding?.buttonInstall?.setOnClickListener {
            val pos = spinner?.selectedItemPosition ?: 0
            val url = kokoroUrls.getOrNull(pos)
            val name = kokoroNames.getOrNull(pos)
            if (url.isNullOrBlank() || name.isNullOrBlank()) {
                Toast.makeText(this, "No Kokoro model selected", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            startDownload(url, name)
        }
    }

    private fun refreshImportedList() {
        val db = LangDB.getInstance(this)
        val installed = db.allInstalledLanguages
        val labels = installed.map { "${'$'}{it.lang}_${'$'}{it.country}  •  ${'$'}{it.name}" }
        binding?.importedList?.adapter = ImportedVoiceAdapter(this, labels)
        binding?.importedList?.onItemClickListener = AdapterView.OnItemClickListener { _, _, pos, _ ->
            PreferenceHelper(this).setCurrentLanguage(installed[pos].lang)
            Toast.makeText(this, "Active voice → ${'$'}{installed[pos].lang}", Toast.LENGTH_SHORT).show()
        }
        binding?.importedList?.onItemLongClickListener = AdapterView.OnItemLongClickListener { _, _, pos, _ ->
            val entry = installed[pos]
            AlertDialog.Builder(this)
                .setTitle("Delete ${'$'}{entry.name}?")
                .setMessage("Remove this voice and its files?")
                .setPositiveButton("Delete") { _, _ ->
                    deleteVoice(entry.lang, entry.country)
                    refreshImportedList()
                }
                .setNegativeButton("Cancel", null)
                .show()
            true
        }
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
            Toast.makeText(this, "Imported ${'$'}{r.modelName} (${ '$'}{r.lang}_${ '$'}{r.country})", Toast.LENGTH_LONG).show()
            refreshImportedList()
        }.onFailure { e ->
            Toast.makeText(this, "Import failed: ${'$'}{e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun startDownload(url: String, modelName: String) {
        try {
            val req = DownloadManager.Request(Uri.parse(url))
                .setAllowedOverMetered(true)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setTitle("Downloading ${'$'}modelName")
                .setDestinationInExternalFilesDir(this, null, "kokoro/${'$'}modelName.tar");
            val dm = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            dm.enqueue(req)
            Toast.makeText(this, "Started download: ${'$'}modelName", Toast.LENGTH_LONG).show()
        } catch (t: Throwable) {
            Toast.makeText(this, "Download failed: ${'$'}{t.message}", Toast.LENGTH_LONG).show()
        }
    }

    fun startMain(@Suppress("UNUSED_PARAMETER") view: View) {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finishAffinity()
    }
}
