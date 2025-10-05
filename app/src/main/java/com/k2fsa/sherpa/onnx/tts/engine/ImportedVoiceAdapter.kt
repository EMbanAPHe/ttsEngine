package com.k2fsa.sherpa.onnx.tts.engine

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ImportedVoiceAdapter(
    private var items: List<LangDB.Lang>,
    private val onClick: (LangDB.Lang) -> Unit,
    private val onLongClick: (LangDB.Lang) -> Unit
) : RecyclerView.Adapter<ImportedVoiceAdapter.Holder>() {

    class Holder(v: View) : RecyclerView.ViewHolder(v) {
        val title: TextView = v.findViewById(R.id.txtVoice)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_imported_voice, parent, false)
        return Holder(v)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = items[position]
        holder.title.text = "${item.lang}_${item.country} • ${item.name}"
        holder.itemView.setOnClickListener { onClick(item) }
        holder.itemView.setOnLongClickListener { onLongClick(item); true }
    }

    override fun getItemCount(): Int = items.size

    fun submit(newItems: List<LangDB.Lang>) {
        items = newItems
        notifyDataSetChanged()
    }
}