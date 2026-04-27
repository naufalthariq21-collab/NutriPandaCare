package com.example.nutripandacare.guru

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.nutripandacare.R
import com.example.nutripandacare.firebase.FirebaseHelper
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale

class KelolaKontenActivity : AppCompatActivity() {

    // ─── Views ────────────────────────────────────────────────────
    // Sesuai id di activity_kelola_konten.xml
    private lateinit var ivBack: ImageView
    private lateinit var btnTambah: MaterialButton
    private lateinit var rvKonten: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var bottomNavigation: BottomNavigationView

    // ─── Data & Adapter ───────────────────────────────────────────
    private val daftarArtikel = mutableListOf<Pair<String, Map<String, Any?>>>()
    private lateinit var adapter: KontenAdapter

    // ═════════════════════════════════════════════════════════════
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_kelola_konten)

        initViews()
        setupRecyclerView()
        setupClickListeners()
        setupBottomNavigation()
        loadArtikel()
    }

    // Refresh setiap kali kembali dari BuatKontenActivity
    override fun onResume() {
        super.onResume()
        loadArtikel()
    }

    // ─── Bind views ───────────────────────────────────────────────
    private fun initViews() {
        ivBack           = findViewById(R.id.ivBack)
        btnTambah        = findViewById(R.id.btnTambah)
        bottomNavigation = findViewById(R.id.bottomNavigation)

        // rvKonten, tvEmpty, progressBar dideklarasikan di dalam
        // NestedScrollView → LinearLayout pada XML asli pakai card statis.
        // Kita ganti LinearLayout isi dengan RecyclerView secara programmatic.
        // Pastikan di activity_kelola_konten.xml sudah ada:
        //   <RecyclerView android:id="@+id/rvKonten" ... />
        //   <TextView android:id="@+id/tvEmpty" ... />
        //   <ProgressBar android:id="@+id/progressBar" ... />
        rvKonten     = findViewById(R.id.rvKonten)
        tvEmpty      = findViewById(R.id.tvEmpty)
        progressBar  = findViewById(R.id.progressBar)
    }

    // ─── RecyclerView setup ───────────────────────────────────────
    private fun setupRecyclerView() {
        adapter = KontenAdapter(
            data      = daftarArtikel,
            onKirimNotif = { artikelId, judulArtikel ->
                // Navigasi ke KirimNotifikasiActivity dengan konteks artikel
                val intent = Intent(this, KirimNotifikasiActivity::class.java)
                intent.putExtra("artikel_id",    artikelId)
                intent.putExtra("judul_artikel", judulArtikel)
                startActivity(intent)
            },
            onHapus = { artikelId ->
                konfirmasiHapus(artikelId)
            }
        )
        rvKonten.layoutManager = LinearLayoutManager(this)
        rvKonten.adapter       = adapter
    }

    // ─── Load artikel dari Firestore ──────────────────────────────
    private fun loadArtikel() {
        progressBar.visibility = View.VISIBLE
        rvKonten.visibility    = View.GONE
        tvEmpty.visibility     = View.GONE

        // getArtikel() di FirebaseHelper support filter kategori;
        // kita pakai "Semua" untuk tampilkan semua
        FirebaseHelper.getArtikel(
            kategori  = "Semua",
            onSuccess = { list ->
                progressBar.visibility = View.GONE

                daftarArtikel.clear()
                daftarArtikel.addAll(list)
                adapter.notifyDataSetChanged()

                if (list.isEmpty()) {
                    tvEmpty.visibility  = View.VISIBLE
                    rvKonten.visibility = View.GONE
                } else {
                    tvEmpty.visibility  = View.GONE
                    rvKonten.visibility = View.VISIBLE
                }
            },
            onError = { err ->
                progressBar.visibility = View.GONE
                Toast.makeText(this, "Gagal memuat konten: $err", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // ─── Konfirmasi hapus artikel ─────────────────────────────────
    private fun konfirmasiHapus(artikelId: String) {
        AlertDialog.Builder(this)
            .setTitle("Hapus Konten")
            .setMessage("Yakin ingin menghapus artikel ini? Tindakan ini tidak bisa dibatalkan.")
            .setPositiveButton("Hapus") { _, _ ->
                FirebaseHelper.hapusArtikel(
                    artikelId = artikelId,
                    onSuccess = {
                        Toast.makeText(this, "Artikel berhasil dihapus", Toast.LENGTH_SHORT).show()
                        loadArtikel()
                    },
                    onError = { err ->
                        Toast.makeText(this, "Gagal hapus: $err", Toast.LENGTH_SHORT).show()
                    }
                )
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    // ─── Click Listeners ──────────────────────────────────────────
    private fun setupClickListeners() {
        ivBack.setOnClickListener { finish() }

        btnTambah.setOnClickListener {
            startActivity(Intent(this, BuatKontenActivity::class.java))
        }
    }

    // ─── Bottom Navigation ────────────────────────────────────────
    private fun setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    val intent = Intent(this, DashboardGuruActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    startActivity(intent)
                    true
                }
                R.id.nav_laporan -> {
                    startActivity(Intent(this, LaporanMbgActivity::class.java))
                    true
                }
                R.id.nav_konten -> true  // sudah di sini
                else -> false
            }
        }
        bottomNavigation.selectedItemId = R.id.nav_konten
    }


    // ═════════════════════════════════════════════════════════════
    // Inner RecyclerView Adapter
    // Layout tiap item: item_konten.xml
    // ═════════════════════════════════════════════════════════════
    inner class KontenAdapter(
        private val data: List<Pair<String, Map<String, Any?>>>,
        private val onKirimNotif: (artikelId: String, judulArtikel: String) -> Unit,
        private val onHapus: (artikelId: String) -> Unit
    ) : RecyclerView.Adapter<KontenAdapter.VH>() {

        private val sdf = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))

        inner class VH(view: View) : RecyclerView.ViewHolder(view) {
            val ivThumbnail : ImageView = view.findViewById(R.id.ivThumbnailKonten)
            val tvJudul     : TextView  = view.findViewById(R.id.tvJudulKonten)
            val tvKategori  : TextView  = view.findViewById(R.id.tvKategoriKonten)
            val tvWaktuMenit: TextView  = view.findViewById(R.id.tvWaktuMenitKonten)
            val tvMenu      : TextView  = view.findViewById(R.id.tvMenuKonten)  // tombol "···"
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_konten, parent, false)
            return VH(view)
        }

        override fun getItemCount() = data.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val (artikelId, item) = data[position]

            // ── Judul ──
            holder.tvJudul.text = item["judul"] as? String ?: "-"

            // ── Kategori ──
            holder.tvKategori.text = item["kategori"] as? String ?: "-"

            // ── Waktu publish + menit baca ──
            val tsPublish  = item["waktu_publish"] as? Timestamp
            val tanggal    = tsPublish?.toDate()?.let { sdf.format(it) } ?: "-"
            val menitBaca  = (item["menit_baca"] as? Number)?.toInt() ?: 0
            holder.tvWaktuMenit.text = "$tanggal • $menitBaca menit"

            // ── Thumbnail via Glide ──
            val thumbUrl = item["thumbnail_url"] as? String ?: ""
            if (thumbUrl.isNotEmpty()) {
                Glide.with(holder.itemView.context)
                    .load(thumbUrl)
                    .placeholder(R.color.green_pastel)
                    .error(R.color.cream_medium)
                    .centerCrop()
                    .into(holder.ivThumbnail)
            } else {
                holder.ivThumbnail.setBackgroundColor(
                    resources.getColor(R.color.green_pastel, theme)
                )
                holder.ivThumbnail.setImageDrawable(null)
            }

            // ── Popup menu "···" ──
            holder.tvMenu.setOnClickListener { anchor ->
                val popup = PopupMenu(anchor.context, anchor)
                popup.menu.add(0, 0, 0, "📤  Kirim Notifikasi ke Ortu")
                popup.menu.add(0, 1, 1, "🗑️  Hapus Artikel")
                popup.setOnMenuItemClickListener { menuItem ->
                    when (menuItem.itemId) {
                        0 -> {
                            val judul = item["judul"] as? String ?: ""
                            onKirimNotif(artikelId, judul)
                            true
                        }
                        1 -> {
                            onHapus(artikelId)
                            true
                        }
                        else -> false
                    }
                }
                popup.show()
            }
        }
    }
}