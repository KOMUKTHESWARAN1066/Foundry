package com.MaDuSOFTSolutions.foundry

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class DetailItemAdapter(private val fields: List<DetailField>) :
    RecyclerView.Adapter<DetailItemAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val label = view.findViewById<TextView>(R.id.tvLabel)
        val value = view.findViewById<TextView>(R.id.tvValue)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_detail_row, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = fields.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val field = fields[position]
        holder.label.text = field.label
        holder.value.text = field.value
    }
}
