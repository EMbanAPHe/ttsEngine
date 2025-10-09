package com.k2fsa.sherpa.onnx.tts.engine

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.k2fsa.sherpa.onnx.tts.engine.databinding.ActivityManageLanguagesBinding

class ManageLanguagesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityManageLanguagesBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageLanguagesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupSpinners()
        setupButtons()
        refreshImportedList()
    }

    private fun setupSpinners() {
        // Piper
        ArrayAdapter.createFromResource(
            this,
            R.array.piper_models,
            R.layout.spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerPiper.adapter = adapter
        }

        // Coqui
        ArrayAdapter.createFromResource(
            this,
            R.array.coqui_models,
            R.layout.spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerCoqui.adapter = adapter
        }

        // Kokoro (new)
        ArrayAdapter.createFromResource(
            this,
            R.array.kokoro_models,
            R.layout.spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerKokoro.adapter = adapter
        }
    }

    private fun setupButtons() {
        // Install button uses whichever tab/section the user actually touched.
        binding.buttonInstall.setOnClickListener {
            val tab = binding.installSourceTabs.checkedButtonId
            when (tab) {
                R.id.tab_piper -> {
                    val selection = binding.spinnerPiper.selectedItem?.toString() ?: ""
                    if (selection.isBlank()) {
                        Toast.makeText(this, "Select a Piper model", Toast.LENGTH_SHORT).show()
                    } else {
                        Downloader.startDownload(this, selection, Downloader.Source.PIPER)
                    }
                }
                R.id.tab_coqui -> {
                    val selection = binding.spinnerCoqui.selectedItem?.toString() ?: ""
                    if (selection.isBlank()) {
                        Toast.makeText(this, "Select a Coqui model", Toast.LENGTH_SHORT).show()
                    } else {
                        Downloader.startDownload(this, selection, Downloader.Source.COQUI)
                    }
                }
                R.id.tab_kokoro -> {
                    val selection = binding.spinnerKokoro.selectedItem?.toString() ?: ""
                    if (selection.isBlank()) {
                        Toast.makeText(this, "Select a Kokoro model", Toast.LENGTH_SHORT).show()
                    } else {
                        Downloader.startDownload(this, selection, Downloader.Source.KOKORO)
                    }
                }
                else -> {
                    Toast.makeText(this, "Choose a source tab first", Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.buttonStart.setOnClickListener { startMain(it) }
    }

    private fun refreshImportedList() {
        val db = LangDB.getInstance(this)
        val installed = db.allInstalledLanguages
        val labels = installed.map { "${it.lang}_${it.country}  •  ${it.name}" }

        val adapter = ArrayAdapter(this, R.layout.list_item, labels)
        binding.importedList.adapter = adapter

        binding.importedList.setOnItemClickListener { _, _, pos, _ ->
            val entry = installed[pos]
            PreferenceHelper(this).setCurrentLanguage(entry.lang)
            Toast.makeText(this, "Active voice → ${entry.lang}", Toast.LENGTH_SHORT).show()
        }

        binding.importedList.setOnItemLongClickListener { _, _, pos, _ ->
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
        VoiceStorage.deleteVoiceFolder(this, lang, country)
        val db = LangDB.getInstance(this)
        db.deleteLanguage(lang)
        if (PreferenceHelper(this).getCurrentLanguage() == lang) {
            PreferenceHelper(this).setCurrentLanguage("")
        }
    }

    fun startMain(@Suppress("UNUSED_PARAMETER") view: View) {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finishAffinity()
    }

    fun testVoices(@Suppress("UNUSED_PARAMETER") view: View) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://huggingface.co/spaces/k2-fsa/text-to-speech/")))
    }
}
