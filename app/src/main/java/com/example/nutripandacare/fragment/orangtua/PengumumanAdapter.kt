package com.example.nutripandacare.fragment.orangtua

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.nutripandacare.databinding.ItemPengumumanBinding
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.*

class PengumumanAdapter(
    private var list: List<Map<String, Any?>>
) : RecyclerView.Adapter<PengumumanAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemPengumumanBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPengumumanBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val data = list[position]
        holder.binding.tvJudulPengumuman.text = data["judul_pengumuman"] as? String ?: ""
        holder.binding.tvIsiPengumuman.text = data["isi_pengumuman"] as? String ?: ""
        
        val ts = data["waktu_pengumuman"] as? Timestamp
        ts?.let {
            holder.binding.tvWaktuPengumuman.text = formatWaktu(it.toDate())
        }
    }

    override fun getItemCount(): Int = list.size

    fun updateData(newList: List<Map<String, Any?>>) {
        list = newList
        notifyDataSetChanged()
    }

    private fun formatWaktu(date: Date): String {
        val diff = Date().time - date.time
        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        return when {
            days > 0 -> "$days hari yang lalu"
            hours > 0 -> "$hours jam yang lalu"
            minutes > 0 -> "$minutes menit yang lalu"
            else -> "Baru saja"
        }
    }
}
