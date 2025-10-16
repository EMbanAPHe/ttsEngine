package com.k2fsa.sherpa.onnx.tts.engine

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.k2fsa.sherpa.onnx.tts.engine.databinding.ActivityManageLanguagesBinding
import java.io.File

class ManageLanguagesActivity : AppCompatActivity() {
    private var binding: ActivityManageLanguagesBinding? = null

    // Import is intentionally no-op to match your “disable import” reset
    private val importFileLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { _: Uri? -> /* no-op */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageLanguagesBinding.inflate(layoutInflater)
        setContentView(binding!!.root)

        setupDownloadLists()
        setupImportedVoicesSection()
    }

    private fun setupDownloadLists() {
        // Piper / Coqui (existing behavior) — use findViewById so it compiles
        findViewById<ListView?>(R.id.piperModelList)?.let { lv ->
            val piper = resources.getStringArray(R.array.piper_models).toMutableList()
            lv.adapter = ArrayAdapter(this, R.layout.list_item, piper)
            lv.onItemClickListener = AdapterView.OnItemClickListener { _, _, pos, _ ->
                val url = resources.getStringArray(R.array.piper_model_urls)[pos]
                // delegate to existing installer if you have one; else show toast
                Toast.makeText(this, "Piper download not implemented here", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<ListView?>(R.id.coquiModelList)?.let { lv ->
            val coqui = resources.getStringArray(R.array.coqui_models).toMutableList()
            lv.adapter = ArrayAdapter(this, R.layout.list_item, coqui)
            lv.onItemClickListener = AdapterView.OnItemClickListener { _, _, _, _ ->
                Toast.makeText(this, "Coqui download not implemented here", Toast.LENGTH_SHORT).show()
            }
        }

        // Kokoro (new)
        findViewById<ListView?>(R.id.kokoroModelList)?.let { lv ->
            val kokoro = resources.getStringArray(R.array.kokoro_models).toMutableList()
            lv.adapter = ArrayAdapter(this, R.layout.list_item, kokoro)
            lv.onItemClickListener = AdapterView.OnItemClickListener { _, _, pos, _ ->
                val urlsPacked = resources.getStringArray(R.array.kokoro_model_urls)[pos]
                KokoroInstaller.install(this, urlsPacked)
            }
        }
    }

    private fun setupImportedVoicesSection() {
        refreshImportedList()
    }

    private fun refreshImportedList() {
        val db = LangDB.getInstance(this)
        val installed = db.allInstalledLanguages

        findViewById<ListView?>(R.id.importedList)?.let { lv ->
            val adapter = ImportedVoiceAdapter(this, installed)
            lv.adapter = adapter

            lv.onItemClickListener = AdapterView.OnItemClickListener { _, _, pos, _ ->
                PreferenceHelper(this).setCurrentLanguage(installed[pos].lang)
                Toast.makeText(this, "Active voice → ${installed[pos].lang}", Toast.LENGTH_SHORT).show()
            }

            lv.onItemLongClickListener = AdapterView.OnItemLongClickListener { _, _, pos, _ ->
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
    }

    private fun deleteVoice(lang: String, country: String) {
        File(getExternalFilesDir(null), lang + country).deleteRecursively()
        val db = LangDB.getInstance(this)
        try {
            db.deleteLanguage(lang)
        } catch (_: Throwable) {
        }
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
}
