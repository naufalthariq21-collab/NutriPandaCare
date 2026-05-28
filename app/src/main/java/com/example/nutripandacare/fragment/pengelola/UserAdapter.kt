package com.example.nutripandacare.fragment.pengelola

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.nutripandacare.R
import com.example.nutripandacare.databinding.ItemPenggunaBinding

/**
 * @param showVerifikasiBtn  true  → tampilkan btn "Verifikasi" (mode pending)
 *                           false → sembunyikan btn verifikasi, tampilkan btn "Nonaktifkan" (mode semua)
 */
class UserAdapter(
    private val userList: MutableList<Pair<String, Map<String, Any?>>>,
    private val showVerifikasiBtn: Boolean = true,
    private val onVerifikasiClick: (String) -> Unit,
    private val onTolakClick: (String) -> Unit         // "Tolak" di pending, "Nonaktifkan" di semua
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    inner class UserViewHolder(private val binding: ItemPenggunaBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(uid: String, data: Map<String, Any?>) {
            binding.tvNama.text  = data["nama"] as? String ?: "No Name"
            binding.tvEmail.text = data["email"] as? String ?: "No Email"

            val role       = data["role"] as? String ?: "-"
            val statusAkun = data["status_akun"] as? String ?: "aktif"
            binding.tvRole.text = "Role: $role  •  Status: $statusAkun"

            val fotoUrl = data["foto_url"] as? String ?: ""
            if (fotoUrl.isNotEmpty()) {
                Glide.with(itemView.context)
                    .load(fotoUrl)
                    .placeholder(R.drawable.ic_placeholder_avatar)
                    .circleCrop()
                    .into(binding.ivUser)
            } else {
                binding.ivUser.setImageResource(R.drawable.ic_placeholder_avatar)
            }

            if (showVerifikasiBtn) {
                // ── Mode pending: Verifikasi + Tolak ──────────────────────
                binding.btnVerifikasi.visibility = View.VISIBLE
                binding.btnVerifikasi.text       = "Verifikasi"
                binding.btnTolak.visibility      = View.VISIBLE
                binding.btnTolak.text            = "Tolak"

                binding.btnVerifikasi.setOnClickListener { onVerifikasiClick(uid) }
                binding.btnTolak.setOnClickListener      { onTolakClick(uid) }
            } else {
                // ── Mode semua pengguna: hanya Nonaktifkan ─────────────────
                binding.btnVerifikasi.visibility = View.GONE
                binding.btnTolak.visibility      = View.VISIBLE
                binding.btnTolak.text            = if (statusAkun == "nonaktif") "Aktifkan" else "Nonaktifkan"
                binding.btnTolak.setOnClickListener { onTolakClick(uid) }
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
        val diffResult = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = userList.size
            override fun getNewListSize() = newList.size
            override fun areItemsTheSame(oldPos: Int, newPos: Int) =
                userList[oldPos].first == newList[newPos].first
            override fun areContentsTheSame(oldPos: Int, newPos: Int) =
                userList[oldPos].second == newList[newPos].second
        })
        userList.clear()
        userList.addAll(newList)
        diffResult.dispatchUpdatesTo(this)
    }
}