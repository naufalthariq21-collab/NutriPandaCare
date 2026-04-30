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

        loadUserData()
        loadClassStats()
        setupClickListeners()
    }

    private fun loadUserData() {
        val uid = FirebaseHelper.uid
        if (uid.isEmpty()) return

        FirebaseHelper.getDataUser(uid,
            onSuccess = { data ->
                _binding?.let { binding ->
                    val nama = data["nama"] as? String ?: "Guru"
                    binding.tvWelcome.text = "Selamat Pagi, $nama 👋"
                }
            },
            onError = { }
        )
    }

    private fun loadClassStats() {
        // Reset to zero if no data
        binding.tvCountSiswa.text = "0"
        binding.tvCountNormal.text = "0"
        binding.tvCountResiko.text = "0"

        FirebaseHelper.getRekapGizi(
            onSuccess = { list ->
                _binding?.let { binding ->
                    if (list.isNotEmpty()) {
                        val latest = list[0].second
                        binding.tvCountSiswa.text = (latest["total_siswa"]?.toString() ?: "0")
                        binding.tvCountNormal.text = (latest["normal"]?.toString() ?: "0")
                        
                        val resiko = ((latest["gizi_buruk"] as? Number)?.toInt() ?: 0) + 
                                     ((latest["gizi_kurang"] as? Number)?.toInt() ?: 0) +
                                     ((latest["obesitas"] as? Number)?.toInt() ?: 0)
                        
                        binding.tvCountResiko.text = resiko.toString()
                    }
                }
            },
            onError = { }
        )
    }

    private fun setupClickListeners() {
        binding.btnRekapGizi.setOnClickListener {
            findNavController().navigate(R.id.nav_rekap_gizi)
        }

        binding.btnKirimPengumuman.setOnClickListener {
            findNavController().navigate(R.id.nav_buat_pengumuman)
        }
        
        binding.btnNotifikasi.setOnClickListener {
            // Jika ada fragment notifikasi guru
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
