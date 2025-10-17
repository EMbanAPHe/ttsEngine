package com.k2fsa.sherpa.onnx.tts.engine

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import android.view.View

class ManageLanguagesActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        inflateAnyManageLayout()

        // Try to locate the three lists by any of the IDs used across master / forks
        val piperList = findViewByAnyId<ListView>(
            "piperModelList", "listPiperModels", "piper_list"
        )
        val coquiList = findViewByAnyId<ListView>(
            "coquiModelList", "listCoquiModels", "coqui_list"
        )
        val importedListView = findViewByAnyId<ListView>(
            "importedList", "listImportedVoices", "imported_list"
        )

        // Fallback to RecyclerView if your layout uses RecyclerViews instead of ListViews
        val piperRecycler = piperList ?: findViewByAnyId<RecyclerView>(
            "piperModelList", "rvPiperModels", "recyclerPiper"
        )
        val coquiRecycler = coquiList ?: findViewByAnyId<RecyclerView>(
            "coquiModelList", "rvCoquiModels", "recyclerCoqui"
        )
        val importedRecycler = importedListView ?: findViewByAnyId<RecyclerView>(
            "importedList", "rvImported", "recyclerImported"
        )

        // Load display names from resources if they exist, otherwise use empty arrays (compiles+runs)
        val piperNames = safeStringArray("piper_model_names")
        val coquiNames = safeStringArray("coqui_model_names")
        val importedNames = safeStringArray("imported_voice_names")

        // Hook up adapters for whichever widgets we actually found
        piperList?.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, piperNames)
        coquiList?.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, coquiNames)
        importedListView?.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, importedNames)

        (piperRecycler)?.adapter = SimpleStringRecyclerAdapter(piperNames)
        (coquiRecycler)?.adapter = SimpleStringRecyclerAdapter(coquiNames)
        (importedRecycler)?.adapter = SimpleStringRecyclerAdapter(importedNames)
    }

    /** Inflate any of the common layouts used by the original and your fork. */
    private fun inflateAnyManageLayout() {
        val candidates = arrayOf(
            "activity_manage_languages",
            "activity_manage_langs",
            "activity_manage_languages_patch"
        )
        for (name in candidates) {
            val id = resources.getIdentifier(name, "layout", packageName)
            if (id != 0) {
                setContentView(id)
                return
            }
        }
        // As a last resort, use the original expected name to at least throw a clear error if truly missing.
        val fallback = resources.getIdentifier("activity_manage_languages", "layout", packageName)
        if (fallback != 0) {
            setContentView(fallback)
        } else {
            error("ManageLanguages layout not found. Make sure a manage-languages layout exists.")
        }
    }

    /** Find a view by trying a set of plausible ID names. */
    private inline fun <reified T : View> findViewByAnyId(vararg ids: String): T? {
        for (name in ids) {
            val id = resources.getIdentifier(name, "id", packageName)
            if (id != 0) {
                val v = findViewById<T?>(id)
                if (v != null) return v
            }
        }
        return null
    }

    /** Get string-array if present, else empty. */
    private fun safeStringArray(arrayName: String): Array<String> {
        val id = resources.getIdentifier(arrayName, "array", packageName)
        return if (id != 0) resources.getStringArray(id) else emptyArray()
    }
}

/** Minimal recycler adapter for lists of strings (works if your layout uses RecyclerViews). */
private class SimpleStringRecyclerAdapter(private val items: Array<String>) :
    RecyclerView.Adapter<SimpleStringViewHolder>() {

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): SimpleStringViewHolder {
        val tv = android.widget.TextView(parent.context)
        tv.setPadding(24, 24, 24, 24)
        return SimpleStringViewHolder(tv)
    }

    override fun onBindViewHolder(holder: SimpleStringViewHolder, position: Int) {
        holder.textView.text = items.getOrNull(position) ?: ""
    }

    override fun getItemCount(): Int = items.size
}

private class SimpleStringViewHolder(val textView: android.widget.TextView) :
    RecyclerView.ViewHolder(textView)
