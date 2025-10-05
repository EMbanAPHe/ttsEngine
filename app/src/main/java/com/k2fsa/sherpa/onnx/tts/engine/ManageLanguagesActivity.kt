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
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.k2fsa.sherpa.onnx.tts.engine.databinding.ActivityManageLanguagesBinding
import java.io.File

class ManageLanguagesActivity  : AppCompatActivity() {
    // Candidate URLs for Kokoro model packs. The installer tries each in order until one works.
    // Replace/update if any mirror changes. Must point to .zip or .tar.bz2 archives that contain:
    // model.onnx, tokens.txt, voices.bin (or voices/), espeak-ng-data/
    private val KOKORO_URLS_INT8 = listOf(
        "https://huggingface.co/csukuangfj/kokoro-en-v0_19/resolve/main/kokoro-en-v0_19-int8.tar.bz2?download=true",
        "https://huggingface.co/k2-fsa/sherpa-onnx/resolve/main/kokoro/kokoro-en-v0_19-int8.tar.bz2?download=true"
    )
    private val KOKORO_URLS_FP16 = listOf(
        "https://huggingface.co/csukuangfj/kokoro-en-v0_19/resolve/main/kokoro-en-v0_19-fp16.tar.bz2?download=true",
        "https://huggingface.co/k2-fsa/sherpa-onnx/resolve/main/kokoro/kokoro-en-v0_19-fp16.tar.bz2?download=true"
    )
    private val KOKORO_URLS_FP32 = listOf(
        "https://huggingface.co/csukuangfj/kokoro-en-v0_19/resolve/main/kokoro-en-v0_19.tar.bz2?download=true",
        "https://huggingface.co/k2-fsa/sherpa-onnx/resolve/main/kokoro/kokoro-en-v0_19.tar.bz2?download=true"
    )

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
        // Kokoro spinner + install wiring
        binding?.spinnerKokoroQuality?.adapter =
            ArrayAdapter(this, R.layout.list_item, R.id.text_view, resources.getStringArray(R.array.kokoro_quality_labels).toList())

        binding?.buttonInstallKokoro?.setOnClickListener {
            val pos = binding?.spinnerKokoroQuality?.selectedItemPosition ?: 0
            val (urls, displayName) = when (pos) {
                0 -> KOKORO_URLS_INT8 to "Kokoro-en v0_19 (int8)"
                1 -> KOKORO_URLS_FP16 to "Kokoro-en v0_19 (fp16)"
                else -> KOKORO_URLS_FP32 to "Kokoro-en v0_19 (fp32)"
            }
            installKokoro(listOf(url), displayName)
        }
    }

    private fun setupDownloadLists() {
        // Keep existing download behavior; only change the adapter ctor to point at the TextView inside list_item.xml
        val piper = resources.getStringArray(R.array.piper_models).toMutableList()
        val coqui = resources.getStringArray(R.array.coqui_models).toMutableList()

        // IMPORTANT: Use the 3-arg ArrayAdapter so it binds to the TextView inside your row layout.
        binding?.piperModelList?.adapter =
            ArrayAdapter(this, R.layout.list_item, R.id.text_view, piper)

        binding?.coquiModelList?.adapter =
            ArrayAdapter(this, R.layout.list_item, R.id.text_view, coqui)
    }

    private fun setupImportedVoicesSection() {
        binding?.buttonImportLocal?.setOnClickListener { importFileLauncher.launch(arrayOf("*/*")) }
        binding?.buttonImportLocal?.setOnLongClickListener { importFolderLauncher.launch(null); true }
        refreshImportedList()
    }

    private fun refreshImportedList() {
        val db = LangDB.getInstance(this)
        val installed = db.allInstalledLanguages
        val labels = installed.map { "${it.lang}_${it.country}  •  ${it.name}" }

        // IMPORTANT: Same fix here—use the 3-arg constructor with the TextView id from list_item.xml.
        binding?.importedList?.adapter =
            ArrayAdapter(this, R.layout.list_item, R.id.text_view, labels)

        binding?.importedList?.onItemClickListener = AdapterView.OnItemClickListener { _, _, pos, _ ->
            PreferenceHelper(this).setCurrentLanguage(installed[pos].lang)
            Toast.makeText(this, "Active voice → ${installed[pos].lang}", Toast.LENGTH_SHORT).show()
        }
        binding?.importedList?.onItemLongClickListener = AdapterView.OnItemLongClickListener { _, _, pos, _ ->
            val entry = installed[pos]
            AlertDialog.Builder(this)
                .setTitle("Delete ${entry.name}?")
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
        // remove folder
        File(getExternalFilesDir(null), lang + country).deleteRecursively()
        // remove DB row
        val db = LangDB.getInstance(this)
        try { db.deleteLanguage(lang) } catch (_: Throwable) { /* ignore if method absent */ }
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

    private fun installKokoro(urls: List<String>, displayName: String) {
        binding?.textKokoroStatus?.text = "Downloading $displayName..."
        lifecycleScope.launch {
            try {
                val archive = KokoroInstaller.downloadFirstAvailable(this@ManageLanguagesActivity, urls)
                binding?.textKokoroStatus?.text = "Extracting ${archive.name}..."
                val outDir = KokoroInstaller.extractToModelsDir(this@ManageLanguagesActivity, archive)
                if (!KokoroInstaller.looksLikeKokoro(outDir)) {
                    binding?.textKokoroStatus?.text = "Extracted, but not a Kokoro pack."
                    return@launch
                }
                val db = LangDB.getInstance(this@ManageLanguagesActivity)
                val modelName = outDir.name
                val exists = db.allInstalledLanguages.any { it.lang == modelName }
                if (!exists) {
                    db.addLanguage(displayName, "en", "US", 0, 1.0f, 1.0f, "kokoro")
                }
                PreferenceHelper(this@ManageLanguagesActivity).setCurrentLanguage(modelName)
                binding?.textKokoroStatus?.text = "Installed: $displayName"
                refreshImportedList()
            } catch (e: Exception) {
                binding?.textKokoroStatus?.text = "Kokoro install failed: ${e.message}"
            }
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
