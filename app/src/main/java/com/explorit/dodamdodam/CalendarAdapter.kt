package com.explorit.dodamdodam

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.time.LocalDate

class CalendarAdapter(private val daysOfMonth: List<String>, private val selectedDate: LocalDate, private val onItemClick: (position: Int) -> Unit, private val missionCompletedDates: List<String>) : RecyclerView.Adapter<CalendarAdapter.CalendarViewHolder>() {

    private val today = LocalDate.now()

    inner class CalendarViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val dayText: TextView = itemView.findViewById(R.id.textViewDay)
        val checkIcon: ImageView = itemView.findViewById(R.id.imageViewCheck)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CalendarViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.calendar_day_item, parent, false)
        return CalendarViewHolder(view)
    }

    override fun onBindViewHolder(holder: CalendarViewHolder, position: Int) {
        val day = daysOfMonth[position]

        holder.dayText.text = day

        if (day.isNotEmpty() && day.all { it.isDigit() }) {
            val cellDate = LocalDate.of(selectedDate.year, selectedDate.month, day.toInt())
            val completedDate = cellDate.toString()

            if (missionCompletedDates.contains(completedDate)) {
                holder.checkIcon.visibility = View.VISIBLE // 체크 아이콘 표시
            } else {
                holder.checkIcon.visibility = View.GONE // 체크 아이콘 숨기기
            }


            when {
                cellDate.isEqual(today) -> {
                    holder.dayText.setBackgroundResource(R.drawable.current_day_background)
                    holder.dayText.setTextColor(Color.RED)
                }

                else -> {
                    holder.dayText.setBackgroundResource(0)
                    holder.dayText.setTextColor(Color.BLACK)
                }


            }

            holder.itemView.setOnClickListener {
                onItemClick(position)
            }

        } else {
            holder.dayText.setBackgroundResource(0)
            holder.dayText.setTextColor(Color.GRAY)
            holder.checkIcon.visibility = View.GONE

            holder.itemView.setOnClickListener(null)
        }

    }


    override fun getItemCount(): Int {
        return daysOfMonth.size
    }

}