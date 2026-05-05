package com.example.nutripandacare.fragment.pengelola

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
import com.example.nutripandacare.databinding.FragmentHomePengelolaBinding
import com.example.nutripandacare.firebase.FirebaseHelper
import java.text.SimpleDateFormat
import java.util.*

class HomePengelolaFragment : Fragment() {

    private var _binding: FragmentHomePengelolaBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomePengelolaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadUserData()
        loadSummaryStats()
        loadMenuHariIni()
        setupClickListeners()
    }

    private fun loadUserData() {
        val uid = FirebaseHelper.uid
        if (uid.isEmpty()) return

        FirebaseHelper.getDataUser(uid,
            onSuccess = { data ->
                if (_binding == null) return@getDataUser
                val nama = data["nama"] as? String ?: "Pengelola"
                binding.tvWelcomeName.text = "Selamat Datang, $nama"
            },
            onError = { }
        )
    }

    private fun loadSummaryStats() {
        FirebaseHelper.getPendaftarBaru(
            onSuccess = { list ->
                if (_binding == null) return@getPendaftarBaru
                binding.tvCountVerifikasi.text = list.size.toString()
            },
            onError = { }
        )

        FirebaseHelper.getAllAduan(
            onSuccess = { list ->
                if (_binding == null) return@getAllAduan
                val pendingCount = list.count { (it.second["status_aduan"] as? String) == "menunggu" }
                binding.tvCountAduan.text = pendingCount.toString()
            },
            onError = { }
        )
    }

    private fun loadMenuHariIni() {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        FirebaseHelper.getMenuHariIni(today,
            onSuccess = { data ->
                if (_binding == null) return@getMenuHariIni
                if (data != null) {
                    binding.tvNamaMenuHariIni.text = data["nama_menu"] as? String ?: "Menu Hari Ini"
                    val kalori = (data["kalori"] as? Number)?.toLong() ?: 0L
                    binding.tvInfoMenuHariIni.text = "$kalori kkal • Nutrisi Lengkap"
                    
                    val fotoUrl = data["foto_menu"] as? String ?: ""
                    if (fotoUrl.isNotEmpty()) {
                        Glide.with(requireContext())
                            .load(fotoUrl)
                            .placeholder(R.drawable.ic_food_plate)
                            .into(binding.ivMenuHariIni)
                    } else {
                        binding.ivMenuHariIni.setImageResource(R.drawable.ic_food_plate)
                    }
                } else {
                    binding.tvNamaMenuHariIni.text = "Belum Ada Menu"
                    binding.tvInfoMenuHariIni.text = "Silakan tambahkan menu untuk hari ini"
                    binding.ivMenuHariIni.setImageResource(R.drawable.ic_food_plate)
                }
            },
            onError = { }
        )
    }

    private fun setupClickListeners() {
        // Navigasi dari Header/Summary
        binding.btnNotifikasi.setOnClickListener {
            findNavController().navigate(R.id.fragment_notifikasi)
        }

        binding.btnLogout.setOnClickListener {
            confirmLogout()
        }

        // Navigasi Menu MBG (dari Card Menu Hari Ini)
        binding.btnEditMenuHariIni.setOnClickListener {
            findNavController().navigate(R.id.fragment_menu_mbg)
        }

        // Navigasi Aksi Cepat
        binding.btnKelolaPengguna.setOnClickListener {
            findNavController().navigate(R.id.fragment_verifikasi_pengelola)
        }

        binding.btnKelolaAduan.setOnClickListener {
            findNavController().navigate(R.id.fragment_aduan_pengelola)
        }

        binding.btnRekapGizi.setOnClickListener {
            findNavController().navigate(R.id.fragment_rekap_gizi)
        }

        binding.btnEdukasi.setOnClickListener {
            findNavController().navigate(R.id.fragment_edukasi)
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
