package com.example.nutripandacare.fragment.orangtua

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.nutripandacare.R
import com.example.nutripandacare.databinding.FragmentGiziBinding
import com.example.nutripandacare.firebase.FirebaseHelper
import java.text.SimpleDateFormat
import java.util.*

class GiziFragment : Fragment() {

    private var _binding: FragmentGiziBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGiziBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadGiziData()
        setupClickListeners()
    }

    @SuppressLint("SetTextI18n")
    private fun loadGiziData() {
        FirebaseHelper.getDataAnak(
            onSuccess = { anakId, data ->
                if (_binding == null) return@getDataAnak

                val namaAnak   = data["nama_anak"]   as? String ?: "Anak"
                val statusGizi = data["status_gizi"] as? String ?: "Belum dicek"
                val zScore     = (data["z_score"]    as? Number)?.toDouble() ?: 0.0
                val berat      = (data["berat_badan"] as? Number)?.toDouble() ?: 0.0
                val tinggi     = (data["tinggi_badan"] as? Number)?.toDouble() ?: 0.0
                val persen     = (data["persentase_nutrisi"] as? Number)?.toInt() ?: 0

                binding.tvStatusGiziNama.text   = statusGizi
                binding.tvStatusGiziDetail.text = "$namaAnak | Z-Score: ${"%.2f".format(zScore)}"

                // Tampilkan data pengukuran terakhir dari field anak langsung
                binding.tvAvgKalori.text    = "${berat} kg"
                binding.tvAvgProtein.text   = "${tinggi} cm"
                binding.tvNutrisiPersen.text = "$persen%"

                val sdf = SimpleDateFormat("MMMM yyyy", Locale("id", "ID"))
                binding.tvPeriodeGizi.text = sdf.format(Date())

                // Load riwayat untuk update tampilan dengan data terbaru
                loadGiziHistory(anakId)
            },
            onError = { err ->
                if (_binding == null) return@getDataAnak
                Toast.makeText(requireContext(), err, Toast.LENGTH_SHORT).show()
                binding.tvStatusGiziNama.text = "Data belum ada"
            }
        )
    }

    @SuppressLint("SetTextI18n")
    private fun loadGiziHistory(anakId: String) {
        FirebaseHelper.getRiwayatGizi(anakId, limit = 1,
            onSuccess = { history ->
                if (_binding == null) return@getRiwayatGizi

                if (history.isNotEmpty()) {
                    val latest = history[0]
                    // Field yang benar sesuai skema FirebaseHelper.simpanHasilGizi:
                    // berat_badan, tinggi_badan, usia_bulan, z_score, status_gizi, persentase_nutrisi
                    val berat  = (latest["berat_badan"]        as? Number)?.toDouble() ?: 0.0
                    val tinggi = (latest["tinggi_badan"]       as? Number)?.toDouble() ?: 0.0
                    val persen = (latest["persentase_nutrisi"] as? Number)?.toInt()    ?: 0

                    binding.tvAvgKalori.text    = "$berat kg"
                    binding.tvAvgProtein.text   = "$tinggi cm"
                    binding.tvNutrisiPersen.text = "$persen%"
                }

                val sdf = SimpleDateFormat("MMMM yyyy", Locale("id", "ID"))
                binding.tvPeriodeGizi.text = sdf.format(Date())
            },
            onError = { /* Tampilan sudah diisi dari data anak di atas */ }
        )
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        // Pakai action ID yang sudah terdaftar di nav graph
        binding.btnUpdateGizi.setOnClickListener {
            findNavController().navigate(R.id.action_gizi_to_input_gizi)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}