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
        FirebaseHelper.getRekapGizi(
            onSuccess = { list ->
                if (_binding == null) return@getRekapGizi
                if (list.isNotEmpty()) {
                    val lastRekap = list[0].second
                    val totalSiswa = (lastRekap["total_siswa"] as? Number)?.toInt() ?: 0
                    val normal = (lastRekap["normal"] as? Number)?.toInt() ?: 0
                    val beresiko = totalSiswa - normal

                    binding.tvTotalSiswa.text = totalSiswa.toString()
                    binding.tvTotalNormal.text = normal.toString()
                    binding.tvTotalBeresiko.text = beresiko.coerceAtLeast(0).toString()
                } else {
                    binding.tvTotalSiswa.text = "0"
                    binding.tvTotalNormal.text = "0"
                    binding.tvTotalBeresiko.text = "0"
                }
            },
            onError = { /* Handle error */ }
        )
    }

    private fun setupClickListeners() {
        binding.btnRekapGizi.setOnClickListener {
            findNavController().navigate(R.id.action_homeGuruFragment_to_rekapGiziFragment)
        }

        binding.btnKirimPengumuman.setOnClickListener {
            findNavController().navigate(R.id.action_homeGuruFragment_to_kirimPengumumanFragment)
        }
        
        binding.ivBell.setOnClickListener {
            // Navigasi ke notifikasi jika ada
            findNavController().navigate(R.id.nav_notifikasi)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
