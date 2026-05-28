package com.example.nutripandacare.fragment.pengelola

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.nutripandacare.R
import com.example.nutripandacare.databinding.ItemMenuMbgBinding

class MenuAdapter(
    private val menuList: MutableList<Map<String, Any?>>,
    private val onDeleteClick: (String) -> Unit,
    private val onEditClick: (String) -> Unit = {}
) : RecyclerView.Adapter<MenuAdapter.MenuViewHolder>() {

    inner class MenuViewHolder(private val binding: ItemMenuMbgBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(data: Map<String, Any?>) {
            val tanggal = data["tanggal"] as? String ?: ""
            binding.tvTanggal.text    = data["tanggal_label"] as? String ?: tanggal
            binding.tvNamaMenu.text   = data["nama_menu"] as? String ?: "No Name"

            val kalori  = data["kalori"] ?: 0
            val protein = data["protein"] ?: 0
            binding.tvInfoNutrisi.text = "$kalori Kkal • ${protein}g Protein"

            val fotoUrl = data["foto_menu"] as? String ?: ""
            if (fotoUrl.isNotEmpty()) {
                Glide.with(itemView.context)
                    .load(fotoUrl)
                    .placeholder(R.drawable.ic_food_plate)
                    .centerCrop()
                    .into(binding.ivFotoMenu)
            } else {
                binding.ivFotoMenu.setImageResource(R.drawable.ic_food_plate)
            }

            binding.btnEdit.setOnClickListener   { onEditClick(tanggal) }
            binding.btnDelete.setOnClickListener { onDeleteClick(tanggal) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuViewHolder {
        val binding = ItemMenuMbgBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MenuViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MenuViewHolder, position: Int) {
        holder.bind(menuList[position])
    }

    override fun getItemCount(): Int = menuList.size

    fun updateData(newList: List<Map<String, Any?>>) {
        val diffResult = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = menuList.size
            override fun getNewListSize() = newList.size
            override fun areItemsTheSame(oldPos: Int, newPos: Int) =
                menuList[oldPos]["tanggal"] == newList[newPos]["tanggal"]
            override fun areContentsTheSame(oldPos: Int, newPos: Int) =
                menuList[oldPos] == newList[newPos]
        })
        menuList.clear()
        menuList.addAll(newList)
        diffResult.dispatchUpdatesTo(this)
    }
}