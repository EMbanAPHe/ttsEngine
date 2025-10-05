package com.k2fsa.sherpa.onnx.tts.engine

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

/**
 * Displays a simple one-line row for imported voices.
 * Accepts a list of display strings instead of a Lang object to reduce coupling.
 */
class ImportedVoiceAdapter(
    context: Context,
    items: List<String>
) : ArrayAdapter<String>(context, R.layout.list_item, items) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val v = convertView ?: LayoutInflater.from(context).inflate(R.layout.list_item, parent, false)
        val tv = v.findViewById<TextView>(R.id.text_view)
        tv.text = getItem(position) ?: ""
        return v
    }
}
