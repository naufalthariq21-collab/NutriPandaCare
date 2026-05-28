package com.example.nutripandacare.fragment.orangtua

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.nutripandacare.LoginActivity
import com.example.nutripandacare.R
import com.example.nutripandacare.databinding.FragmentHomeOrangTuaBinding
import com.example.nutripandacare.firebase.FirebaseHelper
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.*

class HomeOrangTuaFragment : Fragment() {

    private var _binding: FragmentHomeOrangTuaBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeOrangTuaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadUserData()
        loadChildData()
        loadMenuHariIni()
        loadPengumuman()
        setupClickListeners()
    }

    private fun loadUserData() {
        val uid = FirebaseHelper.uid
        if (uid.isEmpty()) return

        FirebaseHelper.getDataUser(uid,
            onSuccess = { data ->
                _binding?.let { b ->
                    val nama = data["nama"] as? String ?: "Bunda"
                    b.tvNamaUser.text = "$nama!"

                    val fotoUrl = data["foto_url"] as? String ?: ""
                    if (fotoUrl.isNotEmpty()) {
                        Glide.with(this)
                            .load(fotoUrl)
                            .placeholder(R.drawable.ic_placeholder_avatar)
                            .into(b.ivFotoProfil)
                    }
                }
            },
            onError = { }
        )
    }

    private fun loadChildData() {
        FirebaseHelper.getDataAnak(
            onSuccess = { _, data ->
                _binding?.let { b ->
                    val namaAnak   = data["nama_anak"]     as? String ?: "Anak"
                    val usia       = data["usia_anak"]     as? String ?: "-"
                    val statusGizi = data["status_gizi"]   as? String ?: "Belum dicek"
                    val sekolah    = data["sekolah_anak"]  as? String ?: "-"
                    val jk         = data["jenis_kelamin"] as? String ?: ""

                    b.tvNamaAnak.text       = namaAnak
                    b.tvNamaAnakStatus.text = "$namaAnak • $usia"
                    b.tvInfoAnak.text       = "$usia • $jk"
                    b.tvStatusNutrisi.text  = statusGizi
                    b.tvSekolahAnak.text    = sekolah

                    val fotoAnak = data["foto_anak"] as? String ?: ""
                    if (fotoAnak.isNotEmpty()) {
                        Glide.with(this)
                            .load(fotoAnak)
                            .placeholder(R.drawable.ic_placeholder_child)
                            .into(b.ivFotoAnak)
                    }
                }
            },
            onError = {
                _binding?.let { b ->
                    b.tvNamaAnak.text      = "Belum ada data anak"
                    b.tvStatusNutrisi.text = "Belum dicek"
                    b.tvInfoAnak.text      = "Silakan tambah data anak"
                    b.tvSekolahAnak.text   = "-"
                }
            }
        )
    }

    private fun loadMenuHariIni() {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        FirebaseHelper.getMenuHariIni(today,
            onSuccess = { data ->
                _binding?.let { b ->
                    if (data != null) {
                        b.tvNamaMenu.text          = data["nama_menu"]          as? String ?: "-"
                        b.tvKaloriMenu.text         = "${data["kalori"] ?: 0} Kkal"
                        b.tvProteinMenu.text        = "${data["protein"] ?: 0}g Protein"
                        val percent = (data["persentase_nutrisi"] as? Number)?.toInt() ?: 0
                        b.tvPersentaseNutrisi.text  = "$percent%"
                        b.progressNutrisi.progress  = percent
                    } else {
                        b.tvNamaMenu.text          = "Belum ada menu hari ini"
                        b.tvKaloriMenu.text         = "0 Kkal"
                        b.tvProteinMenu.text        = "0g Protein"
                        b.tvPersentaseNutrisi.text  = "0%"
                        b.progressNutrisi.progress  = 0
                    }
                }
            },
            onError = { }
        )
    }

    private fun loadPengumuman() {
        FirebaseHelper.getPengumumanTerbaru(1,
            onSuccess = { list ->
                _binding?.let { b ->
                    if (list.isNotEmpty()) {
                        val p = list[0]
                        b.tvJudulPengumuman.text = p["judul_pengumuman"] as? String ?: ""
                        b.tvIsiPengumuman.text   = p["isi_pengumuman"]   as? String ?: ""
                        val ts = p["waktu_pengumuman"] as? Timestamp
                        ts?.let { b.tvWaktuPengumuman.text = formatWaktu(it.toDate()) }
                    } else {
                        b.tvJudulPengumuman.text = "Belum ada pengumuman"
                        b.tvIsiPengumuman.text   = "Info terbaru akan muncul di sini."
                        b.tvWaktuPengumuman.text = "-"
                    }
                }
            },
            onError = { }
        )
    }

    private fun formatWaktu(date: Date): String {
        val diff    = Date().time - date.time
        val minutes = diff / 60000
        val hours   = minutes / 60
        val days    = hours / 24
        return when {
            days > 0    -> "$days hari yang lalu"
            hours > 0   -> "$hours jam yang lalu"
            minutes > 0 -> "$minutes menit yang lalu"
            else        -> "Baru saja"
        }
    }

    private fun setupClickListeners() {
        // Semua navigate pakai action ID agar aman & type-safe
        binding.btnLihatDetailGizi.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_gizi)
        }
        binding.btnQaCekGizi.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_gizi)
        }
        binding.btnQaMenuMbg.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_menu_mbg)
        }
        binding.btnQaEdukasi.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_edukasi)
        }
        binding.btnQaNotifikasi.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_notifikasi)
        }
        binding.btnNotifikasi.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_notifikasi)
        }
        binding.tvEditProfilAnak.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_edit_profil_anak)
        }
        binding.cardProfilAnak.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_edit_profil_anak)
        }
        binding.tvLihatSemuaPengumuman.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_pengumuman)
        }
        binding.cardPengumuman.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_pengumuman)
        }
        binding.btnLogout.setOnClickListener {
            confirmLogout()
        }
    }

    private fun confirmLogout() {
        AlertDialog.Builder(requireContext())
            .setTitle("Logout")
            .setMessage("Apakah Anda yakin ingin keluar?")
            .setPositiveButton("Ya") { _, _ ->
                FirebaseHelper.logout()
                val intent = Intent(requireContext(), LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }
            .setNegativeButton("Tidak", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}