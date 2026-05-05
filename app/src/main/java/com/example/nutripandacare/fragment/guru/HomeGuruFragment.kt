package com.example.nutripandacare.fragment.guru

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
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

        loadUserData()
        loadClassStats()
        setupClickListeners()
    }

    private fun loadUserData() {
        val uid = FirebaseHelper.uid
        if (uid.isEmpty()) return

        FirebaseHelper.getDataUser(uid,
            onSuccess = { data ->
                val nama = data["nama"] as? String ?: "Guru"
                binding.tvWelcome.text = "Selamat Pagi, $nama 👋"
            },
            onError = { /* Handle error */ }
        )
    }

    private fun loadClassStats() {
        // Logika untuk mengambil data statistik dari rekap gizi terakhir
        FirebaseHelper.getRekapGizi(
            onSuccess = { list ->
                if (list.isNotEmpty()) {
                    // Ambil rekap terbaru
                    val lastRekap = list[0].second
                    // Update UI dengan data dari Firestore (asumsi field sesuai)
                    // binding.tvTotalSiswa.text = (lastRekap["total_siswa"] as? Number)?.toString() ?: "0"
                    // dst...
                }
            },
            onError = { /* Handle error */ }
        )
    }

    private fun setupClickListeners() {
        binding.btnRekapGizi.setOnClickListener {
            findNavController().navigate(R.id.nav_rekap_gizi)
        }

        binding.btnKirimPengumuman.setOnClickListener {
            findNavController().navigate(R.id.nav_buat_pengumuman)
        }
        
        binding.ivBell.setOnClickListener {
            // Navigasi ke notifikasi jika ada
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
