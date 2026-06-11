package com.example.nutripandacare.fragment.guru

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.nutripandacare.R
import com.example.nutripandacare.databinding.FragmentRekapGiziBinding
import com.example.nutripandacare.firebase.FirebaseHelper
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.*

class RekapGiziFragment : Fragment() {

    private var _binding: FragmentRekapGiziBinding? = null
    private val binding get() = _binding!!

    private val semuaSiswa    = mutableListOf<Map<String, Any?>>()
    private val filteredSiswa = mutableListOf<Map<String, Any?>>()
    private lateinit var adapter: SiswaAdapter

    private val statusBerisiko = setOf(
        "gizi kurang", "gizi buruk", "gizi lebih", "obesitas",
        "stunting", "stunting berat", "stunting - gizi buruk",
        "stunting - gizi kurang", "berisiko gizi lebih"
    )

    private var filterAktif = "semua"
    private var rekapId: String? = null

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
        loadAtauBuatRekap()
    }

    // ─── SETUP UI ────────────────────────────────────────────────────────────

    private fun setupRecyclerView() {
        adapter = SiswaAdapter(filteredSiswa) { siswaData -> showDetailSiswa(siswaData) }
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
        binding.chipSemua.setOnClickListener   { filterAktif = "semua";    applyFilter() }
        binding.chipNormal.setOnClickListener  { filterAktif = "normal";   applyFilter() }
        binding.chipResiko.setOnClickListener  { filterAktif = "berisiko"; applyFilter() }
        binding.chipStunting.setOnClickListener { filterAktif = "stunting"; applyFilter() }
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
        binding.btnTambahSiswa.setOnClickListener {
            val id = rekapId
            if (id != null) showTambahSiswaDialog(id)
            else Toast.makeText(requireContext(), "Memuat rekap, coba lagi...", Toast.LENGTH_SHORT).show()
        }
    }

    // ─── LOAD / BUAT REKAP ───────────────────────────────────────────────────

    private fun loadAtauBuatRekap() {
        binding.progressBar.visibility = View.VISIBLE
        FirebaseHelper.getRekapGizi(
            onSuccess = { list ->
                if (_binding == null) return@getRekapGizi
                if (list.isNotEmpty()) {
                    rekapId = list[0].first
                    loadDetailSiswa(rekapId!!)
                } else {
                    // Belum ada rekap → buat otomatis, list tetap kosong dulu
                    buatRekapBaru()
                }
            },
            onError = { err ->
                if (_binding == null) return@getRekapGizi
                binding.progressBar.visibility = View.GONE
                Toast.makeText(requireContext(), "Gagal memuat rekap: $err", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun buatRekapBaru() {
        val periode = SimpleDateFormat("MMMM yyyy", Locale("id", "ID")).format(Date())
        FirebaseHelper.getDataUser(FirebaseHelper.uid,
            onSuccess = { data ->
                val sekolah = data["sekolah"] as? String ?: "Sekolah"
                simpanRekap(sekolah, periode)
            },
            onError = { simpanRekap("Sekolah", periode) }
        )
    }

    private fun simpanRekap(sekolah: String, periode: String) {
        FirebaseHelper.simpanRekapGizi(
            sekolah = sekolah, periode = periode,
            totalSiswa = 0, normal = 0,
            giziKurang = 0, giziBuruk = 0,
            giziLebih = 0, obesitas = 0,
            onSuccess = { id ->
                if (_binding == null) return@simpanRekapGizi
                rekapId = id
                binding.progressBar.visibility = View.GONE
                // Rekap baru → list kosong, summary = 0
                semuaSiswa.clear()
                filteredSiswa.clear()
                adapter.notifyDataSetChanged()
                updateSummaryUI()
            },
            onError = { err ->
                if (_binding == null) return@simpanRekapGizi
                binding.progressBar.visibility = View.GONE
                Toast.makeText(requireContext(), "Gagal buat rekap: $err", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun loadDetailSiswa(id: String) {
        binding.progressBar.visibility = View.VISIBLE
        FirebaseHelper.getDetailSiswa(id,
            onSuccess = { list ->
                if (_binding == null) return@getDetailSiswa
                binding.progressBar.visibility = View.GONE
                semuaSiswa.clear()
                semuaSiswa.addAll(list)
                applyFilter()
                updateSummaryUI()
            },
            onError = { err ->
                if (_binding == null) return@getDetailSiswa
                binding.progressBar.visibility = View.GONE
                Toast.makeText(requireContext(), err, Toast.LENGTH_SHORT).show()
            }
        )
    }

    // ─── SUMMARY UI ──────────────────────────────────────────────────────────

    @SuppressLint("SetTextI18n")
    private fun updateSummaryUI() {
        if (_binding == null) return
        val total    = semuaSiswa.size
        val normal   = semuaSiswa.count { (it["status_gizi"] as? String ?: "").lowercase() == "normal" }
        val berisiko = semuaSiswa.count {
            val status = (it["status_gizi"] as? String ?: "").lowercase().trim()
            status in statusBerisiko && !status.contains("stunting")
        }
        val stunting = semuaSiswa.count {
            (it["status_gizi"] as? String ?: "").lowercase().trim().contains("stunting")
        }
        
        binding.tvSubHeaderSiswa.text = "$total Siswa Terdaftar"
        binding.tvStatNormal.text     = normal.toString()
        binding.tvStatResiko.text     = berisiko.toString()
        binding.tvStatStunting.text   = stunting.toString()
    }

    // ─── TAMBAH SISWA ────────────────────────────────────────────────────────

    @SuppressLint("SetTextI18n")
    private fun showTambahSiswaDialog(idRekap: String) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_tambah_siswa, null)

        val etNama    = dialogView.findViewById<EditText>(R.id.etNamaSiswa)
        val etKelas   = dialogView.findViewById<EditText>(R.id.etKelasSiswa)
        val etBerat   = dialogView.findViewById<EditText>(R.id.etBeratSiswa)
        val etTinggi  = dialogView.findViewById<EditText>(R.id.etTinggiSiswa)
        val etUsia    = dialogView.findViewById<EditText>(R.id.etUsiaBulanSiswa)
        val tvHasil   = dialogView.findViewById<TextView>(R.id.tvHasilGiziSiswa)
        val btnHitung = dialogView.findViewById<Button>(R.id.btnHitungSiswa)

        var currentStatus = ""
        var currentZScore = 0.0

        btnHitung.setOnClickListener {
            val berat  = etBerat.text.toString().toDoubleOrNull() ?: 0.0
            val tinggi = etTinggi.text.toString().toDoubleOrNull() ?: 0.0
            val usia   = etUsia.text.toString().toIntOrNull() ?: 0
            if (berat <= 0 || tinggi <= 0 || usia <= 0) {
                Toast.makeText(requireContext(), "Lengkapi berat, tinggi, dan usia", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            currentZScore = FirebaseHelper.hitungZScore(berat, usia)
            val zTbu      = FirebaseHelper.hitungZScoreTbu(tinggi, usia)
            currentStatus = tentukanStatus(currentZScore, zTbu)
            tvHasil.text = "Status: $currentStatus\nZ-Score BB/U: ${"%.2f".format(currentZScore)}"
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
                if (nama.isEmpty() || kelas.isEmpty()) {
                    Toast.makeText(requireContext(), "Nama dan kelas wajib diisi", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (currentStatus.isEmpty()) {
                    currentZScore = FirebaseHelper.hitungZScore(berat, usia)
                    val zTbu = FirebaseHelper.hitungZScoreTbu(tinggi, usia)
                    currentStatus = tentukanStatus(currentZScore, zTbu)
                }
                simpanDataSiswa(idRekap, nama, kelas, berat, tinggi, usia, currentZScore, currentStatus)
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun tentukanStatus(zBbu: Double, zTbu: Double): String {
        val isStunting       = zTbu < -2.0
        val isSevereStunting = zTbu < -3.0
        return when {
            isSevereStunting && zBbu < -3.0 -> "Stunting - Gizi Buruk"
            isSevereStunting                -> "Stunting Berat"
            isStunting && zBbu < -2.0       -> "Stunting - Gizi Kurang"
            isStunting                      -> "Stunting"
            zBbu < -3.0                     -> "Gizi Buruk"
            zBbu < -2.0                     -> "Gizi Kurang"
            zBbu <= 1.0                     -> "Normal"
            zBbu <= 2.0                     -> "Berisiko Gizi Lebih"
            zBbu <= 3.0                     -> "Gizi Lebih"
            else                            -> "Obesitas"
        }
    }

    private fun simpanDataSiswa(
        idRekap: String, nama: String, kelas: String,
        berat: Double, tinggi: Double, usia: Int,
        zScore: Double, status: String
    ) {
        FirebaseHelper.tambahDetailSiswa(
            rekapId    = idRekap,
            namaSiswa  = nama,
            kelas      = kelas,
            berat      = berat,
            tinggi     = tinggi,
            usiaBulan  = usia,
            zScore     = zScore,
            statusGizi = status,
            onSuccess  = {
                if (_binding == null) return@tambahDetailSiswa
                Toast.makeText(requireContext(), "Data $nama berhasil ditambahkan ✅", Toast.LENGTH_SHORT).show()
                loadDetailSiswa(idRekap)
                updateSummaryRekap(idRekap)
            },
            onError = { err ->
                if (_binding == null) return@tambahDetailSiswa
                Toast.makeText(requireContext(), "Gagal: $err", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun updateSummaryRekap(idRekap: String) {
        FirebaseHelper.getDetailSiswa(idRekap,
            onSuccess = { list ->
                var total = 0; var normal = 0; var kurang = 0
                var buruk = 0; var lebih = 0; var obs = 0
                list.forEach { siswa ->
                    total++
                    when ((siswa["status_gizi"] as? String ?: "").lowercase().trim()) {
                        "normal"                                                -> normal++
                        "gizi kurang", "stunting - gizi kurang", "stunting"    -> kurang++
                        "gizi buruk", "stunting - gizi buruk", "stunting berat" -> buruk++
                        "gizi lebih", "berisiko gizi lebih"                    -> lebih++
                        "obesitas"                                              -> obs++
                    }
                }
                FirebaseHelper.db.collection("rekap_gizi").document(idRekap)
                    .update(mapOf(
                        "total_siswa" to total,
                        "normal"      to normal,
                        "gizi_kurang" to kurang,
                        "gizi_buruk"  to buruk,
                        "gizi_lebih"  to lebih,
                        "obesitas"    to obs,
                        "updated_at"  to Timestamp.now()
                    ))
            },
            onError = {}
        )
    }

    // ─── FILTER ──────────────────────────────────────────────────────────────

    private fun applyFilter() {
        if (_binding == null) return
        val query = binding.etCariSiswa.text.toString().trim().lowercase()
        val filtered = semuaSiswa.filter { siswa ->
            val nama       = (siswa["nama_siswa"] as? String ?: "").lowercase()
            val statusGizi = (siswa["status_gizi"] as? String ?: "").lowercase().trim()
            val namaMatch  = nama.contains(query)
            val filterMatch = when (filterAktif) {
                "normal"   -> statusGizi == "normal"
                "berisiko" -> statusGizi in statusBerisiko && !statusGizi.contains("stunting")
                "stunting" -> statusGizi.contains("stunting")
                else       -> true
            }
            namaMatch && filterMatch
        }
        filteredSiswa.clear()
        filteredSiswa.addAll(filtered)
        adapter.notifyDataSetChanged()
        updateSummaryUI()
    }

    // ─── DETAIL SISWA ────────────────────────────────────────────────────────

    @SuppressLint("SetTextI18n")
    private fun showDetailSiswa(siswa: Map<String, Any?>) {
        val nama   = siswa["nama_siswa"]   as? String ?: "-"
        val kelas  = siswa["kelas"]        as? String ?: "-"
        val berat  = (siswa["berat_badan"] as? Number)?.toDouble() ?: 0.0
        val tinggi = (siswa["tinggi_badan"] as? Number)?.toDouble() ?: 0.0
        val z      = (siswa["z_score"]     as? Number)?.toDouble() ?: 0.0
        val status = siswa["status_gizi"]  as? String ?: "-"
        val usia   = (siswa["usia_bulan"]  as? Number)?.toInt() ?: 0
        val rekomendasi = FirebaseHelper.rekomendasiMakanan(status, usia)
        AlertDialog.Builder(requireContext())
            .setTitle("Detail Gizi: $nama")
            .setMessage(
                "Kelas   : $kelas\nUsia    : $usia bulan\nBerat   : $berat kg\n" +
                        "Tinggi  : $tinggi cm\nZ-Score : ${"%.2f".format(z)}\nStatus  : $status\n\n" +
                        "📋 Rekomendasi:\n" + rekomendasi.joinToString("\n")
            )
            .setPositiveButton("Tutup", null)
            .show()
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

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
            VH(LayoutInflater.from(parent.context).inflate(R.layout.item_siswa, parent, false))

        override fun getItemCount() = data.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val siswa      = data[position]
            val nama       = siswa["nama_siswa"]  as? String ?: "-"
            val kelas      = siswa["kelas"]       as? String ?: "-"
            val zScore     = (siswa["z_score"]    as? Number)?.toDouble() ?: 0.0
            val statusGizi = siswa["status_gizi"] as? String ?: "Normal"
            val inisial = nama.split(" ").take(2)
                .joinToString("") { it.firstOrNull()?.toString() ?: "" }
            holder.tvInisial.text = inisial.uppercase()
            holder.tvNama.text    = nama
            holder.tvInfo.text    = "$kelas • Z-Score: ${"%.1f".format(zScore)}"
            holder.tvStatus.text  = statusGizi
            val (bgColor, txtColor) = when (statusGizi.lowercase().trim()) {
                "normal"                          -> Pair(R.color.green_pastel, R.color.green_primary)
                "gizi kurang", "gizi buruk",
                "stunting", "stunting berat",
                "stunting - gizi kurang",
                "stunting - gizi buruk"           -> Pair(R.color.pending_bg,  R.color.pending_text)
                "berisiko gizi lebih",
                "gizi lebih", "obesitas"          -> Pair(R.color.cream_warm,  R.color.text_secondary)
                else                              -> Pair(R.color.cream_warm,  R.color.text_secondary)
            }
            holder.tvStatus.setBackgroundResource(bgColor)
            holder.tvStatus.setTextColor(requireContext().getColor(txtColor))
            holder.itemView.setOnClickListener { onClick(siswa) }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}