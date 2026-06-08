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

    override fun onResume() {
        super.onResume()
        // Refresh data setiap kali kembali dari InputGiziFragment
        loadGiziData()
    }

    @SuppressLint("SetTextI18n")
    private fun loadGiziData() {
        FirebaseHelper.getDataAnak(
            onSuccess = { anakId, data ->
                if (_binding == null) return@getDataAnak

                val namaAnak   = data["nama_anak"]    as? String ?: "Anak"
                val statusGizi = data["status_gizi"]  as? String ?: "Belum dicek"
                val zScore     = (data["z_score"]     as? Number)?.toDouble() ?: 0.0
                val berat      = (data["berat_badan"] as? Number)?.toDouble() ?: 0.0
                val tinggi     = (data["tinggi_badan"] as? Number)?.toDouble() ?: 0.0
                val persen     = (data["persentase_nutrisi"] as? Number)?.toInt() ?: 0

                binding.tvStatusGiziNama.text   = statusGizi
                binding.tvStatusGiziDetail.text = "$namaAnak  |  Z-Score: ${"%.2f".format(zScore)}"
                binding.tvAvgKalori.text        = "$berat kg"
                binding.tvAvgProtein.text       = "$tinggi cm"
                binding.tvNutrisiPersen.text    = "$persen%"

                val sdf = SimpleDateFormat("MMMM yyyy", Locale("id", "ID"))
                binding.tvPeriodeGizi.text = sdf.format(Date())

                // Tampilkan rekomendasi berdasarkan status gizi terakhir
                val usiaBulan = when (val v = data["usia_bulan"]) {
                    is Long -> v.toInt(); is Int -> v; is Number -> v.toInt(); else -> 0
                }
                tampilkanRekomendasi(statusGizi, usiaBulan)

                // Load riwayat terbaru untuk update tampilan jika ada data baru
                loadGiziHistory(anakId)
            },
            onError = { err ->
                if (_binding == null) return@getDataAnak
                Toast.makeText(requireContext(), err, Toast.LENGTH_SHORT).show()
                binding.tvStatusGiziNama.text   = "Data belum ada"
                binding.tvStatusGiziDetail.text = "Silakan cek gizi anak"
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
                    val berat  = (latest["berat_badan"]        as? Number)?.toDouble() ?: 0.0
                    val tinggi = (latest["tinggi_badan"]       as? Number)?.toDouble() ?: 0.0
                    val persen = (latest["persentase_nutrisi"] as? Number)?.toInt()    ?: 0
                    val status = latest["status_gizi"] as? String ?: ""
                    val z      = (latest["z_score"] as? Number)?.toDouble() ?: 0.0

                    binding.tvAvgKalori.text     = "$berat kg"
                    binding.tvAvgProtein.text    = "$tinggi cm"
                    binding.tvNutrisiPersen.text = "$persen%"

                    if (status.isNotEmpty()) {
                        binding.tvStatusGiziNama.text = status
                        binding.tvStatusGiziDetail.text = "Z-Score: ${"%.2f".format(z)}"

                        val usiaBulan = (latest["usia_bulan"] as? Number)?.toInt() ?: 0
                        tampilkanRekomendasi(status, usiaBulan)
                    }
                }
            },
            onError = { /* Tetap pakai data dari dokumen anak */ }
        )
    }

    private fun tampilkanRekomendasi(statusGizi: String, usiaBulan: Int) {
        val rekomendasi = FirebaseHelper.rekomendasiMakanan(statusGizi, usiaBulan)
        try {
            binding.tvRekomendasiMakanan?.text = rekomendasi.joinToString("\n")
            binding.tvRekomendasiMakanan?.visibility = View.VISIBLE
        } catch (_: Exception) {
            // tvRekomendasiMakanan mungkin belum ada di layout lama — skip
        }
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }
        binding.btnUpdateGizi.setOnClickListener {
            findNavController().navigate(R.id.action_gizi_to_input_gizi)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
