package com.k2fsa.sherpa.onnx.tts.engine

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class ImportLocalVoiceActivity : AppCompatActivity() {

    private val importFileLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) {
            Toast.makeText(this, "No file selected", Toast.LENGTH_SHORT).show()
            finish()
        } else {
            installFromUri(uri)
        }
    }

    private val importFolderLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri == null) {
            Toast.makeText(this, "No folder selected", Toast.LENGTH_SHORT).show()
            finish()
        } else {
            installFromTree(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Start with a file picker (zip) for most cases; user can use folder option from share menu or we can add a chooser later.
        importFileLauncher.launch(arrayOf("*/*"))
    }

    private fun installFromUri(uri: Uri) {
        val res = LocalModelInstaller.installFromUri(this, uri)
        handleResult(res)
    }

    private fun installFromTree(uri: Uri) {
        val res = LocalModelInstaller.installFromTree(this, uri)
        handleResult(res)
    }

    private fun handleResult(res: Result<LocalModelInstaller.ImportResult>) {
        res.onSuccess { r ->
            // Update DB and preferences
            val db = LangDB.getInstance(this)
            val ph = PreferenceHelper(this)
            // Add language if missing
            val existing = db.allInstalledLanguages.firstOrNull { it.lang == r.lang }
            if (existing == null) {
                db.addLanguage(r.modelName, r.lang, r.country, 0, 1.0f, 1.0f, r.modelType)
            }
            ph.setCurrentLanguage(r.lang)
            Toast.makeText(this, "Imported voice ${r.modelName} (${r.lang}_${r.country})", Toast.LENGTH_LONG).show()
            // Go to main UI
            startActivity(Intent(this, MainActivity::class.java))
        }.onFailure { e ->
            Toast.makeText(this, "Import failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
        finish()
    }
}
