package com.example.nutripandacare.fragment.guru

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.nutripandacare.LoginActivity
import com.example.nutripandacare.R
import com.example.nutripandacare.databinding.FragmentHomeGuruBinding
import com.example.nutripandacare.firebase.FirebaseHelper

class HomeGuruFragment : Fragment() {

    private var _binding: FragmentHomeGuruBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeGuruBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        resetUI()
        loadUserData()
        loadClassStats()
        setupClickListeners()
    }

    override fun onResume() {
        super.onResume()
        loadClassStats()
        // Badge notifikasi
        loadNotifBadge()
    }

    private fun resetUI() {
        binding.tvWelcome.text     = "Memuat..."
        binding.tvCountSiswa.text  = "-"
        binding.tvCountNormal.text = "-"
        binding.tvCountResiko.text = "-"
    }

    private fun loadUserData() {
        val uid = FirebaseHelper.uid
        if (uid.isEmpty()) return
        FirebaseHelper.getDataUser(uid,
            onSuccess = { data ->
                _binding?.let { b ->
                    val nama = data["nama"] as? String ?: "Guru"
                    b.tvWelcome.text = "Selamat Datang, $nama 👋"
                }
            },
            onError = { }
        )
    }

    private fun loadClassStats() {
        FirebaseHelper.getRekapGizi(
            onSuccess = { list ->
                _binding?.let { b ->
                    if (list.isNotEmpty()) {
                        var totalSiswa  = 0
                        var totalNormal = 0
                        var totalResiko = 0

                        list.forEach { (_, data) ->
                            totalSiswa  += (data["total_siswa"] as? Number)?.toInt() ?: 0
                            totalNormal += (data["normal"]      as? Number)?.toInt() ?: 0
                            totalResiko += ((data["gizi_buruk"]  as? Number)?.toInt() ?: 0) +
                                    ((data["gizi_kurang"] as? Number)?.toInt() ?: 0) +
                                    ((data["obesitas"]    as? Number)?.toInt() ?: 0)
                        }

                        b.tvCountSiswa.text  = totalSiswa.toString()
                        b.tvCountNormal.text = totalNormal.toString()
                        b.tvCountResiko.text = totalResiko.toString()
                    } else {
                        b.tvCountSiswa.text  = "0"
                        b.tvCountNormal.text = "0"
                        b.tvCountResiko.text = "0"
                    }
                }
            },
            onError = { }
        )
    }

    private fun loadNotifBadge() {
        FirebaseHelper.getJumlahNotifBelumDibaca { count ->
            _binding?.let { b ->
                try {
                    // Tampilkan badge jika ada notif yang belum dibaca
                    // Sesuaikan dengan ID badge di layout kamu
                    if (count > 0) {
                        b.btnNotifikasi.text = "🔔 ($count)"
                    } else {
                        b.btnNotifikasi.text = "🔔"
                    }
                } catch (_: Exception) {}
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnRekapGizi.setOnClickListener     { navigateSafe(R.id.nav_rekap_gizi) }
        binding.btnKirimPengumuman.setOnClickListener { navigateSafe(R.id.nav_buat_pengumuman) }
        binding.btnAduan.setOnClickListener         { navigateSafe(R.id.nav_aduan) }

        // Notifikasi → nav_notifikasi_guru (sudah ditambahkan di nav_guru.xml)
        binding.btnNotifikasi.setOnClickListener {
            navigateSafe(R.id.nav_notifikasi_guru)
        }

        binding.btnLogout.setOnClickListener { confirmLogout() }
    }

    private fun navigateSafe(destinationId: Int) {
        val nav = findNavController()
        if (nav.currentDestination?.id == R.id.nav_home) {
            nav.navigate(destinationId)
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
