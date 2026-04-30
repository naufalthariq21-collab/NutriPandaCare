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
                _binding?.let { binding ->
                    val nama = data["nama"] as? String ?: "Pengelola"
                    binding.tvWelcome.text = "Halo, $nama!"
                }
            },
            onError = { }
        )
    }

    private fun loadSummaryStats() {
        FirebaseHelper.getPendaftarBaru(
            onSuccess = { list ->
                _binding?.let { binding ->
                    binding.tvCountVerifikasi.text = list.size.toString()
                }
            },
            onError = { }
        )

        FirebaseHelper.getAllAduan(
            onSuccess = { list ->
                _binding?.let { binding ->
                    val pendingAduan = list.filter { (it.second["status_aduan"] as? String) == "menunggu" }
                    binding.tvCountAduan.text = pendingAduan.size.toString()
                }
            },
            onError = { }
        )
    }

    private fun loadMenuHariIni() {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        FirebaseHelper.getMenuHariIni(today,
            onSuccess = { data ->
                _binding?.let { binding ->
                    if (data != null) {
                        binding.tvNamaMenuToday.text = data["nama_menu"] as? String
                        binding.tvDescMenuToday.text = "${data["kalori"]} kkal • Nutrisi Lengkap"
                        
                        val fotoUrl = data["foto_menu"] as? String ?: ""
                        if (fotoUrl.isNotEmpty()) {
                            Glide.with(this).load(fotoUrl).into(binding.ivMenuToday)
                        } else {
                            binding.ivMenuToday.setImageResource(R.color.green_pastel)
                        }
                    } else {
                        binding.tvNamaMenuToday.text = "Belum ada menu"
                        binding.tvDescMenuToday.text = "Klik untuk tambah menu hari ini"
                        binding.ivMenuToday.setImageResource(R.color.green_pastel)
                    }
                }
            },
            onError = { }
        )
    }

    private fun setupClickListeners() {
        binding.btnKelolaPengguna.setOnClickListener {
            findNavController().navigate(R.id.fragment_verifikasi_pengelola)
        }

        binding.cardVerifikasi.setOnClickListener {
            findNavController().navigate(R.id.fragment_verifikasi_pengelola)
        }

        binding.btnKelolaAduan.setOnClickListener {
            findNavController().navigate(R.id.fragment_aduan_pengelola)
        }

        binding.cardAduan.setOnClickListener {
            findNavController().navigate(R.id.fragment_aduan_pengelola)
        }

        binding.cardMenuMbg.setOnClickListener {
            findNavController().navigate(R.id.fragment_menu_mbg)
        }

        binding.btnEditMenu.setOnClickListener {
            findNavController().navigate(R.id.fragment_menu_mbg)
        }

        binding.btnNotifikasi.setOnClickListener {
            findNavController().navigate(R.id.fragment_notifikasi)
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
