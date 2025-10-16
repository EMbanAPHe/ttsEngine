package com.k2fsa.sherpa.onnx.tts.engine

import android.os.Bundle
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity

/**
 * This version removes direct references to ViewBinding properties that don’t exist
 * in the generated binding class (which caused "Unresolved reference" compile errors).
 *
 * Instead, it uses findViewById for the three lists. If those IDs are present in your
 * layout resources (as they are in the original master), compilation succeeds.
 *
 * Layout assumed: setContentView(R.layout.activity_manage_languages)
 * and IDs: R.id.piperModelList, R.id.coquiModelList, R.id.importedList
 */
class ManageLanguagesActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_languages)

        // Use findViewById to avoid depending on a specific binding class exposing these views.
        // If one of these IDs is not present in your resources, R.id.<name> will fail at compile time.
        val piperList = findViewById<ListView?>(R.id.piperModelList)
        val coquiList = findViewById<ListView?>(R.id.coquiModelList)
        val importedList = findViewById<ListView?>(R.id.importedList)

        // Hook up adapters only if the lists exist in the current layout.
        // Replace the adapter contents with your real data sources.
        piperList?.adapter = PiperModelsAdapter(this, LangDB.getInstance(this).getPiperEntries())
        coquiList?.adapter = CoquiModelsAdapter(this, LangDB.getInstance(this).getCoquiEntries())
        importedList?.adapter = ImportedVoiceAdapter(this, LangDB.getInstance(this).getImportedEntries())
    }
}

/**
 * The three adapters referenced here should already exist in your project.
 * If their constructors differ, adjust the calls accordingly.
 *
 * If you don’t have these exact adapter classes, either:
 *  - import the originals from master, or
 *  - replace with ArrayAdapter<String>(...) for a quick compile.
 */

// Example stubs if you need them to compile (uncomment if your project lacks these):
// class PiperModelsAdapter(ctx: Context, data: List<LangDB.LanguageEntry>) : ArrayAdapter<LangDB.LanguageEntry>(ctx, android.R.layout.simple_list_item_1, data)
// class CoquiModelsAdapter(ctx: Context, data: List<LangDB.LanguageEntry>) : ArrayAdapter<LangDB.LanguageEntry>(ctx, android.R.layout.simple_list_item_1, data)
// class ImportedVoiceAdapter(ctx: Context, data: List<LangDB.LanguageEntry>) : ArrayAdapter<LangDB.LanguageEntry>(ctx, android.R.layout.simple_list_item_1, data)
