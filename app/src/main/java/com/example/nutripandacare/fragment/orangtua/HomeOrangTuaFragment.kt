package com.example.nutripandacare.fragment.orangtua

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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
                if (_binding == null) return@getDataUser
                val nama = data["nama"] as? String ?: "Bunda"
                binding.tvNamaUser.text = "$nama!"
                
                val fotoUrl = data["foto_url"] as? String ?: ""
                if (fotoUrl.isNotEmpty()) {
                    Glide.with(this).load(fotoUrl).into(binding.ivFotoProfil)
                }
            },
            onError = { /* Handle error */ }
        )
    }

    private fun loadChildData() {
        FirebaseHelper.getDataAnak(
            onSuccess = { _, data ->
                if (_binding == null) return@getDataAnak
                val namaAnak = data["nama_anak"] as? String ?: "Anak"
                val usia = data["usia_anak"] as? String ?: "-"
                val statusGizi = data["status_gizi"] as? String ?: "Normal"

                binding.tvNamaAnak.text = namaAnak
                binding.tvNamaAnakStatus.text = "$namaAnak • $usia"
                binding.tvInfoAnak.text = "$usia • ${data["jenis_kelamin"] ?: ""}"
                binding.tvStatusNutrisi.text = statusGizi
                
                val fotoAnak = data["foto_anak"] as? String ?: ""
                if (fotoAnak.isNotEmpty()) {
                    Glide.with(this).load(fotoAnak).into(binding.ivFotoAnak)
                }
            },
            onError = { /* Handle error */ }
        )
    }

    private fun loadMenuHariIni() {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        FirebaseHelper.getMenuHariIni(today,
            onSuccess = { data ->
                if (_binding == null) return@getMenuHariIni
                if (data != null) {
                    binding.tvNamaMenu.text = data["nama_menu"] as? String ?: "-"
                    binding.tvKaloriMenu.text = "${data["kalori"] ?: 0} Kkal"
                    binding.tvProteinMenu.text = "${data["protein"] ?: 0}g Protein"
                    
                    val percent = (data["persentase_nutrisi"] as? Number)?.toInt() ?: 0
                    binding.tvPersentaseNutrisi.text = "$percent%"
                    binding.progressNutrisi.progress = percent
                }
            },
            onError = { /* Handle error */ }
        )
    }

    private fun loadPengumuman() {
        FirebaseHelper.getPengumumanTerbaru(1,
            onSuccess = { list ->
                if (_binding == null) return@getPengumumanTerbaru
                if (list.isNotEmpty()) {
                    val p = list[0]
                    binding.tvJudulPengumuman.text = p["judul_pengumuman"] as? String ?: ""
                    binding.tvIsiPengumuman.text = p["isi_pengumuman"] as? String ?: ""
                    
                    val ts = p["waktu_pengumuman"] as? Timestamp
                    ts?.let {
                        binding.tvWaktuPengumuman.text = formatWaktu(it.toDate())
                    }
                }
            },
            onError = { /* Handle error */ }
        )
    }

    private fun formatWaktu(date: Date): String {
        val diff = Date().time - date.time
        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        return when {
            days > 0 -> "$days hari yang lalu"
            hours > 0 -> "$hours jam yang lalu"
            minutes > 0 -> "$minutes menit yang lalu"
            else -> "Baru saja"
        }
    }

    private fun setupClickListeners() {
        binding.btnLihatDetailGizi.setOnClickListener {
            findNavController().navigate(R.id.giziFragment)
        }
        
        binding.btnQaCekGizi.setOnClickListener {
            findNavController().navigate(R.id.giziFragment)
        }

        binding.btnQaMenuMbg.setOnClickListener {
            findNavController().navigate(R.id.menuMbgFragment)
        }

        binding.btnQaEdukasi.setOnClickListener {
            findNavController().navigate(R.id.edukasiFragment)
        }

        binding.tvEditProfilAnak.setOnClickListener {
            findNavController().navigate(R.id.editProfilAnakFragment)
        }

        binding.cardProfilAnak.setOnClickListener {
            findNavController().navigate(R.id.editProfilAnakFragment)
        }

        binding.btnQaNotifikasi.setOnClickListener {
            findNavController().navigate(R.id.notifikasiFragment)
        }

        binding.btnNotifikasi.setOnClickListener {
            findNavController().navigate(R.id.notifikasiFragment)
        }

        binding.tvLihatSemuaPengumuman.setOnClickListener {
             Toast.makeText(requireContext(), "Fitur Pengumuman segera hadir!", Toast.LENGTH_SHORT).show()
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
