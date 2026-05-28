package com.example.nutripandacare.fragment.pengelola

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.nutripandacare.databinding.ItemNotifikasiBinding
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.*

class NotifikasiAdapter(
    private val notifList: MutableList<Pair<String, Map<String, Any?>>>,
    private val onItemClick: (String) -> Unit
) : RecyclerView.Adapter<NotifikasiAdapter.NotifViewHolder>() {

    inner class NotifViewHolder(private val binding: ItemNotifikasiBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(id: String, data: Map<String, Any?>) {
            binding.tvJudul.text = data["judul"] as? String ?: "Notifikasi"
            binding.tvIsi.text   = data["isi"] as? String ?: ""

            val timestamp = data["waktu"] as? Timestamp
            binding.tvWaktu.text = if (timestamp != null) {
                SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(timestamp.toDate())
            } else "-"

            val isRead = data["is_read"] as? Boolean ?: false
            binding.root.alpha           = if (isRead) 0.6f else 1.0f
            binding.ivDot.visibility     = if (isRead) View.GONE else View.VISIBLE

            binding.root.setOnClickListener { onItemClick(id) }
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
        val diffResult = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = notifList.size
            override fun getNewListSize() = newList.size
            override fun areItemsTheSame(oldPos: Int, newPos: Int) =
                notifList[oldPos].first == newList[newPos].first
            override fun areContentsTheSame(oldPos: Int, newPos: Int) =
                notifList[oldPos].second == newList[newPos].second
        })
        notifList.clear()
        notifList.addAll(newList)
        diffResult.dispatchUpdatesTo(this)
    }
}