package com.seanh.lightsensor

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.seanh.lightsensor.models.Data

class DataAdapter(private val dataList: ArrayList<Data>) : RecyclerView.Adapter<DataAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.list_layout, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindItems(dataList[position])
    }

    override fun getItemCount(): Int {
        return dataList.size
    }

    class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        fun bindItems(data: Data) {
            val tvKey = itemView.findViewById(R.id.tvKey) as TextView
            val tvValue  = itemView.findViewById(R.id.tvValue) as TextView
            tvKey.text = data.key
            tvValue.text = data.value
        }
    }
}