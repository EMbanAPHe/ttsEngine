package com.k2fsa.sherpa.onnx.tts.engine

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// We use a local row type instead of depending on LangDB's internal class to avoid duplicate/visibility issues.
data class LangRow(
    val name: String,
    val lang: String,
    val country: String
)

class ImportedVoiceAdapter(
    private var items: List<LangRow>,
    private val onClick: (LangRow) -> Unit,
    private val onLongClick: (LangRow) -> Unit
) : RecyclerView.Adapter<ImportedVoiceAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val title: TextView = v.findViewById(R.id.title)
        val subtitle: TextView = v.findViewById(R.id.subtitle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_imported_voice, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.title.text = "${item.lang}_${item.country}"
        holder.subtitle.text = item.name

        holder.itemView.setOnClickListener { onClick(item) }
        holder.itemView.setOnLongClickListener { onLongClick(item); true }
    }

    override fun getItemCount(): Int = items.size

    fun submitList(newItems: List<LangRow>) {
        items = newItems
        notifyDataSetChanged()
    }
}