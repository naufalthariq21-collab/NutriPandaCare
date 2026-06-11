package com.example.nutripandacare.fragment.pengelola

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.nutripandacare.R
import com.example.nutripandacare.databinding.ItemAduanBinding

class AduanAdapter(
    private val aduanList: MutableList<Pair<String, Map<String, Any?>>>,
    private val isPengelola: Boolean = true,
    private val onBalasClick: (String, String, String) -> Unit = { _, _, _ -> } // id, judul, balasanLama
) : RecyclerView.Adapter<AduanAdapter.AduanViewHolder>() {

    inner class AduanViewHolder(private val binding: ItemAduanBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(aduanId: String, data: Map<String, Any?>) {
            binding.tvJudul.text    = data["judul_aduan"] as? String ?: "No Title"
            binding.tvKategori.text = data["kategori_aduan"] as? String ?: "N/A"
            binding.tvPengirim.text =
                "Dari: ${data["pengirim_nama"] as? String ?: "Unknown"} (${data["pengirim_role"] ?: "-"})"
            binding.tvIsi.text      = data["isi_aduan"] as? String ?: ""

            val status = data["status_aduan"] as? String ?: "menunggu"
            binding.tvStatus.text = status.replaceFirstChar { it.uppercase() }

            // Chip warna status
            binding.tvStatus.setBackgroundResource(
                if (status == "selesai") R.drawable.bg_status_selesai
                else R.drawable.bg_status_menunggu
            )

            // Foto bukti
            val fotoUrl = data["foto_aduan"] as? String ?: ""
            if (fotoUrl.isNotEmpty()) {
                binding.ivAduan.visibility = View.VISIBLE
                Glide.with(itemView.context)
                    .load(fotoUrl)
                    .placeholder(R.drawable.ic_photo_camera)
                    .into(binding.ivAduan)
            } else {
                binding.ivAduan.visibility = View.GONE
            }

            // Balasan
            val balasan = data["balasan"] as? String ?: ""
            if (balasan.isNotEmpty()) {
                binding.layoutBalasan.visibility  = View.VISIBLE
                binding.dividerBalasan.visibility = View.VISIBLE
                binding.tvBalasan.text            = balasan
                if (isPengelola) {
                    binding.btnBalas.visibility = View.VISIBLE
                    binding.btnBalas.text = "Ubah Balasan"
                } else {
                    binding.btnBalas.visibility = View.GONE
                }
            } else {
                binding.layoutBalasan.visibility  = View.GONE
                binding.dividerBalasan.visibility = View.GONE
                if (isPengelola) {
                    binding.btnBalas.visibility = View.VISIBLE
                    binding.btnBalas.text = "Beri Balasan"
                } else {
                    binding.btnBalas.visibility = View.GONE
                }
            }

            binding.btnBalas.setOnClickListener {
                onBalasClick(
                    aduanId,
                    data["judul_aduan"] as? String ?: "",
                    balasan
                )
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
        val diffResult = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = aduanList.size
            override fun getNewListSize() = newList.size
            override fun areItemsTheSame(oldPos: Int, newPos: Int) =
                aduanList[oldPos].first == newList[newPos].first
            override fun areContentsTheSame(oldPos: Int, newPos: Int) =
                aduanList[oldPos].second == newList[newPos].second
        })
        aduanList.clear()
        aduanList.addAll(newList)
        diffResult.dispatchUpdatesTo(this)
    }
}
