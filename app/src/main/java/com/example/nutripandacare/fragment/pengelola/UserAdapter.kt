package com.example.nutripandacare.fragment.pengelola

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.nutripandacare.R
import com.example.nutripandacare.databinding.ItemPenggunaBinding

class UserAdapter(
    private val userList: MutableList<Pair<String, Map<String, Any?>>>,
    private val onVerifikasiClick: (String) -> Unit
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    inner class UserViewHolder(private val binding: ItemPenggunaBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(uid: String, data: Map<String, Any?>) {
            binding.tvNama.text = data["nama"] as? String ?: "No Name"
            binding.tvEmail.text = data["email"] as? String ?: "No Email"
            binding.tvRole.text = "Role: ${data["role"] as? String ?: "N/A"}"

            val fotoUrl = data["foto_url"] as? String ?: ""
            if (fotoUrl.isNotEmpty()) {
                Glide.with(itemView.context).load(fotoUrl).placeholder(R.drawable.ic_placeholder_avatar).into(binding.ivUser)
            } else {
                binding.ivUser.setImageResource(R.drawable.ic_placeholder_avatar)
            }

            binding.btnVerifikasi.setOnClickListener {
                onVerifikasiClick(uid)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = ItemPenggunaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val (uid, data) = userList[position]
        holder.bind(uid, data)
    }

    override fun getItemCount(): Int = userList.size

    fun updateData(newList: List<Pair<String, Map<String, Any?>>>) {
        userList.clear()
        userList.addAll(newList)
        notifyDataSetChanged()
    }
}
