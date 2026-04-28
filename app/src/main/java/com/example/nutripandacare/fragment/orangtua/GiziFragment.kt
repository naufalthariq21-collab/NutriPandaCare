package com.example.nutripandacare.fragment.orangtua

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
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

    private fun loadGiziData() {
        // Ambil data anak
        FirebaseHelper.getDataAnak(
            onSuccess = { anakId, data ->
                if (_binding == null) return@getDataAnak
                
                val namaAnak = data["nama_anak"] as? String ?: "Anak"
                val statusGizi = data["status_gizi"] as? String ?: "Normal"
                val zScore = (data["z_score"] as? Number)?.toDouble() ?: 0.0
                
                binding.tvStatusGiziNama.text = statusGizi
                binding.tvStatusGiziDetail.text = "$namaAnak | Z-Score: ${"%.2f".format(zScore)}"
                
                // Ambil rata-rata gizi mingguan (dummy logic or from history)
                loadGiziHistory(anakId)
            },
            onError = { err ->
                if (_binding == null) return@getDataAnak
                Toast.makeText(requireContext(), err, Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun loadGiziHistory(anakId: String) {
        FirebaseHelper.getRiwayatGizi(anakId, limit = 7,
            onSuccess = { history ->
                if (_binding == null || history.isEmpty()) return@getRiwayatGizi
                
                // Kalkulasi rata-rata (Contoh sederhana)
                var totalCal = 0.0
                var totalProt = 0.0
                var totalNutr = 0
                
                history.forEach {
                    // Di Firestore riwayat_gizi harusnya simpan kalori/protein per hari
                    // Untuk saat ini kita tampilkan data terakhir jika field tersebut belum ada
                }
                
                val latest = history[0]
                binding.tvAvgKalori.text = (latest["kalori"] ?: "450").toString()
                binding.tvAvgProtein.text = (latest["protein"] ?: "25g").toString()
                binding.tvNutrisiPersen.text = "${latest["persentase_nutrisi"] ?: "75"}%"
                
                val sdf = SimpleDateFormat("MMMM yyyy", Locale("id", "ID"))
                binding.tvPeriodeGizi.text = sdf.format(Date())
            },
            onError = { }
        )
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
        
        binding.btnUpdateGizi.setOnClickListener {
            // Navigasi ke form input antropometri (jika ada)
            Toast.makeText(requireContext(), "Fitur Update segera hadir", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
