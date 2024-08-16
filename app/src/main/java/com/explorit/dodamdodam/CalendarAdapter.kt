package com.explorit.dodamdodam

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CalendarAdapter(private val daysOfMonth: List<String>, private val onItemClick: (Int) -> Unit) : RecyclerView.Adapter<CalendarAdapter.CalendarViewHolder>() {
    inner class CalendarViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val dayText: TextView = itemView.findViewById(R.id.textViewDay)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CalendarViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.calendar_day_item, parent, false)
        return CalendarViewHolder(view)
    }

    override fun onBindViewHolder(holder: CalendarViewHolder, position: Int) {
        holder.dayText.text = daysOfMonth[position]

        holder.itemView.setOnClickListener{
            onItemClick(position)
        }
    }

    override fun getItemCount(): Int {
        return daysOfMonth.size
    }

}