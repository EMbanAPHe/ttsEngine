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
import com.k2fsa.sherpa.onnx.tts.engine.databinding.ActivityManageLanguagesBinding
import kotlinx.coroutines.launch
import java.io.File

class ManageLanguagesActivity  : AppCompatActivity() {
    private var binding: ActivityManageLanguagesBinding? = null

    // Kokoro mirrors
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
            installKokoro(urls, displayName)
        }
    }

    private fun setupDownloadLists() {
        val piper = resources.getStringArray(R.array.piper_models).toMutableList()
        val coqui = resources.getStringArray(R.array.coqui_models).toMutableList()

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
        val labels = installed.map { "${it.lang}_${it.country} • ${it.name}" }

        binding?.importedList?.adapter = ArrayAdapter(this, R.layout.list_item, R.id.text_view, labels)
        binding?.importedList?.onItemClickListener = AdapterView.OnItemClickListener { _, _, pos, _ ->
            val entry = installed[pos]
            PreferenceHelper(this).setCurrentLanguage(entry.lang)
            Toast.makeText(this, "Active voice → ${entry.lang}", Toast.LENGTH_SHORT).show()
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
        // remove folder (best-effort)
        File(getExternalFilesDir(null), lang + country).deleteRecursively()
        // Update DB
        val db = LangDB.getInstance(this)
        db.deleteLanguage(lang)
        if (PreferenceHelper(this).getCurrentLanguage() == lang) {
            PreferenceHelper(this).setCurrentLanguage("")
        }
    }

    private fun installFromUri(uri: Uri) {
        val res = LocalModelInstaller.installFromUri(this, uri)
        handleImportResult(Result.success(res))
    }

    private fun installFromTree(uri: Uri) {
        val res = LocalModelInstaller.installFromTree(this, uri)
        handleImportResult(Result.success(res))
    }

    private fun handleImportResult(res: Result<LocalModelInstaller.ImportResult>) {
        res.onSuccess { r ->
            val db = LangDB.getInstance(this)
            val existing = db.allInstalledLanguages.firstOrNull { it.lang == r.lang }
            if (existing == null) {
                // Infer model type from files the LocalModelInstaller detected (coqui/piper)
                val modelType = if (r.modelName.lowercase().contains("kokoro")) "kokoro"
                                else if (r.modelName.lowercase().contains("piper")) "piper"
                                else "coqui"
                db.addLanguage(r.modelName, r.lang, r.country, 0, 1.0f, 1.0f, modelType)
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
                val modelName = outDir.name // e.g. kokoro-en-v0_19
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

    fun testVoices(@Suppress("UNUSED_PARAMETER") view: View) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://huggingface.co/spaces/k2-fsa/text-to-speech/")))
    }
}
