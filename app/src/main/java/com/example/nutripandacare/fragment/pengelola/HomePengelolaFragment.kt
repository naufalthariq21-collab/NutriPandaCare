package com.example.nutripandacare.fragment.pengelola

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
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
                // Jika ingin menampilkan nama pengelola di header
            },
            onError = { }
        )
    }

    private fun loadSummaryStats() {
        // Ambil jumlah pendaftar baru yang belum diverifikasi
        FirebaseHelper.getPendaftarBaru(
            onSuccess = { list ->
                if (_binding == null) return@getPendaftarBaru
                // binding.tvCountVerifikasi.text = list.size.toString()
            },
            onError = { }
        )

        // Ambil jumlah aduan masuk
        FirebaseHelper.getAllAduan(
            onSuccess = { list ->
                if (_binding == null) return@getAllAduan
                // binding.tvCountAduan.text = list.size.toString()
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
                    // binding.tvNamaMenu.text = data["nama_menu"] as? String ?: "-"
                    // binding.tvInfoMenu.text = "${data["kalori"] ?: 0} kkal • Nutrisi Lengkap"
                }
            },
            onError = { }
        )
    }

    private fun setupClickListeners() {
        binding.btnKelolaPengguna.setOnClickListener {
            findNavController().navigate(R.id.fragment_verifikasi_pengelola)
        }

        binding.btnKelolaAduan.setOnClickListener {
            findNavController().navigate(R.id.fragment_aduan_pengelola)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
