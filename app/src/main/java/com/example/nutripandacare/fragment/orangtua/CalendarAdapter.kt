package com.example.nutripandacare.fragment.orangtua

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.nutripandacare.R
import com.example.nutripandacare.databinding.ItemCalendarDayBinding
import java.text.SimpleDateFormat
import java.util.*

class CalendarAdapter(
    private val days: List<Date>,
    private val selectedDate: Date,
    private val menuDates: Set<String>,
    private val currentMonth: Int,
    private val onItemClick: (Date) -> Unit
) : RecyclerView.Adapter<CalendarAdapter.CalendarViewHolder>() {

    inner class CalendarViewHolder(private val binding: ItemCalendarDayBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(date: Date) {
            val cal = Calendar.getInstance()
            cal.time = date
            val dayOfMonth = cal.get(Calendar.DAY_OF_MONTH)
            binding.tvDay.text = dayOfMonth.toString()

            // Warna abu-abu untuk tanggal di luar bulan yang sedang aktif
            if (cal.get(Calendar.MONTH) != currentMonth) {
                binding.tvDay.setTextColor(Color.LTGRAY)
            } else {
                binding.tvDay.setTextColor(ContextCompat.getColor(itemView.context, R.color.text_primary))
            }

            // Tandai jika tanggal dipilih
            val selCal = Calendar.getInstance()
            selCal.time = selectedDate
            
            val isSelected = cal.get(Calendar.YEAR) == selCal.get(Calendar.YEAR) &&
                    cal.get(Calendar.DAY_OF_YEAR) == selCal.get(Calendar.DAY_OF_YEAR)

            if (isSelected) {
                binding.viewSelected.visibility = View.VISIBLE
                binding.tvDay.setTextColor(Color.WHITE)
            } else {
                binding.viewSelected.visibility = View.GONE
            }

            // Tampilkan titik jika ada menu di tanggal tersebut
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val dateStr = sdf.format(date)
            binding.dotMenu.visibility = if (menuDates.contains(dateStr)) View.VISIBLE else View.GONE

            binding.root.setOnClickListener {
                onItemClick(date)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CalendarViewHolder {
        val binding = ItemCalendarDayBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CalendarViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CalendarViewHolder, position: Int) {
        holder.bind(days[position])
    }

    override fun getItemCount(): Int = days.size
}
