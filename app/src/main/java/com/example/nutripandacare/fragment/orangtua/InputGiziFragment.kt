package com.example.nutripandacare.fragment.orangtua

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.ViewGroup.LayoutParams
import android.widget.TextView
import com.example.nutripandacare.R
import com.example.nutripandacare.databinding.FragmentInputGiziBinding
import com.example.nutripandacare.firebase.FirebaseHelper

class InputGiziFragment : Fragment() {

    private var _binding: FragmentInputGiziBinding? = null
    private val binding get() = _binding!!
    private var anakId: String = ""

    private var currentZScore: Double = 0.0
    private var currentStatus: String = ""
    private var currentPersentase: Int = 0

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
        binding.btnSimpanGizi.isEnabled = false

        FirebaseHelper.getDataAnak(
            onSuccess = { id, data ->
                if (_binding == null) return@getDataAnak
                anakId = id
                // ... (kode pengisian data)
            },
            onError = {
                if (_binding == null) return@getDataAnak
                // Jika data belum ada, arahkan untuk isi profil dulu
                if (it.contains("belum ada", true)) {
                    Toast.makeText(requireContext(), "Silakan lengkapi Profil Anak terlebih dahulu", Toast.LENGTH_LONG).show()
                    findNavController().navigate(R.id.action_home_to_edit_profil_anak) // Atau arahkan manual
                }
            }
        )
    }

    @SuppressLint("SetTextI18n")
    private fun setupActions() {
        binding.btnHitungGizi.setOnClickListener {
            val berat     = binding.etBeratBadan.text.toString().toDoubleOrNull() ?: 0.0
            val tinggi    = binding.etTinggiBadan.text.toString().toDoubleOrNull() ?: 0.0
            val usiaBulan = binding.etUsiaBulanGizi.text.toString().toIntOrNull() ?: 0

            if (berat <= 0.0) {
                binding.etBeratBadan.error = "Masukkan berat badan yang valid"
                return@setOnClickListener
            }
            if (tinggi <= 0.0) {
                binding.etTinggiBadan.error = "Masukkan tinggi badan yang valid"
                return@setOnClickListener
            }
            if (usiaBulan <= 0) {
                binding.etUsiaBulanGizi.error = "Masukkan usia dalam bulan"
                return@setOnClickListener
            }

            // Hitung Z-Score
            currentZScore   = FirebaseHelper.hitungZScore(berat, usiaBulan)
            val zScoreTbu   = FirebaseHelper.hitungZScoreTbu(tinggi, usiaBulan)
            currentStatus   = tentukanStatusGizi(currentZScore, zScoreTbu)
            currentPersentase = FirebaseHelper.hitungPersentaseNutrisi(currentZScore)

            // Tampilkan hasil
            binding.tvHasilStatusGizi.text  = currentStatus
            binding.tvHasilZscore.text      = "Z-Score BB/U: ${"%.2f".format(currentZScore)}  |  TB/U: ${"%.2f".format(zScoreTbu)}"
            binding.cardHasilGizi.visibility = View.VISIBLE

            // Aktifkan tombol simpan hanya jika ID anak sudah ada
            if (anakId.isNotEmpty()) {
                binding.btnSimpanGizi.isEnabled = true
            }

            tampilkanRekomendasi(currentStatus, usiaBulan)
        }

        binding.btnSimpanGizi.setOnClickListener {
            if (anakId.isEmpty()) {
                Toast.makeText(requireContext(), "Data anak belum siap, silakan tunggu...", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (currentStatus.isEmpty()) {
                Toast.makeText(requireContext(), "Hitung status gizi dulu sebelum menyimpan", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val berat     = binding.etBeratBadan.text.toString().toDoubleOrNull() ?: 0.0
            val tinggi    = binding.etTinggiBadan.text.toString().toDoubleOrNull() ?: 0.0
            val usiaBulan = binding.etUsiaBulanGizi.text.toString().toIntOrNull() ?: 0

            binding.btnSimpanGizi.isEnabled = false
            binding.btnSimpanGizi.text      = "Menyimpan..."

            FirebaseHelper.simpanHasilGizi(
                anakId            = anakId,
                berat             = berat,
                tinggi            = tinggi,
                usiaBulan         = usiaBulan,
                zScore            = currentZScore,
                statusGizi        = currentStatus,
                persentaseNutrisi = currentPersentase,
                onSuccess = {
                    if (_binding == null) return@simpanHasilGizi
                    Toast.makeText(requireContext(), "Data gizi berhasil disimpan ✅", Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                },
                onError = {
                    if (_binding == null) return@simpanHasilGizi
                    binding.btnSimpanGizi.isEnabled = true
                    binding.btnSimpanGizi.text      = "Simpan Data Gizi"
                    Toast.makeText(requireContext(), "Gagal menyimpan: $it", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    private fun tentukanStatusGizi(zBbu: Double, zTbu: Double): String {
        val isStunting   = zTbu < -2.0
        val isSevereStunt = zTbu < -3.0

        return when {
            isSevereStunt && zBbu < -3.0 -> "Stunting - Gizi Buruk"
            isSevereStunt                -> "Stunting Berat"
            isStunting && zBbu < -2.0   -> "Stunting - Gizi Kurang"
            isStunting                  -> "Stunting"
            zBbu < -3.0                 -> "Gizi Buruk"
            zBbu < -2.0                 -> "Gizi Kurang"
            zBbu <= 1.0                 -> "Normal"
            zBbu <= 2.0                 -> "Berisiko Gizi Lebih"
            zBbu <= 3.0                 -> "Gizi Lebih"
            else                        -> "Obesitas"
        }
    }

    @SuppressLint("SetTextI18n")
    private fun tampilkanRekomendasi(statusGizi: String, usiaBulan: Int) {
        val rekomendasi = FirebaseHelper.rekomendasiMakanan(statusGizi, usiaBulan)

        try {
            val rv = binding.root.findViewById<RecyclerView>(
                resources.getIdentifier("rvRekomendasi", "id", requireContext().packageName)
            )
            if (rv != null) {
                rv.visibility    = View.VISIBLE
                rv.layoutManager = LinearLayoutManager(requireContext())
                rv.adapter       = RekomendasiAdapter(rekomendasi)
                return
            }
        } catch (_: Exception) {}

        try {
            val tv = binding.root.findViewById<TextView>(
                resources.getIdentifier("tvRekomendasi", "id", requireContext().packageName)
            )
            tv?.let {
                it.visibility = View.VISIBLE
                it.text       = rekomendasi.joinToString("\n")
            }
        } catch (_: Exception) {}
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    inner class RekomendasiAdapter(
        private val items: List<String>
    ) : RecyclerView.Adapter<RekomendasiAdapter.VH>() {

        inner class VH(val tv: TextView) : RecyclerView.ViewHolder(tv)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val tv = TextView(parent.context).apply {
                layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
                setPadding(0, 8, 0, 8)
                textSize = 14f
            }
            return VH(tv)
        }

        override fun getItemCount() = items.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            holder.tv.text = items[position]
        }
    }
}
