package com.k2fsa.sherpa.onnx.tts.engine

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity

class ManageLanguagesActivity : AppCompatActivity() {

    private lateinit var piperList: ListView
    private lateinit var coquiList: ListView
    private lateinit var importedList: ListView
    private var installKokoroBtn: Button? = null
    private var kokoroProgress: ProgressBar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_languages)

        // These IDs match the original master layout
        piperList     = findViewById(R.id.piperModelList)
        coquiList     = findViewById(R.id.coquiModelList)
        importedList  = findViewById(R.id.importedList)
        // Optional controls if you added them to the layout:
        installKokoroBtn = findViewById(R.id.buttonInstallKokoro)
        kokoroProgress   = findViewById(R.id.progressKokoro)

        // Piper list
        piperList.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            LangDB.getPiperDisplayList(this)
        )
        piperList.setOnItemClickListener { _, _, position, _ ->
            LangDB.getPiperEntryAt(this, position)?.let { LangDB.toggleInstalled(this, it) }
            refresh()
        }

        // Coqui list
        coquiList.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            LangDB.getCoquiDisplayList(this)
        )
        coquiList.setOnItemClickListener { _, _, position, _ ->
            LangDB.getCoquiEntryAt(this, position)?.let { LangDB.toggleInstalled(this, it) }
            refresh()
        }

        // Imported languages list
        importedList.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            LangDB.getImportedDisplayList(this)
        )
        importedList.setOnItemClickListener { _, _, position, _ ->
            LangDB.getImportedEntryAt(this, position)?.let { LangDB.toggleInstalled(this, it) }
            refresh()
        }

        // Install Kokoro on demand
        installKokoroBtn?.setOnClickListener {
            installKokoroBtn?.isEnabled = false
            kokoroProgress?.visibility = View.VISIBLE
            KokoroInstaller.installAll(this@ManageLanguagesActivity)
            it.postDelayed({
                kokoroProgress?.visibility = View.GONE
                installKokoroBtn?.isEnabled = true
                refresh()
            }, 1500)
        }
    }

    private fun refresh() {
        (piperList.adapter as? ArrayAdapter<String>)?.let {
            it.clear(); it.addAll(LangDB.getPiperDisplayList(this)); it.notifyDataSetChanged()
        }
        (coquiList.adapter as? ArrayAdapter<String>)?.let {
            it.clear(); it.addAll(LangDB.getCoquiDisplayList(this)); it.notifyDataSetChanged()
        }
        (importedList.adapter as? ArrayAdapter<String>)?.let {
            it.clear(); it.addAll(LangDB.getImportedDisplayList(this)); it.notifyDataSetChanged()
        }
    }
}
