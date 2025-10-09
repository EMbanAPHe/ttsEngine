package com.k2fsa.sherpa.onnx.tts.engine

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

class ImportedVoiceAdapter(
    context: Context,
    private val items: List<LangDB.LanguageEntry>
) : ArrayAdapter<LangDB.LanguageEntry>(context, R.layout.list_item, items) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.list_item, parent, false)
        val tv = view.findViewById<TextView>(R.id.text_view)
        val it = items[position]
        tv.text = "${it.lang}_${it.country}  •  ${it.name}"
        return view
    }
}