package com.k2fsa.sherpa.onnx.tts.engine

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.k2fsa.sherpa.onnx.tts.engine.databinding.ActivityManageLanguagesBinding

class ManageLanguagesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityManageLanguagesBinding

    private lateinit var piperAdapter: DownloadableModelAdapter
    private lateinit var coquiAdapter: DownloadableModelAdapter
    private lateinit var importedAdapter: ImportedVoiceAdapter

    private val kokoro by lazy { KokoroInstaller(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageLanguagesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Lists (IDs match original master layout)
        binding.piperModelList.layoutManager = LinearLayoutManager(this)
        binding.coquiModelList.layoutManager = LinearLayoutManager(this)
        binding.importedList.layoutManager = LinearLayoutManager(this)

        // Piper (original behavior)
        piperAdapter = DownloadableModelAdapter(
            this,
            LangDB.piperCatalog(),
            onInstall = { lang, name, url ->
                PiperInstaller(this).installPiper(lang, name, url)
            }
        )
        binding.piperModelList.adapter = piperAdapter

        // Coqui (original behavior)
        coquiAdapter = DownloadableModelAdapter(
            this,
            LangDB.coquiCatalog(),
            onInstall = { lang, name, url ->
                CoquiInstaller(this).installCoqui(lang, name, url)
            }
        )
        binding.coquiModelList.adapter = coquiAdapter

        // Imported voices (includes Kokoro after install)
        importedAdapter = ImportedVoiceAdapter(this, LangDB.listImportedVoices(this))
        binding.importedList.adapter = importedAdapter

        // Example Kokoro install trigger (wire to your actual button/menu if present)
        // This shows 82M; add more buttons for the other sizes if you like.
        // If you don't have such a button in the layout, delete this block.
        binding.buttonInstallKokoroSmall?.setOnClickListener {
            kokoro.installKokoro(
                language = "en",
                modelName = "Kokoro-82M-v1.0",
                modelUrl = "https://huggingface.co/onnx-community/Kokoro-82M-v1.0-ONNX/resolve/main/onnx/kokoro-v1_0.onnx?download=true"
            )
        }
    }

    fun refreshLists() {
        importedAdapter.submitList(LangDB.listImportedVoices(this))
        importedAdapter.notifyDataSetChanged()
    }

    fun toast(msg: String) {
        android.widget.Toast.makeText(this, msg, android.widget.Toast.LENGTH_SHORT).show()
    }
}
