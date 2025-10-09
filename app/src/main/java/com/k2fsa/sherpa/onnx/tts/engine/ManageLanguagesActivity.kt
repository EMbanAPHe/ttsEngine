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
import com.k2fsa.sherpa.onnx.tts.engine.databinding.ActivityManageLanguagesBinding
import java.io.File

class ManageLanguagesActivity  : AppCompatActivity() {
    private var binding: ActivityManageLanguagesBinding? = null

    private val importFileLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? -> uri?.let { /* no-op: import disabled per reset */ } }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageLanguagesBinding.inflate(layoutInflater)
        setContentView(binding!!.root)

        setupDownloadLists()
        setupImportedVoicesSection()
    }

    private fun setupDownloadLists() {
        // Piper / Coqui (existing behavior)
        val piper = resources.getStringArray(R.array.piper_models).toMutableList()
        val coqui = resources.getStringArray(R.array.coqui_models).toMutableList()

        binding?.piperModelList?.adapter = ArrayAdapter(this, R.layout.list_item, piper)
        binding?.coquiModelList?.adapter = ArrayAdapter(this, R.layout.list_item, coqui)

        // Kokoro
        val kokoro = resources.getStringArray(R.array.kokoro_models).toMutableList()
        binding?.kokoroModelList?.adapter = ArrayAdapter(this, R.layout.list_item, kokoro)

        binding?.kokoroModelList?.onItemClickListener = AdapterView.OnItemClickListener { _, _, pos, _ ->
            val urls = resources.getStringArray(R.array.kokoro_model_urls)[pos]
            KokoroInstaller.install(this, urls)
        }
    }

    private fun setupImportedVoicesSection() {
        refreshImportedList()
    }

    private fun refreshImportedList() {
        val db = LangDB.getInstance(this)
        val installed = db.allInstalledLanguages
        val adapter = ImportedVoiceAdapter(this, installed)
        binding?.importedList?.adapter = adapter
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
        File(getExternalFilesDir(null), lang + country).deleteRecursively()
        val db = LangDB.getInstance(this)
        try { db.deleteLanguage(lang) } catch (_: Throwable) { /* ignore */ }
        if (PreferenceHelper(this).getCurrentLanguage() == lang) {
            PreferenceHelper(this).setCurrentLanguage("")
        }
    }

    fun startMain(view: View) {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finishAffinity()
    }
}