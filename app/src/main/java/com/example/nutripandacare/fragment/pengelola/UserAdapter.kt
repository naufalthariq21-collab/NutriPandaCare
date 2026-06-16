package com.example.nutripandacare.fragment.pengelola

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.nutripandacare.R
import com.example.nutripandacare.databinding.ItemPenggunaBinding

/**
 * @param showVerifikasiBtn  true  → tampilkan btn "Verifikasi" + "Tolak" (mode pending)
 *                           false → tampilkan btn "Nonaktifkan"/"Aktifkan" saja (mode semua)
 */
class UserAdapter(
    private val userList: MutableList<Pair<String, Map<String, Any?>>>,
    private val showVerifikasiBtn: Boolean = true,
    private val onVerifikasiClick: (String) -> Unit,
    private val onTolakClick: (String) -> Unit
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    inner class UserViewHolder(private val binding: ItemPenggunaBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(uid: String, data: Map<String, Any?>) {
            val nama       = data["nama"]        as? String ?: "No Name"
            val email      = data["email"]       as? String ?: "No Email"
            val role       = data["role"]        as? String ?: "-"
            val statusAkun = data["status_akun"] as? String ?: "aktif"

            binding.tvNama.text  = nama
            binding.tvEmail.text = email

            // Tampilkan role + status dengan warna sesuai kondisi
            val roleLabel = when (role) {
                "guru"       -> "Guru"
                "orang_tua"  -> "Orang Tua"
                "pengelola"  -> "Pengelola"
                else         -> role.replaceFirstChar { it.uppercase() }
            }
            val statusLabel = when (statusAkun) {
                "aktif"    -> "✅ Aktif"
                "nonaktif" -> "🔴 Nonaktif"
                "ditolak"  -> "❌ Ditolak"
                else       -> statusAkun.replaceFirstChar { it.uppercase() }
            }
            binding.tvRole.text = "$roleLabel  •  $statusLabel"

            // Warnai item jika nonaktif/ditolak supaya lebih mudah dibedakan
            val context = itemView.context
            val bgColor = when (statusAkun) {
                "nonaktif" -> 0x1FFF4444.toInt() // merah transparan
                "ditolak"  -> 0x1FFF8800.toInt() // oranye transparan
                else       -> ContextCompat.getColor(context, R.color.white)
            }
            binding.root.setCardBackgroundColor(bgColor)

            // Foto profil
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
                // ── Mode PENDING: Verifikasi + Tolak ────────────────────
                binding.btnVerifikasi.visibility = View.VISIBLE
                binding.btnVerifikasi.text       = "Verifikasi"
                binding.btnTolak.visibility      = View.VISIBLE
                binding.btnTolak.text            = "Tolak"

                binding.btnVerifikasi.setOnClickListener { onVerifikasiClick(uid) }
                binding.btnTolak.setOnClickListener      { onTolakClick(uid) }
            } else {
                // ── Mode SEMUA PENGGUNA: hanya toggle aktif/nonaktif ────
                binding.btnVerifikasi.visibility = View.GONE
                binding.btnTolak.visibility      = View.VISIBLE

                when (statusAkun) {
                    "nonaktif" -> {
                        binding.btnTolak.text = "Aktifkan"
                        binding.btnTolak.setTextColor(ContextCompat.getColor(context, R.color.green_primary))
                        binding.btnTolak.setStrokeColorResource(R.color.green_primary)
                    }
                    "ditolak" -> {
                        // Jika ditolak, pengelola bisa aktifkan kembali (verifikasi ulang manual)
                        binding.btnTolak.text = "Verifikasi"
                        binding.btnTolak.setTextColor(ContextCompat.getColor(context, R.color.green_primary))
                        binding.btnTolak.setStrokeColorResource(R.color.green_primary)
                    }
                    else -> {
                        binding.btnTolak.text = "Nonaktifkan"
                        binding.btnTolak.setTextColor(ContextCompat.getColor(context, R.color.badge_red))
                        binding.btnTolak.setStrokeColorResource(R.color.badge_red)
                    }
                }

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