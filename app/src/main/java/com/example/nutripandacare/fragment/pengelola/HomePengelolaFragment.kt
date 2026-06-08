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

    override fun onResume() {
        super.onResume()
        loadSummaryStats()
        loadMenuHariIni()
    }

    private fun loadUserData() {
        val uid = FirebaseHelper.uid
        if (uid.isEmpty()) return
        FirebaseHelper.getDataUser(uid,
            onSuccess = { data ->
                _binding?.tvWelcome?.text = "Halo, ${data["nama"] as? String ?: "Pengelola"}! 👋"
            },
            onError = {}
        )
    }

    private fun loadSummaryStats() {
        FirebaseHelper.getPendaftarBaru(
            onSuccess = { list -> _binding?.tvCountVerifikasi?.text = list.size.toString() },
            onError   = {}
        )
        FirebaseHelper.getAllAduan(
            onSuccess = { list ->
                val pending = list.count { (it.second["status_aduan"] as? String) == "menunggu" }
                _binding?.tvCountAduan?.text = pending.toString()
            },
            onError = {}
        )
    }

    private fun loadMenuHariIni() {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        FirebaseHelper.getMenuHariIni(today,
            onSuccess = { data ->
                val b = _binding ?: return@getMenuHariIni
                if (data != null) {
                    b.tvNamaMenuToday.text = data["nama_menu"] as? String ?: "Menu Tanpa Nama"
                    b.tvDescMenuToday.text = "${data["kalori"] ?: 0} kkal • Nutrisi Terjaga"
                    val fotoUrl = data["foto_menu"] as? String ?: ""
                    if (fotoUrl.isNotEmpty()) {
                        Glide.with(this).load(fotoUrl)
                            .placeholder(R.drawable.ic_food_plate)
                            .into(b.ivMenuToday)
                    }
                } else {
                    b.tvNamaMenuToday.text = "Belum ada menu hari ini"
                    b.tvDescMenuToday.text = "Klik edit untuk menambahkan"
                    b.ivMenuToday.setImageResource(R.drawable.ic_food_plate)
                }
            },
            onError = {}
        )
    }

    private fun setupClickListeners() {
        binding.btnKelolaPengguna.setOnClickListener {
            navigateSafe(R.id.fragment_verifikasi_pengelola)
        }
        binding.cardVerifikasi.setOnClickListener {
            navigateSafe(R.id.fragment_verifikasi_pengelola)
        }
        binding.btnKelolaAduan.setOnClickListener {
            navigateSafe(R.id.fragment_aduan_pengelola)
        }
        binding.cardAduan.setOnClickListener {
            navigateSafe(R.id.fragment_aduan_pengelola)
        }
        binding.cardMenuMbg.setOnClickListener {
            navigateSafe(R.id.fragment_menu_mbg)
        }
        binding.btnEditMenu.setOnClickListener {
            navigateSafe(R.id.fragment_menu_mbg)
        }
        binding.btnNotifikasi.setOnClickListener {
            navigateSafe(R.id.fragment_notifikasi)
        }
        binding.btnLogout.setOnClickListener {
            confirmLogout()
        }
    }

    private fun navigateSafe(destinationId: Int) {
        val nav = findNavController()
        if (nav.currentDestination?.id == R.id.fragment_home_pengelola) {
            nav.navigate(destinationId)
        }
    }

    private fun confirmLogout() {
        AlertDialog.Builder(requireContext())
            .setTitle("Logout")
            .setMessage("Apakah Anda yakin ingin keluar?")
            .setPositiveButton("Ya") { _, _ ->
                FirebaseHelper.logout()
                startActivity(
                    Intent(requireContext(), LoginActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                )
            }
            .setNegativeButton("Tidak", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
