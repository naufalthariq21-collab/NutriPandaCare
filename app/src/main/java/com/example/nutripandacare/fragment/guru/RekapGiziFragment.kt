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

class RekapGiziFragment : Fragment() {

    private var _binding: FragmentRekapGiziBinding? = null
    private val binding get() = _binding!!

    private val semuaSiswa    = mutableListOf<Map<String, Any?>>()
    private val filteredSiswa = mutableListOf<Map<String, Any?>>()
    private lateinit var adapter: SiswaAdapter

    private var filterAktif = "semua"
    private var rekapId: String = ""
    private var namaKelas: String = "Rekap Gizi"

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
        loadDataRekap()
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
            if (rekapId.isEmpty()) {
                showSetNamaKelasDialog()
            } else {
                showTambahSiswaDialog()
            }
        }
        binding.tvNamaKelas.setOnClickListener {
            showSetNamaKelasDialog()
        }
    }

    private fun loadDataRekap() {
        FirebaseHelper.getRekapGizi(
            onSuccess = { rekapList ->
                if (_binding == null) return@getRekapGizi
                if (rekapList.isNotEmpty()) {
                    val (id, data) = rekapList.first()
                    rekapId = id
                    namaKelas = data["periode"] as? String ?: "Kelas"
                    binding.tvNamaKelas.text = namaKelas
                    loadDaftarSiswa(rekapId)
                } else {
                    binding.tvNamaKelas.text = "Klik untuk Atur Kelas"
                    binding.tvSubHeaderSiswa.text = "0 Siswa Terdaftar"
                    binding.tvEmpty.visibility = View.VISIBLE
                }
            },
            onError = { binding.tvEmpty.visibility = View.VISIBLE }
        )
    }

    private fun loadDaftarSiswa(rId: String) {
        FirebaseHelper.getDetailSiswa(rId,
            onSuccess = { list ->
                if (_binding == null) return@getDetailSiswa
                semuaSiswa.clear()
                semuaSiswa.addAll(list.map { it + mapOf("rekap_id" to rId) })
                applyFilter()
                updateSummaryStats()
            },
            onError = { }
        )
    }

    private fun applyFilter() {
        if (_binding == null) return
        val query = binding.etCariSiswa.text.toString().trim().lowercase()
        val filtered = semuaSiswa.filter { siswa ->
            val nama = (siswa["nama_siswa"] as? String ?: "").lowercase()
            val status = (siswa["status_gizi"] as? String ?: "").lowercase()
            val matchesSearch = nama.contains(query)
            val matchesChip = when(filterAktif) {
                "normal" -> status == "normal"
                "stunting" -> status.contains("stunting")
                "berisiko" -> status != "normal"
                else -> true
            }
            matchesSearch && matchesChip
        }
        filteredSiswa.clear()
        filteredSiswa.addAll(filtered)
        adapter.notifyDataSetChanged()
        binding.tvEmpty.visibility = if (filteredSiswa.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun updateSummaryStats() {
        if (_binding == null) return
        val total = semuaSiswa.size
        val normal = semuaSiswa.count { (it["status_gizi"] as? String ?: "").lowercase() == "normal" }
        val stunting = semuaSiswa.count { (it["status_gizi"] as? String ?: "").lowercase().contains("stunting") }
        val resiko = total - normal

        binding.tvSubHeaderSiswa.text = "$total Siswa Terdaftar"
        binding.tvStatNormal.text = normal.toString()
        binding.tvStatStunting.text = stunting.toString()
        binding.tvStatResiko.text = resiko.toString()
        
        binding.tvTotalSiswa.text = "Total: $total siswa"
        binding.tvNormalCount.text = "Normal: $normal"
        binding.tvResikoCount.text = "Berisiko: $resiko"
        
        // Update aggregate ke Firestore
        if (rekapId.isNotEmpty()) {
            FirebaseHelper.db.collection("rekap_gizi").document(rekapId)
                .update(mapOf(
                    "total_siswa" to total,
                    "normal" to normal,
                    "stunting" to stunting,
                    "gizi_kurang" to resiko // Disederhanakan untuk dashboard
                ))
        }
    }

    private fun showSetNamaKelasDialog() {
        val et = EditText(requireContext())
        et.hint = "Contoh: Kelas 5A"
        if (namaKelas != "Rekap Gizi") et.setText(namaKelas)

        AlertDialog.Builder(requireContext())
            .setTitle("Nama Kelas / Periode")
            .setView(et)
            .setPositiveButton("Simpan") { _, _ ->
                val input = et.text.toString().trim()
                if (input.isNotEmpty()) {
                    simpanAtauUpdateRekap(input)
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun simpanAtauUpdateRekap(nama: String) {
        if (rekapId.isEmpty()) {
            FirebaseHelper.simpanRekapGizi("Sekolah", nama, 0, 0, 0, 0, 0, 0,
                onSuccess = { id ->
                    rekapId = id
                    namaKelas = nama
                    binding.tvNamaKelas.text = nama
                    Toast.makeText(requireContext(), "Kelas berhasil dibuat", Toast.LENGTH_SHORT).show()
                },
                onError = { Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show() }
            )
        } else {
            FirebaseHelper.db.collection("rekap_gizi").document(rekapId)
                .update("periode", nama)
                .addOnSuccessListener {
                    namaKelas = nama
                    binding.tvNamaKelas.text = nama
                }
        }
    }

    private fun showTambahSiswaDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_tambah_siswa, null)
        val etNama = dialogView.findViewById<EditText>(R.id.etNamaSiswa)
        val etBerat = dialogView.findViewById<EditText>(R.id.etBeratSiswa)
        val etTinggi = dialogView.findViewById<EditText>(R.id.etTinggiSiswa)
        val etUsia = dialogView.findViewById<EditText>(R.id.etUsiaBulanSiswa)

        AlertDialog.Builder(requireContext())
            .setTitle("Tambah Siswa")
            .setView(dialogView)
            .setPositiveButton("Tambah") { _, _ ->
                val nama = etNama.text.toString().trim()
                val bb = etBerat.text.toString().toDoubleOrNull() ?: 0.0
                val tb = etTinggi.text.toString().toDoubleOrNull() ?: 0.0
                val usia = etUsia.text.toString().toIntOrNull() ?: 0
                
                if (nama.isNotEmpty()) {
                    val zScore = FirebaseHelper.hitungZScore(bb, usia)
                    val status = FirebaseHelper.kategoriGizi(zScore)
                    
                    FirebaseHelper.tambahDetailSiswa(rekapId, nama, namaKelas, bb, tb, usia, zScore, status,
                        onSuccess = {
                            loadDaftarSiswa(rekapId)
                            Toast.makeText(requireContext(), "$nama ditambahkan", Toast.LENGTH_SHORT).show()
                        },
                        onError = { Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show() }
                    )
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun showDetailDialog(siswa: Map<String, Any?>) {
        val nama = siswa["nama_siswa"] as? String ?: ""
        val status = siswa["status_gizi"] as? String ?: ""
        val bb = (siswa["berat_badan"] as? Number)?.toDouble() ?: 0.0
        val tb = (siswa["tinggi_badan"] as? Number)?.toDouble() ?: 0.0
        val usia = (siswa["usia_bulan"] as? Number)?.toInt() ?: 0
        
        AlertDialog.Builder(requireContext())
            .setTitle("Profil Siswa: $nama")
            .setMessage("Usia: $usia bulan\nBB: $bb kg\nTB: $tb cm\nStatus: $status")
            .setPositiveButton("Tutup", null)
            .show()
    }

    inner class SiswaAdapter(
        private val data: List<Map<String, Any?>>,
        private val onClick: (Map<String, Any?>) -> Unit
    ) : RecyclerView.Adapter<SiswaAdapter.VH>() {
        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val tvInisial: TextView = v.findViewById(R.id.tvInisialSiswa)
            val tvNama: TextView = v.findViewById(R.id.tvNamaSiswa)
            val tvInfo: TextView = v.findViewById(R.id.tvInfoSiswa)
            val tvStatus: TextView = v.findViewById(R.id.tvStatusGiziSiswa)
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_siswa, parent, false)
            return VH(v)
        }
        override fun getItemCount() = data.size
        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = data[position]
            val nama = item["nama_siswa"] as? String ?: ""
            holder.tvNama.text = nama
            holder.tvInisial.text = nama.take(1).uppercase()
            holder.tvInfo.text = "BB: ${item["berat_badan"]}kg • TB: ${item["tinggi_badan"]}cm"
            holder.tvStatus.text = item["status_gizi"] as? String ?: "Normal"
            holder.itemView.setOnClickListener { onClick(item) }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}