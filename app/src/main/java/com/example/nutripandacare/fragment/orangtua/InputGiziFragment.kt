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

    // GAP-9 FIX: implementasikan body loadAnakData yang sebelumnya kosong
    // (hanya ada "// ... (kode pengisian data)" — placeholder tanpa implementasi).
    // Sekarang mengisi usia_bulan default dari data anak ke field input,
    // dan menggunakan key konsisten: "nama_anak", "usia_bulan" sesuai Firestore schema.
    // FR-07 & UAT B-6 terpenuhi: form input siap dengan data anak yang sudah ada.
    @SuppressLint("SetTextI18n")
    private fun loadAnakData() {
        binding.btnSimpanGizi.isEnabled = false
        binding.btnHitungGizi.isEnabled = false

        FirebaseHelper.getDataAnak(
            onSuccess = { id, data ->
                if (_binding == null) return@getDataAnak
                anakId = id

                // Isi usia bulan default dari profil anak agar user tidak perlu input ulang
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

                // Isi berat & tinggi terakhir dari profil anak sebagai nilai default
                val beratTerakhir  = (data["berat_badan"]  as? Number)?.toDouble() ?: 0.0
                val tinggiTerakhir = (data["tinggi_badan"] as? Number)?.toDouble() ?: 0.0
                if (beratTerakhir  > 0.0) binding.etBeratBadan.setText("${"%.1f".format(beratTerakhir)}")
                if (tinggiTerakhir > 0.0) binding.etTinggiBadan.setText("${"%.1f".format(tinggiTerakhir)}")

                // Aktifkan tombol setelah anakId siap
                binding.btnHitungGizi.isEnabled = true
                binding.btnSimpanGizi.isEnabled = false // tetap disabled sampai user hitung dulu
            },
            onError = { pesan ->
                if (_binding == null) return@getDataAnak
                binding.btnHitungGizi.isEnabled = true

                // UAT B-5: jika profil anak belum diisi, arahkan ke Edit Profil Anak
                if (pesan.contains("belum ada", true) || pesan.contains("Data anak", true)) {
                    Toast.makeText(
                        requireContext(),
                        "Silakan lengkapi Profil Anak terlebih dahulu",
                        Toast.LENGTH_LONG
                    ).show()
                    // Navigasi ke edit profil anak agar user bisa isi data dulu
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
        // UAT B-6: Hitung Status Gizi berdasarkan input berat, tinggi, usia
        binding.btnHitungGizi.setOnClickListener {
            val berat     = binding.etBeratBadan.text.toString().toDoubleOrNull() ?: 0.0
            val tinggi    = binding.etTinggiBadan.text.toString().toDoubleOrNull() ?: 0.0
            val usiaBulan = binding.etUsiaBulanGizi.text.toString().toIntOrNull() ?: 0

            // UAT B-7: validasi field tidak boleh kosong / nol
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

            // Hitung Z-Score WHO (FR-08)
            currentZScore     = FirebaseHelper.hitungZScore(berat, usiaBulan)
            val zScoreTbu     = FirebaseHelper.hitungZScoreTbu(tinggi, usiaBulan)
            currentStatus     = tentukanStatusGizi(currentZScore, zScoreTbu)
            currentPersentase = FirebaseHelper.hitungPersentaseNutrisi(currentZScore)

            // Tampilkan hasil Z-Score dan status gizi
            binding.tvHasilStatusGizi.text  = currentStatus
            binding.tvHasilZscore.text      =
                "Z-Score BB/U: ${"%.2f".format(currentZScore)}  |  TB/U: ${"%.2f".format(zScoreTbu)}"
            binding.cardHasilGizi.visibility = View.VISIBLE

            // Aktifkan tombol simpan hanya jika ID anak sudah ada
            binding.btnSimpanGizi.isEnabled = anakId.isNotEmpty()

            // FR-09: tampilkan rekomendasi makanan sesuai status gizi (UAT B-6)
            tampilkanRekomendasi(currentStatus, usiaBulan)
        }

        // UAT B-8: simpan data gizi ke riwayat anak
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

    // FR-08: Tentukan status gizi berdasarkan Z-Score BB/U dan TB/U (standar WHO)
    private fun tentukanStatusGizi(zBbu: Double, zTbu: Double): String {
        val isStunting    = zTbu < -2.0
        val isSevereStunt = zTbu < -3.0

        return when {
            isSevereStunt && zBbu < -3.0 -> "Stunting - Gizi Buruk"
            isSevereStunt                 -> "Stunting Berat"
            isStunting && zBbu < -2.0    -> "Stunting - Gizi Kurang"
            isStunting                   -> "Stunting"
            zBbu < -3.0                  -> "Gizi Buruk"
            zBbu < -2.0                  -> "Gizi Kurang"
            zBbu <= 1.0                  -> "Normal"
            zBbu <= 2.0                  -> "Berisiko Gizi Lebih"
            zBbu <= 3.0                  -> "Gizi Lebih"
            else                         -> "Obesitas"
        }
    }

    // FR-09: tampilkan rekomendasi makanan berdasarkan status gizi
    @SuppressLint("SetTextI18n")
    private fun tampilkanRekomendasi(statusGizi: String, usiaBulan: Int) {
        val rekomendasi = FirebaseHelper.rekomendasiMakanan(statusGizi, usiaBulan)

        // Coba tampilkan di RecyclerView jika ada
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

        // Fallback: tampilkan di TextView jika RecyclerView tidak ada
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

    // Adapter sederhana untuk menampilkan daftar rekomendasi makanan
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
