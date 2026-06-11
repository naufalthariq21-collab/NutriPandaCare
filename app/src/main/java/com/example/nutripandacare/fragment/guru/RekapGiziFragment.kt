package com.example.nutripandacare.fragment.guru

import android.app.AlertDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.nutripandacare.R
import com.example.nutripandacare.databinding.FragmentRekapGiziBinding
import com.example.nutripandacare.firebase.FirebaseHelper
import com.google.firebase.Timestamp
import kotlin.math.pow

class RekapGiziFragment : Fragment() {

    private var _binding: FragmentRekapGiziBinding? = null
    private val binding get() = _binding!!

    private val semuaSiswa    = mutableListOf<Map<String, Any?>>()
    private val filteredSiswa = mutableListOf<Map<String, Any?>>()
    private lateinit var adapter: SiswaAdapter

    private val statusBerisiko = setOf("gizi kurang", "gizi buruk", "gizi lebih", "obesitas", "stunting")

    private var filterAktif = "semua"
    private var rekapId: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRekapGiziBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupSearch()
        setupChips()
        setupClickListeners()
        loadSemuaSiswa()
    }

    private fun setupRecyclerView() {
        adapter = SiswaAdapter(filteredSiswa) { siswaData ->
            showDetailDialog(siswaData)
        }
        binding.rvDaftarSiswa.layoutManager = LinearLayoutManager(requireContext())
        binding.rvDaftarSiswa.adapter = adapter
    }

    private fun setupSearch() {
        binding.etCariSiswa.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { applyFilter() }
        })
    }

    private fun setupChips() {
        binding.chipSemua.setOnClickListener    { filterAktif = "semua";    applyFilter() }
        binding.chipNormal.setOnClickListener   { filterAktif = "normal";   applyFilter() }
        binding.chipStunting.setOnClickListener { filterAktif = "stunting"; applyFilter() }
        binding.chipResiko.setOnClickListener   { filterAktif = "berisiko"; applyFilter() }
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
        binding.btnTambahSiswa.setOnClickListener {
            showTambahSiswaDialog()
        }
    }

    // ─── LOAD DATA ───────────────────────────────────────────────────────────

    private fun loadSemuaSiswa() {
        FirebaseHelper.getRekapGizi(
            onSuccess = { rekapList ->
                if (_binding == null) return@getRekapGizi
                semuaSiswa.clear()

                if (rekapList.isEmpty()) {
                    applyFilter()
                    updateSummaryStats()
                    return@getRekapGizi
                }

                // FIX (#10): Pakai rekapId milik guru yang login (field guru_uid)
                rekapId = rekapList.first().first

                var selesai = 0
                rekapList.forEach { (rId, _) ->
                    FirebaseHelper.getDetailSiswa(
                        rekapId   = rId,
                        onSuccess = { siswaList ->
                            siswaList.forEach { siswaData ->
                                semuaSiswa.add(siswaData + mapOf("rekap_id" to rId))
                            }
                            selesai++
                            if (selesai == rekapList.size) {
                                applyFilter()
                                updateSummaryStats()
                                // FIX (#9): Setelah load, update aggregate di Firestore supaya dashboard sinkron
                                updateStatistikRekap(rekapId)
                            }
                        },
                        onError = { _ ->
                            selesai++
                            if (selesai == rekapList.size) {
                                applyFilter()
                                updateSummaryStats()
                            }
                        }
                    )
                }
            },
            onError = { err ->
                if (_binding == null) return@getRekapGizi
                Toast.makeText(requireContext(), err, Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun applyFilter() {
        if (_binding == null) return
        val query = binding.etCariSiswa.text.toString().trim().lowercase()

        val filtered = semuaSiswa.filter { siswa ->
            val nama       = (siswa["nama_siswa"] as? String ?: "").lowercase()
            val statusGizi = (siswa["status_gizi"] as? String ?: "").lowercase().trim()
            val namaMatch  = nama.contains(query)
            val filterMatch = when (filterAktif) {
                "normal"   -> statusGizi == "normal"
                "stunting" -> statusGizi == "stunting"
                "berisiko" -> statusGizi in statusBerisiko
                else       -> true
            }
            namaMatch && filterMatch
        }

        filteredSiswa.clear()
        filteredSiswa.addAll(filtered)
        adapter.notifyDataSetChanged()
    }

    private fun updateSummaryStats() {
        if (_binding == null) return
        val total    = semuaSiswa.size
        val normal   = semuaSiswa.count { (it["status_gizi"] as? String ?: "").lowercase() == "normal" }
        val berisiko = semuaSiswa.count {
            (it["status_gizi"] as? String ?: "").lowercase().trim() in statusBerisiko
        }
        binding.tvTotalSiswa.text  = "Total: $total siswa"
        binding.tvNormalCount.text = "Normal: $normal"
        binding.tvResikoCount.text = "Berisiko: $berisiko"
    }

    // ─── DIALOG TAMBAH SISWA ─────────────────────────────────────────────────

    private fun showTambahSiswaDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_tambah_siswa, null)

        val etNama    = dialogView.findViewById<EditText>(R.id.etNamaSiswa)
        val etKelas   = dialogView.findViewById<EditText>(R.id.etKelasSiswa)
        val etBerat   = dialogView.findViewById<EditText>(R.id.etBeratSiswa)
        val etTinggi  = dialogView.findViewById<EditText>(R.id.etTinggiSiswa)
        val etUsia    = dialogView.findViewById<EditText>(R.id.etUsiaBulanSiswa)
        val btnHitung = dialogView.findViewById<Button>(R.id.btnHitungSiswa)
        val tvHasil   = dialogView.findViewById<TextView>(R.id.tvHasilGiziSiswa)

        var statusGiziHasil = ""
        var zScoreHasil     = 0.0

        btnHitung.setOnClickListener {
            val berat  = etBerat.text.toString().toDoubleOrNull()
            val tinggi = etTinggi.text.toString().toDoubleOrNull()
            val usia   = etUsia.text.toString().toIntOrNull()

            if (berat == null || tinggi == null || usia == null) {
                Toast.makeText(requireContext(), "Isi berat, tinggi, dan usia dengan benar", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val result  = hitungStatusGizi(berat, tinggi, usia)
            statusGiziHasil = result.first
            zScoreHasil     = result.second

            tvHasil.text       = "Status Gizi: $statusGiziHasil\nZ-Score: ${"%.2f".format(zScoreHasil)}"
            tvHasil.visibility = View.VISIBLE
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Tambah Data Siswa")
            .setView(dialogView)
            .setPositiveButton("Simpan") { _, _ ->
                val nama   = etNama.text.toString().trim()
                val kelas  = etKelas.text.toString().trim()
                val berat  = etBerat.text.toString().toDoubleOrNull() ?: 0.0
                val tinggi = etTinggi.text.toString().toDoubleOrNull() ?: 0.0
                val usia   = etUsia.text.toString().toIntOrNull() ?: 0

                if (nama.isEmpty()) {
                    Toast.makeText(requireContext(), "Nama siswa wajib diisi", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (statusGiziHasil.isEmpty()) {
                    val result  = hitungStatusGizi(berat, tinggi, usia)
                    statusGiziHasil = result.first
                    zScoreHasil     = result.second
                }

                simpanSiswa(nama, kelas, berat, tinggi, usia, statusGiziHasil, zScoreHasil)
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun simpanSiswa(
        nama: String, kelas: String,
        berat: Double, tinggi: Double, usia: Int,
        statusGizi: String, zScore: Double
    ) {
        val dataSiswa = mapOf(
            "nama_siswa"   to nama,
            "kelas"        to kelas,
            "berat_badan"  to berat,
            "tinggi_badan" to tinggi,
            "usia_bulan"   to usia,
            "status_gizi"  to statusGizi,
            "z_score"      to zScore,
            "created_at"   to Timestamp.now()
        )

        if (rekapId.isEmpty()) {
            // FIX (#10): Pakai guru_uid (konsisten dengan FirebaseHelper.getRekapGizi yang query berdasarkan guru_uid)
            val uid = FirebaseHelper.uid
            FirebaseHelper.db.collection("rekap_gizi")
                .add(mapOf(
                    "guru_uid"    to uid,   // ← FIX: pakai guru_uid bukan guru_id
                    "created_at"  to Timestamp.now(),
                    "total_siswa" to 0,
                    "normal"      to 0,
                    "gizi_kurang" to 0,
                    "stunting"    to 0,
                    "obesitas"    to 0
                ))
                .addOnSuccessListener { docRef ->
                    rekapId = docRef.id
                    tambahSiswaKeRekap(rekapId, dataSiswa)
                }
                .addOnFailureListener { e ->
                    if (_binding != null) {
                        Toast.makeText(requireContext(), "Gagal buat rekap: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        } else {
            tambahSiswaKeRekap(rekapId, dataSiswa)
        }
    }

    private fun tambahSiswaKeRekap(rId: String, dataSiswa: Map<String, Any?>) {
        FirebaseHelper.db
            .collection("rekap_gizi").document(rId)
            .collection("detail_siswa")
            .add(dataSiswa)
            .addOnSuccessListener {
                if (_binding == null) return@addOnSuccessListener
                Toast.makeText(requireContext(), "Siswa berhasil ditambahkan ✅", Toast.LENGTH_SHORT).show()
                semuaSiswa.add(dataSiswa + mapOf("rekap_id" to rId))
                applyFilter()
                updateSummaryStats()
                // FIX (#6 & #9): Update aggregate di Firestore supaya HomeGuru sinkron
                updateStatistikRekap(rId)
            }
            .addOnFailureListener { e ->
                if (_binding != null) {
                    Toast.makeText(requireContext(), "Gagal simpan: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    // FIX (#6 & #9): Update aggregate di dokumen rekap_gizi
    // HomeGuruFragment membaca field ini (total_siswa, normal, gizi_kurang, stunting, obesitas)
    private fun updateStatistikRekap(rId: String) {
        val siswaInRekap = semuaSiswa.filter { (it["rekap_id"] as? String) == rId }
        val total      = siswaInRekap.size
        val normal     = siswaInRekap.count { (it["status_gizi"] as? String ?: "").lowercase() == "normal" }
        val giziKurang = siswaInRekap.count {
            (it["status_gizi"] as? String ?: "").lowercase() in setOf("gizi kurang", "gizi buruk")
        }
        val stunting   = siswaInRekap.count {
            (it["status_gizi"] as? String ?: "").lowercase() == "stunting"
        }
        val obesitas   = siswaInRekap.count {
            (it["status_gizi"] as? String ?: "").lowercase() in setOf("gizi lebih", "obesitas")
        }

        FirebaseHelper.db.collection("rekap_gizi").document(rId)
            .update(mapOf(
                "total_siswa" to total,
                "normal"      to normal,
                "gizi_kurang" to giziKurang,
                "stunting"    to stunting,
                "obesitas"    to obesitas
            ))
    }

    // ─── HITUNG STATUS GIZI ───────────────────────────────────────────────────

    private fun hitungStatusGizi(beratKg: Double, tinggiCm: Double, usiaBulan: Int): Pair<String, Double> {
        val zScoreBbu = FirebaseHelper.hitungZScore(beratKg, usiaBulan)
        val zScoreTbu = FirebaseHelper.hitungZScoreTbu(tinggiCm, usiaBulan)

        val status = when {
            zScoreTbu < -3.0 && zScoreBbu < -3.0 -> "Stunting - Gizi Buruk"
            zScoreTbu < -2.0 && zScoreBbu < -2.0 -> "Stunting - Gizi Kurang"
            zScoreTbu < -2.0                      -> "Stunting"
            zScoreBbu < -3.0                      -> "Gizi Buruk"
            zScoreBbu < -2.0                      -> "Gizi Kurang"
            zScoreBbu <= 1.0                       -> "Normal"
            zScoreBbu <= 2.0                       -> "Gizi Lebih"
            else                                   -> "Obesitas"
        }
        return Pair(status, zScoreBbu)
    }

    // ─── DIALOG DETAIL SISWA ─────────────────────────────────────────────────

    private fun showDetailDialog(siswa: Map<String, Any?>) {
        val nama       = siswa["nama_siswa"]    as? String ?: "-"
        val kelas      = siswa["kelas"]         as? String ?: "-"
        val berat      = (siswa["berat_badan"]  as? Number)?.toDouble() ?: 0.0
        val tinggi     = (siswa["tinggi_badan"] as? Number)?.toDouble() ?: 0.0
        val usia       = (siswa["usia_bulan"]   as? Number)?.toInt() ?: 0
        val statusGizi = siswa["status_gizi"]   as? String ?: "-"
        val zScore     = (siswa["z_score"]      as? Number)?.toDouble() ?: 0.0

        val rekomendasi = rekomendasiMakanan(statusGizi)

        AlertDialog.Builder(requireContext())
            .setTitle("Detail: $nama")
            .setMessage(
                "Kelas      : $kelas\n" +
                        "Usia       : $usia bulan\n" +
                        "Berat      : $berat kg\n" +
                        "Tinggi     : $tinggi cm\n" +
                        "Z-Score    : ${"%.2f".format(zScore)}\n" +
                        "Status Gizi: $statusGizi\n\n" +
                        "📋 Rekomendasi:\n$rekomendasi"
            )
            .setPositiveButton("Tutup", null)
            .show()
    }

    private fun rekomendasiMakanan(status: String): String = when (status.lowercase().trim()) {
        "gizi buruk"             -> "Segera rujuk ke puskesmas. Tingkatkan asupan protein (telur, ikan, daging) dan kalori tinggi."
        "gizi kurang"            -> "Tambah porsi makan, perbanyak protein (tempe, tahu, ikan), sayuran hijau, dan susu."
        "stunting", "stunting - gizi kurang", "stunting - gizi buruk"
            -> "Perbanyak makanan kaya protein dan kalsium. Pantau tinggi badan setiap bulan."
        "gizi lebih"             -> "Kurangi makanan berlemak dan manis, perbanyak sayur dan buah, rutin olahraga."
        "obesitas"               -> "Konsultasi ke dokter/ahli gizi. Batasi kalori, perbanyak aktivitas fisik."
        else                     -> "Pertahankan pola makan seimbang: nasi, lauk protein, sayuran, buah, dan susu."
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // ─── ADAPTER ─────────────────────────────────────────────────────────────

    inner class SiswaAdapter(
        private val data: List<Map<String, Any?>>,
        private val onClick: (Map<String, Any?>) -> Unit
    ) : RecyclerView.Adapter<SiswaAdapter.VH>() {

        inner class VH(view: View) : RecyclerView.ViewHolder(view) {
            val tvInisial: TextView = view.findViewById(R.id.tvInisialSiswa)
            val tvNama: TextView    = view.findViewById(R.id.tvNamaSiswa)
            val tvInfo: TextView    = view.findViewById(R.id.tvInfoSiswa)
            val tvStatus: TextView  = view.findViewById(R.id.tvStatusGiziSiswa)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_siswa, parent, false)
            return VH(view)
        }

        override fun getItemCount() = data.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val siswa      = data[position]
            val nama       = siswa["nama_siswa"]  as? String ?: "-"
            val kelas      = siswa["kelas"]       as? String ?: "-"
            val zScore     = (siswa["z_score"]    as? Number)?.toDouble() ?: 0.0
            val statusGizi = siswa["status_gizi"] as? String ?: "Normal"

            val inisial = nama.split(" ")
                .take(2).joinToString("") { it.firstOrNull()?.toString() ?: "" }
            holder.tvInisial.text = inisial.uppercase()
            holder.tvNama.text    = nama
            holder.tvInfo.text    = "$kelas • Z-Score: ${"%.1f".format(zScore)}"
            holder.tvStatus.text  = statusGizi

            val (bgColor, txtColor) = when (statusGizi.lowercase().trim()) {
                "normal"                 -> Pair(R.color.green_pastel, R.color.green_primary)
                "gizi kurang",
                "gizi buruk",
                "stunting",
                "stunting - gizi kurang",
                "stunting - gizi buruk"  -> Pair(R.color.pending_bg,  R.color.pending_text)
                "gizi lebih", "obesitas" -> Pair(R.color.cream_warm,  R.color.text_secondary)
                else                     -> Pair(R.color.cream_warm,  R.color.text_secondary)
            }

            holder.tvStatus.setBackgroundResource(bgColor)
            holder.tvStatus.setTextColor(requireContext().getColor(txtColor))
            holder.itemView.setOnClickListener { onClick(siswa) }
        }
    }
}