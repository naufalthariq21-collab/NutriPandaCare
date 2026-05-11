package com.example.nutripandacare.fragment.orangtua

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

    private fun loadGiziData() {
        FirebaseHelper.getDataAnak(
            onSuccess = { anakId, data ->
                if (_binding == null) return@getDataAnak
                
                val namaAnak = data["nama_anak"] as? String ?: "Anak"
                val statusGizi = data["status_gizi"] as? String ?: "Belum dicek"
                val zScore = (data["z_score"] as? Number)?.toDouble() ?: 0.0
                
                binding.tvStatusGiziNama.text = statusGizi
                binding.tvStatusGiziDetail.text = "$namaAnak | Z-Score: ${"%.2f".format(zScore)}"
                
                loadGiziHistory(anakId)
            },
            onError = { err ->
                if (_binding == null) return@getDataAnak
                Toast.makeText(requireContext(), err, Toast.LENGTH_SHORT).show()
                binding.tvStatusGiziNama.text = "Data belum ada"
            }
        )
    }

    private fun loadGiziHistory(anakId: String) {
        FirebaseHelper.getRiwayatGizi(anakId, limit = 1,
            onSuccess = { history ->
                if (_binding == null) return@getRiwayatGizi
                
                if (history.isNotEmpty()) {
                    val latest = history[0]
                    binding.tvAvgKalori.text = latest["kalori"]?.toString() ?: "0"
                    binding.tvAvgProtein.text = latest["protein"]?.toString() ?: "0g"
                    binding.tvNutrisiPersen.text = "${latest["persentase_nutrisi"] ?: "0"}%"
                } else {
                    binding.tvAvgKalori.text = "0"
                    binding.tvAvgProtein.text = "0g"
                    binding.tvNutrisiPersen.text = "0%"
                }
                
                val sdf = SimpleDateFormat("MMMM yyyy", Locale("id", "ID"))
                binding.tvPeriodeGizi.text = sdf.format(Date())
            },
            onError = { }
        )
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }
        
        binding.btnUpdateGizi.setOnClickListener {
            findNavController().navigate(R.id.inputGiziFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
