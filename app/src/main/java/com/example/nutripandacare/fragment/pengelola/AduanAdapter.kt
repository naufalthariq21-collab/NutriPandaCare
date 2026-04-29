package com.example.nutripandacare.fragment.pengelola

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.nutripandacare.databinding.ItemAduanBinding

class AduanAdapter(
    private val aduanList: MutableList<Pair<String, Map<String, Any?>>>,
    private val onBalasClick: (String, String) -> Unit
) : RecyclerView.Adapter<AduanAdapter.AduanViewHolder>() {

    inner class AduanViewHolder(private val binding: ItemAduanBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(aduanId: String, data: Map<String, Any?>) {
            binding.tvJudul.text = data["judul_aduan"] as? String ?: "No Title"
            binding.tvKategori.text = data["kategori_aduan"] as? String ?: "N/A"
            binding.tvPengirim.text = "Dari: ${data["pengirim_nama"] as? String ?: "Unknown"}"
            binding.tvIsi.text = data["isi_aduan"] as? String ?: ""
            binding.tvStatus.text = data["status_aduan"] as? String ?: "Menunggu"

            binding.btnBalas.setOnClickListener {
                onBalasClick(aduanId, data["judul_aduan"] as? String ?: "")
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AduanViewHolder {
        val binding = ItemAduanBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AduanViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AduanViewHolder, position: Int) {
        val (id, data) = aduanList[position]
        holder.bind(id, data)
    }

    override fun getItemCount(): Int = aduanList.size

    fun updateData(newList: List<Pair<String, Map<String, Any?>>>) {
        aduanList.clear()
        aduanList.addAll(newList)
        notifyDataSetChanged()
    }
}
