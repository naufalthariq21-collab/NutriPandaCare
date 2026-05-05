package com.example.nutripandacare.fragment.pengelola

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.nutripandacare.R
import com.example.nutripandacare.databinding.ItemNotifikasiBinding
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.*

class NotifikasiAdapter(
    private val notifList: MutableList<Pair<String, Map<String, Any?>>>,
    private val onItemClick: (String) -> Unit
) : RecyclerView.Adapter<NotifikasiAdapter.NotifViewHolder>() {

    inner class NotifViewHolder(private val binding: ItemNotifikasiBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(id: String, data: Map<String, Any?>) {
            binding.tvJudul.text = data["judul"] as? String ?: "Notifikasi"
            binding.tvIsi.text = data["isi"] as? String ?: ""
            
            val timestamp = data["waktu"] as? Timestamp
            if (timestamp != null) {
                val sdf = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
                binding.tvWaktu.text = sdf.format(timestamp.toDate())
            }

            val isRead = data["is_read"] as? Boolean ?: false
            if (isRead) {
                binding.root.alpha = 0.6f
                binding.ivDot.visibility = android.view.View.GONE
            } else {
                binding.root.alpha = 1.0f
                binding.ivDot.visibility = android.view.View.VISIBLE
            }

            binding.root.setOnClickListener {
                onItemClick(id)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotifViewHolder {
        val binding = ItemNotifikasiBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NotifViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NotifViewHolder, position: Int) {
        val (id, data) = notifList[position]
        holder.bind(id, data)
    }

    override fun getItemCount(): Int = notifList.size

    fun updateData(newList: List<Pair<String, Map<String, Any?>>>) {
        notifList.clear()
        notifList.addAll(newList)
        notifyDataSetChanged()
    }
}
