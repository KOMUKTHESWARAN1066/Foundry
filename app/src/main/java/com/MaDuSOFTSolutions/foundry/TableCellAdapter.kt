package com.MaDuSOFTSolutions.foundry

import android.graphics.Typeface
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView


class TableCellAdapter(
    private val headers: List<String>,
    private val rows: List<List<String>>,
    private val onRowClick: (Int) -> Unit
) : RecyclerView.Adapter<TableCellAdapter.ViewHolder>() {

    private val flattenedData = buildList {
        addAll(headers)
        rows.forEach { addAll(it) }
    }

    override fun getItemCount(): Int = flattenedData.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val textView = TextView(parent.context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setPadding(16, 8, 16, 8)
        }
        return ViewHolder(textView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val value = flattenedData[position]
        holder.textView.text = value

        val totalColumns = headers.size
        val rowIndex = (position - totalColumns) / totalColumns

        if (position >= totalColumns) {
            holder.textView.setOnClickListener {
                onRowClick(rowIndex)
            }
        } else {
            holder.textView.setTypeface(null, Typeface.BOLD)
        }
    }

    class ViewHolder(val textView: TextView) : RecyclerView.ViewHolder(textView)
}


