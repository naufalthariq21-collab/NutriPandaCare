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

    @SuppressLint("SetTextI18n")
    private fun loadAnakData() {
        binding.btnSimpanGizi.isEnabled = false
        binding.btnHitungGizi.isEnabled = false

        FirebaseHelper.getDataAnak(
            onSuccess = { id, data ->
                if (_binding == null) return@getDataAnak
                anakId = id

                val usiaBulan = when (val v = data["usia_bulan"]) {
                    is Long   -> v.toInt()
                    is Int    -> v
                    is Number -> v.toInt()
                    is String -> v.toIntOrNull() ?: 0
                    else      -> 0
                }
                if (usiaBulan > 0) {
                    binding.etUsiaBulanGizi.setText(usiaBulan.toString())
                }

                val beratTerakhir  = (data["berat_badan"]  as? Number)?.toDouble() ?: 0.0
                val tinggiTerakhir = (data["tinggi_badan"] as? Number)?.toDouble() ?: 0.0
                if (beratTerakhir  > 0.0) binding.etBeratBadan.setText("%.1f".format(beratTerakhir))
                if (tinggiTerakhir > 0.0) binding.etTinggiBadan.setText("%.1f".format(tinggiTerakhir))

                binding.btnHitungGizi.isEnabled = true
            },
            onError = { pesan ->
                if (_binding == null) return@getDataAnak
                binding.btnHitungGizi.isEnabled = true

                if (pesan.contains("belum ada", true) || pesan.contains("Data anak", true)) {
                    Toast.makeText(requireContext(), "Silakan lengkapi Profil Anak terlebih dahulu", Toast.LENGTH_LONG).show()
                    try {
                        findNavController().navigate(R.id.action_home_to_edit_profil_anak)
                    } catch (e: Exception) {
                        requireActivity().onBackPressedDispatcher.onBackPressed()
                    }
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

            currentZScore     = FirebaseHelper.hitungZScore(berat, usiaBulan)
            val zScoreTbu     = FirebaseHelper.hitungZScoreTbu(tinggi, usiaBulan)
            currentStatus     = FirebaseHelper.tentukanStatusGizi(currentZScore, zScoreTbu)
            currentPersentase = FirebaseHelper.hitungPersentaseNutrisi(currentZScore)

            binding.tvHasilStatusGizi.text  = currentStatus
            binding.tvHasilZscore.text      =
                "Z-Score BB/U: ${"%.2f".format(currentZScore)}  |  TB/U: ${"%.2f".format(zScoreTbu)}"
            binding.cardHasilGizi.visibility = View.VISIBLE

            binding.btnSimpanGizi.isEnabled = anakId.isNotEmpty()
            tampilkanRekomendasi(currentStatus, usiaBulan)
        }

        binding.btnSimpanGizi.setOnClickListener {
            if (anakId.isEmpty()) {
                Toast.makeText(requireContext(), "Data anak belum siap", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (currentStatus.isEmpty()) {
                Toast.makeText(requireContext(), "Hitung status gizi dulu", Toast.LENGTH_SHORT).show()
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

    @SuppressLint("SetTextI18n")
    private fun tampilkanRekomendasi(statusGizi: String, usiaBulan: Int) {
        val rekomendasi = FirebaseHelper.rekomendasiMakanan(statusGizi, usiaBulan)

        // Menggunakan ViewBinding untuk menghindari masalah 'Unresolved reference' pada R.id
        binding.rvRekomendasi.apply {
            visibility = View.VISIBLE
            layoutManager = LinearLayoutManager(requireContext())
            adapter = RekomendasiAdapter(rekomendasi)
        }

        binding.tvRekomendasi.apply {
            text = rekomendasi.joinToString("\n")
            // Sembunyikan TextView jika RecyclerView sudah ditampilkan
            visibility = View.GONE 
        }
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
