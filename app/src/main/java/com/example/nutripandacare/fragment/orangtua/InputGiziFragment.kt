package com.example.nutripandacare.fragment.orangtua

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.nutripandacare.databinding.FragmentInputGiziBinding
import com.example.nutripandacare.firebase.FirebaseHelper

class InputGiziFragment : Fragment() {

    private var _binding: FragmentInputGiziBinding? = null
    private val binding get() = _binding!!
    private var anakId: String = ""
    
    private var currentZScore: Double = 0.0
    private var currentStatus: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInputGiziBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        loadAnakData()
        setupActions()
    }

    private fun loadAnakData() {
        FirebaseHelper.getDataAnak(
            onSuccess = { id, data ->
                anakId = id
                binding.etUsiaBulanGizi.setText((data["usia_bulan"] as? Long)?.toString() ?: "")
            },
            onError = {
                Toast.makeText(requireContext(), "Gagal memuat data anak", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun setupActions() {
        binding.btnHitungGizi.setOnClickListener {
            val berat = binding.etBeratBadan.text.toString().toDoubleOrNull() ?: 0.0
            val tinggi = binding.etTinggiBadan.text.toString().toDoubleOrNull() ?: 0.0
            val usiaBulan = binding.etUsiaBulanGizi.text.toString().toIntOrNull() ?: 0

            if (berat == 0.0 || tinggi == 0.0 || usiaBulan == 0) {
                Toast.makeText(requireContext(), "Harap isi semua data dengan benar", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            currentZScore = FirebaseHelper.hitungZScore(berat, usiaBulan)
            currentStatus = FirebaseHelper.kategoriGizi(currentZScore)

            binding.tvHasilStatusGizi.text = currentStatus
            binding.tvHasilZscore.text = "Z-Score: %.2f".format(currentZScore)
            binding.cardHasilGizi.visibility = View.VISIBLE
        }

        binding.btnSimpanGizi.setOnClickListener {
            val berat = binding.etBeratBadan.text.toString().toDoubleOrNull() ?: 0.0
            val tinggi = binding.etTinggiBadan.text.toString().toDoubleOrNull() ?: 0.0
            val usiaBulan = binding.etUsiaBulanGizi.text.toString().toIntOrNull() ?: 0
            
            // Dummy percentage logic for now
            val percentage = if (currentStatus == "Normal") 100 else 75

            FirebaseHelper.simpanHasilGizi(
                anakId, berat, tinggi, usiaBulan, currentZScore, currentStatus, percentage,
                onSuccess = {
                    Toast.makeText(requireContext(), "Data gizi berhasil disimpan", Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                },
                onError = {
                    Toast.makeText(requireContext(), "Gagal menyimpan: $it", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
