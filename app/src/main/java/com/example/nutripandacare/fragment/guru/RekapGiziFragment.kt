package com.example.nutripandacare.guru

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.nutripandacare.R
import com.example.nutripandacare.firebase.FirebaseHelper
import com.google.android.material.bottomnavigation.BottomNavigationView

class RekapGiziKelasActivity : AppCompatActivity() {

    // ─── Views ───────────────────────────────────────────────────
    private lateinit var ivBack: ImageView
    private lateinit var etSearch: EditText
    private lateinit var chipSemua: TextView
    private lateinit var chipNormal: TextView
    private lateinit var chipBerisiko: TextView
    private lateinit var rvSiswa: RecyclerView
    private lateinit var bottomNavigation: BottomNavigationView

    // ─── Data ────────────────────────────────────────────────────
    // Setiap item: map data siswa dengan tambahan key "siswa_id" & "rekap_id"
    private val semuaSiswa   = mutableListOf<Map<String, Any?>>()
    private val filteredSiswa = mutableListOf<Map<String, Any?>>()
    private lateinit var adapter: SiswaAdapter

    private var filterAktif = "semua"   // "semua" | "normal" | "berisiko"

    // ═════════════════════════════════════════════════════════════
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rekap_gizi_kelas)

        initViews()
        setupRecyclerView()
        setupSearch()
        setupChips()
        setupClickListeners()
        setupBottomNavigation()
        loadSemuaSiswa()
    }

    // ─── Init ─────────────────────────────────────────────────────
    private fun initViews() {
        ivBack           = findViewById(R.id.ivBack)
        etSearch         = findViewById(R.id.etSearch)
        chipSemua        = findViewById(R.id.chipSemua)
        chipNormal       = findViewById(R.id.chipNormal)
        chipBerisiko     = findViewById(R.id.chipBerisiko)
        rvSiswa          = findViewById(R.id.rvSiswa)
        bottomNavigation = findViewById(R.id.bottomNavigation)
    }

    // ─── RecyclerView ─────────────────────────────────────────────
    private fun setupRecyclerView() {
        adapter = SiswaAdapter(filteredSiswa) { siswaData ->
            val intent = Intent(this, DetailSiswaActivity::class.java)
            intent.putExtra("rekap_id", siswaData["rekap_id"] as? String ?: "")
            intent.putExtra("siswa_id", siswaData["siswa_id"] as? String ?: "")
            intent.putExtra("nama_siswa",  siswaData["nama_siswa"]  as? String ?: "-")
            intent.putExtra("kelas",       siswaData["kelas"]       as? String ?: "-")
            intent.putExtra("berat_badan", (siswaData["berat_badan"] as? Number)?.toDouble() ?: 0.0)
            intent.putExtra("tinggi_badan",(siswaData["tinggi_badan"] as? Number)?.toDouble() ?: 0.0)
            intent.putExtra("usia_bulan",  (siswaData["usia_bulan"]  as? Number)?.toInt() ?: 0)
            intent.putExtra("z_score",     (siswaData["z_score"]     as? Number)?.toDouble() ?: 0.0)
            intent.putExtra("status_gizi", siswaData["status_gizi"] as? String ?: "-")
            startActivity(intent)
        }
        rvSiswa.layoutManager = LinearLayoutManager(this)
        rvSiswa.adapter       = adapter
    }

    // ─── Muat semua rekap gizi milik guru → flatten detail_siswa ─
    private fun loadSemuaSiswa() {
        FirebaseHelper.getRekapGizi(
            onSuccess = { rekapList ->
                semuaSiswa.clear()

                if (rekapList.isEmpty()) {
                    applyFilter()
                    return@getRekapGizi
                }

                var selesai = 0
                rekapList.forEach { (rekapId, _) ->
                    FirebaseHelper.getDetailSiswa(
                        rekapId   = rekapId,
                        onSuccess = { siswaList ->
                            siswaList.forEach { siswaData ->
                                semuaSiswa.add(siswaData + mapOf("rekap_id" to rekapId))
                            }
                            selesai++
                            if (selesai == rekapList.size) applyFilter()
                        },
                        onError = { _ ->
                            selesai++
                            if (selesai == rekapList.size) applyFilter()
                        }
                    )
                }
            },
            onError = { err ->
                Toast.makeText(this, err, Toast.LENGTH_SHORT).show()
            }
        )
    }

    // ─── Filter + Search ──────────────────────────────────────────
    private fun applyFilter() {
        val query = etSearch.text.toString().trim().lowercase()

        val filtered = semuaSiswa.filter { siswa ->
            val nama        = (siswa["nama_siswa"] as? String ?: "").lowercase()
            val statusGizi  = (siswa["status_gizi"] as? String ?: "").lowercase()
            val namaMatch   = nama.contains(query)

            val filterMatch = when (filterAktif) {
                "normal"   -> statusGizi == "normal"
                "berisiko" -> statusGizi != "normal"
                else       -> true
            }
            namaMatch && filterMatch
        }

        filteredSiswa.clear()
        filteredSiswa.addAll(filtered)
        adapter.notifyDataSetChanged()
    }

    // ─── Search ───────────────────────────────────────────────────
    private fun setupSearch() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { applyFilter() }
        })
    }

    // ─── Chips ────────────────────────────────────────────────────
    private fun setupChips() {
        val chips = listOf(chipSemua, chipNormal, chipBerisiko)

        fun setActiveChip(active: TextView, filterKey: String) {
            chips.forEach { chip ->
                chip.setBackgroundColor(resources.getColor(R.color.white, theme))
                chip.setTextColor(resources.getColor(R.color.text_secondary, theme))
            }
            active.setBackgroundColor(resources.getColor(R.color.green_primary, theme))
            active.setTextColor(resources.getColor(R.color.white, theme))
            filterAktif = filterKey
            applyFilter()
        }

        chipSemua.setOnClickListener    { setActiveChip(chipSemua, "semua") }
        chipNormal.setOnClickListener   { setActiveChip(chipNormal, "normal") }
        chipBerisiko.setOnClickListener { setActiveChip(chipBerisiko, "berisiko") }
    }

    // ─── Click Listeners ──────────────────────────────────────────
    private fun setupClickListeners() {
        ivBack.setOnClickListener { finish() }
    }

    // ─── Bottom Navigation ────────────────────────────────────────
    private fun setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home    -> { finish(); true }
                R.id.nav_laporan -> { startActivity(Intent(this, LaporanMbgActivity::class.java)); true }
                R.id.nav_konten  -> { startActivity(Intent(this, KelolaKontenActivity::class.java)); true }
                else             -> false
            }
        }
    }

    // ═════════════════════════════════════════════════════════════
    // Inner RecyclerView Adapter — item_siswa.xml
    // ═════════════════════════════════════════════════════════════
    inner class SiswaAdapter(
        private val data: List<Map<String, Any?>>,
        private val onClick: (Map<String, Any?>) -> Unit
    ) : RecyclerView.Adapter<SiswaAdapter.VH>() {

        inner class VH(view: View) : RecyclerView.ViewHolder(view) {
            val tvInisial: TextView    = view.findViewById(R.id.tvInisialSiswa)
            val tvNama: TextView       = view.findViewById(R.id.tvNamaSiswa)
            val tvInfo: TextView       = view.findViewById(R.id.tvInfoSiswa)
            val tvStatus: TextView     = view.findViewById(R.id.tvStatusGiziSiswa)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_siswa, parent, false)
            return VH(view)
        }

        override fun getItemCount() = data.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val siswa      = data[position]
            val nama       = siswa["nama_siswa"] as? String ?: "-"
            val kelas      = siswa["kelas"]      as? String ?: "-"
            val zScore     = (siswa["z_score"]   as? Number)?.toDouble() ?: 0.0
            val statusGizi = siswa["status_gizi"] as? String ?: "Normal"

            // Inisial
            val inisial = nama.split(" ")
                .take(2).joinToString("") { it.firstOrNull()?.toString() ?: "" }
            holder.tvInisial.text = inisial.uppercase()
            holder.tvNama.text    = nama
            holder.tvInfo.text    = "$kelas • Z-Score: ${"%.1f".format(zScore)}"
            holder.tvStatus.text  = statusGizi

            // Warna status
            val (bgColor, txtColor) = when (statusGizi) {
                "Normal"     -> Pair(R.color.green_pastel, R.color.green_primary)
                "Gizi Kurang",
                "Gizi Buruk" -> Pair(R.color.error_bg, R.color.error_text)
                else         -> Pair(R.color.cream_warm, R.color.text_secondary)
            }
            holder.tvStatus.setBackgroundColor(resources.getColor(bgColor, theme))
            holder.tvStatus.setTextColor(resources.getColor(txtColor, theme))

            holder.itemView.setOnClickListener { onClick(siswa) }
        }
    }
}