package com.example.nutripandacare.fragment.orangtua

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.nutripandacare.databinding.ItemKontenBinding

class ArtikelAdapter(
    private val list: MutableList<Pair<String, Map<String, Any?>>>,
    private val onClick: (String, Map<String, Any?>) -> Unit
) : RecyclerView.Adapter<ArtikelAdapter.VH>() {

    inner class VH(val binding: ItemKontenBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemKontenBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun getItemCount() = list.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val (id, data) = list[position]
        with(holder.binding) {
            tvJudulKonten.text = data["judul"] as? String ?: ""
            tvKategoriKonten.text = data["kategori"] as? String ?: ""
            
            val menit = (data["menit_baca"] as? Number)?.toInt() ?: 0
            tvWaktuMenitKonten.text = "$menit menit baca"

            val thumb = data["thumbnail_url"] as? String ?: ""
            if (thumb.isNotEmpty()) {
                Glide.with(holder.itemView.context).load(thumb).into(ivThumbnailKonten)
            }
            
            tvMenuKonten.visibility = android.view.View.GONE

            root.setOnClickListener { onClick(id, data) }
        }
    }

    fun updateData(newList: List<Pair<String, Map<String, Any?>>>) {
        list.clear()
        list.addAll(newList)
        notifyDataSetChanged()
    }
}
