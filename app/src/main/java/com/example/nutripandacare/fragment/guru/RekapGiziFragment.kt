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
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.nutripandacare.R
import com.example.nutripandacare.databinding.FragmentRekapGiziBinding
import com.example.nutripandacare.firebase.FirebaseHelper
import com.google.android.material.textfield.TextInputEditText
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

    private fun setupRecyclerView() {
        adapter = SiswaAdapter(filteredSiswa) { showDetailSiswa(it) }
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
        binding.chipResiko.setOnClickListener   { filterAktif = "berisiko"; applyFilter() }
        binding.chipStunting.setOnClickListener { filterAktif = "stunting"; applyFilter() }
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }
        binding.btnTambahSiswa.setOnClickListener {
            rekapId?.let { showTambahSiswaDialog(it) } ?: Toast.makeText(requireContext(), "Memuat data rekap...", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadAtauBuatRekap() {
        binding.progressBar.visibility = View.VISIBLE
        FirebaseHelper.getRekapGizi(
            onSuccess = { list ->
                if (_binding == null) return@getRekapGizi
                if (list.isNotEmpty()) {
                    rekapId = list[0].first
                    loadDetailSiswa(rekapId!!)
                } else {
                    buatRekapBaru()
                }
            },
            onError = {
                if (_binding == null) return@getRekapGizi
                binding.progressBar.visibility = View.GONE
                Toast.makeText(requireContext(), "Gagal memuat: $it", Toast.LENGTH_SHORT).show()
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
        // totalSiswa, normal, giziKurang, giziBuruk, giziLebih, obesitas, stunting
        FirebaseHelper.simpanRekapGizi(sekolah, periode, 0, 0, 0, 0, 0, 0, 0,
            onSuccess = { id ->
                rekapId = id
                binding.progressBar.visibility = View.GONE
                updateSummaryUI()
            },
            onError = { 
                binding.progressBar.visibility = View.GONE
                Toast.makeText(requireContext(), "Gagal membuat rekap baru", Toast.LENGTH_SHORT).show()
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
            },
            onError = {
                if (_binding == null) return@getDetailSiswa
                binding.progressBar.visibility = View.GONE
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun showTambahSiswaDialog(idRekap: String) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_tambah_siswa, null)
        val etNama    = dialogView.findViewById<TextInputEditText>(R.id.etNamaSiswa)
        val etKelas   = dialogView.findViewById<TextInputEditText>(R.id.etKelasSiswa)
        val etBerat   = dialogView.findViewById<TextInputEditText>(R.id.etBeratSiswa)
        val etTinggi  = dialogView.findViewById<TextInputEditText>(R.id.etTinggiSiswa)
        val etUsia    = dialogView.findViewById<TextInputEditText>(R.id.etUsiaBulanSiswa)
        val btnHitung = dialogView.findViewById<Button>(R.id.btnHitungSiswa)
        val tvHasil   = dialogView.findViewById<TextView>(R.id.tvHasilGiziSiswa)

        var currentStatus = ""
        var currentZScore = 0.0

        btnHitung.setOnClickListener {
            val berat  = etBerat.text.toString().toDoubleOrNull() ?: 0.0
            val tinggi = etTinggi.text.toString().toDoubleOrNull() ?: 0.0
            val usia   = etUsia.text.toString().toIntOrNull() ?: 0
            if (berat <= 0 || tinggi <= 0 || usia <= 0) {
                Toast.makeText(requireContext(), "Lengkapi Berat, Tinggi, dan Usia!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            currentZScore = FirebaseHelper.hitungZScore(berat, usia)
            val zTbu      = FirebaseHelper.hitungZScoreTbu(tinggi, usia)
            currentStatus = FirebaseHelper.tentukanStatusGizi(currentZScore, zTbu)

            tvHasil.visibility = View.VISIBLE
            tvHasil.text = "Hasil: $currentStatus\nZ-Score BB/U: ${"%.2f".format(currentZScore)} | TB/U: ${"%.2f".format(zTbu)}"
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

                if (nama.isEmpty() || kelas.isEmpty() || berat <= 0 || usia <= 0) {
                    Toast.makeText(requireContext(), "Harap isi semua kolom wajib!", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (currentStatus.isEmpty()) {
                    currentZScore = FirebaseHelper.hitungZScore(berat, usia)
                    currentStatus = FirebaseHelper.tentukanStatusGizi(currentZScore, FirebaseHelper.hitungZScoreTbu(tinggi, usia))
                }

                FirebaseHelper.tambahDetailSiswa(idRekap, nama, kelas, berat, tinggi, usia, currentZScore, currentStatus,
                    onSuccess = {
                        Toast.makeText(requireContext(), "$nama berhasil ditambahkan ✅", Toast.LENGTH_SHORT).show()
                        loadDetailSiswa(idRekap) // Refresh list
                    },
                    onError = { Toast.makeText(requireContext(), "Gagal simpan: $it", Toast.LENGTH_SHORT).show() }
                )
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    @SuppressLint("SetTextI18n")
    private fun applyFilter() {
        if (_binding == null) return
        val query = binding.etCariSiswa.text.toString().trim().lowercase()
        val filtered = semuaSiswa.filter { s ->
            val nama   = (s["nama_siswa"] as? String ?: "").lowercase()
            val status = (s["status_gizi"] as? String ?: "").lowercase().trim()
            val matchQuery  = nama.contains(query)
            val matchFilter = when (filterAktif) {
                "normal"   -> status == "normal"
                "berisiko" -> status in statusBerisiko && !status.contains("stunting")
                "stunting" -> status.contains("stunting")
                else       -> true
            }
            matchQuery && matchFilter
        }
        filteredSiswa.clear()
        filteredSiswa.addAll(filtered)
        adapter.notifyDataSetChanged()
        updateSummaryUI()
    }

    @SuppressLint("SetTextI18n")
    private fun updateSummaryUI() {
        if (_binding == null) return
        val total    = semuaSiswa.size
        val normal   = semuaSiswa.count { (it["status_gizi"] as? String ?: "").lowercase() == "normal" }
        val berisiko = semuaSiswa.count {
            val s = (it["status_gizi"] as? String ?: "").lowercase().trim()
            s in statusBerisiko && !s.contains("stunting")
        }
        val stunting = semuaSiswa.count { (it["status_gizi"] as? String ?: "").lowercase().contains("stunting") }

        binding.tvSubHeaderSiswa.text = "$total Siswa Terdaftar"
        binding.tvStatNormal.text     = normal.toString()
        binding.tvStatResiko.text     = berisiko.toString()
        binding.tvStatStunting.text   = stunting.toString()
    }

    @SuppressLint("SetTextI18n")
    private fun showDetailSiswa(siswa: Map<String, Any?>) {
        val status = siswa["status_gizi"] as? String ?: "-"
        val usia   = (siswa["usia_bulan"] as? Number)?.toInt() ?: 0
        AlertDialog.Builder(requireContext())
            .setTitle(siswa["nama_siswa"] as? String ?: "Detail Siswa")
            .setMessage("Kelas: ${siswa["kelas"]}\nUsia: $usia bulan\nBerat: ${siswa["berat_badan"]} kg\nTinggi: ${siswa["tinggi_badan"]} cm\nStatus: $status\n\n" +
                    "Rekomendasi:\n${FirebaseHelper.rekomendasiMakanan(status, usia).joinToString("\n")}")
            .setPositiveButton("Tutup", null)
            .show()
    }

    private class SiswaAdapter(
        private val list: List<Map<String, Any?>>,
        private val onClick: (Map<String, Any?>) -> Unit
    ) : RecyclerView.Adapter<SiswaAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvInisial: TextView = view.findViewById(R.id.tvInisialSiswa)
            val tvNama: TextView = view.findViewById(R.id.tvNamaSiswa)
            val tvKet: TextView  = view.findViewById(R.id.tvInfoSiswa)
            val tvStatus: TextView = view.findViewById(R.id.tvStatusGiziSiswa)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_siswa, parent, false)
            return ViewHolder(v)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val s = list[position]
            val nama = s["nama_siswa"] as? String ?: "-"
            holder.tvNama.text = nama
            holder.tvKet.text  = "Kelas ${s["kelas"]} • ${s["usia_bulan"]} Bln • ${s["berat_badan"]} Kg"
            
            holder.tvInisial.text = if (nama.length >= 2) nama.substring(0, 2).uppercase() else nama.take(1).uppercase()

            val status = (s["status_gizi"] as? String ?: "Normal").trim()
            holder.tvStatus.text = status
            
            val color = when {
                status.lowercase() == "normal" -> R.color.green_primary
                status.lowercase().contains("stunting") -> R.color.badge_red
                else -> R.color.pending_text
            }
            holder.tvStatus.setTextColor(ContextCompat.getColor(holder.itemView.context, color))
            
            holder.itemView.setOnClickListener { onClick(s) }
        }

        override fun getItemCount() = list.size
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
