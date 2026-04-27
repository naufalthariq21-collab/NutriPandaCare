package com.example.nutripandacare.guru

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.nutripandacare.R
import com.example.nutripandacare.firebase.FirebaseHelper
import com.google.android.material.bottomnavigation.BottomNavigationView

class KirimNotifikasiActivity : AppCompatActivity() {

    // ─── Views ────────────────────────────────────────────────────
    // Sesuai id di activity_kirim_notifikasi.xml
    private lateinit var ivBack: ImageView
    private lateinit var etPesanNotif: EditText
    private lateinit var tvCharCount: TextView
    private lateinit var btnKirim: Button
    private lateinit var bottomNavigation: BottomNavigationView

    // ─── State ────────────────────────────────────────────────────
    private val MAX_CHAR = 300
    private var isLoading = false

    // Jika masuk dari KelolaKontenActivity, ada artikel_id sebagai konteks
    private var artikelId: String? = null

    // ═════════════════════════════════════════════════════════════
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_kirim_notifikasi)

        artikelId = intent.getStringExtra("artikel_id")

        initViews()
        setupCharCounter()
        setupClickListeners()
        setupBottomNavigation()
    }

    // ─── Bind views ───────────────────────────────────────────────
    private fun initViews() {
        ivBack           = findViewById(R.id.ivBack)
        etPesanNotif     = findViewById(R.id.etPesanNotif)
        tvCharCount      = findViewById(R.id.tvCharCount)
        btnKirim         = findViewById(R.id.btnKirim)
        bottomNavigation = findViewById(R.id.bottomNavigation)
    }

    // ─── Counter karakter real-time ───────────────────────────────
    private fun setupCharCounter() {
        etPesanNotif.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val len = s?.length ?: 0
                tvCharCount.text = "$len / $MAX_CHAR karakter"

                // Warna merah saat melebihi batas
                val warna = if (len > MAX_CHAR)
                    getColor(R.color.error_text)
                else
                    getColor(R.color.text_hint)
                tvCharCount.setTextColor(warna)
            }
        })
    }

    // ─── Listener tombol & back ───────────────────────────────────
    private fun setupClickListeners() {
        ivBack.setOnClickListener { finish() }

        btnKirim.setOnClickListener { kirimNotifikasi() }
    }

    // ─── Kirim notifikasi ke semua orang tua ─────────────────────
    /**
     * Pakai FirebaseHelper.blastNotifikasi() yang sudah ada:
     *   - query users WHERE role == "orang_tua" AND status_akun == "aktif"
     *   - tulis notifikasi via Firestore batch (1 operasi, efisien)
     */
    private fun kirimNotifikasi() {
        if (isLoading) return

        val pesan = etPesanNotif.text.toString().trim()

        // ── Validasi ──
        if (pesan.isEmpty()) {
            etPesanNotif.error = "Pesan tidak boleh kosong"
            etPesanNotif.requestFocus()
            return
        }
        if (pesan.length > MAX_CHAR) {
            Toast.makeText(this, "Pesan melebihi $MAX_CHAR karakter", Toast.LENGTH_SHORT).show()
            return
        }

        // ── Loading state ──
        isLoading = true
        btnKirim.isEnabled = false
        btnKirim.text = "Mengirim..."

        // Judul notifikasi: gunakan konteks artikel jika ada,
        // atau pakai judul generik dari guru
        val judulNotif = "Pesan dari Guru 📢"

        FirebaseHelper.blastNotifikasi(
            judul      = judulNotif,
            isi        = pesan,
            tipe       = "pengumuman",
            targetRole = "orang_tua",
            onSuccess  = {
                isLoading = false
                // Navigasi ke halaman sukses
                val intent = Intent(this, BerhasilTerkirimActivity::class.java)
                intent.putExtra("pesan", pesan)
                startActivity(intent)
                finish()
            },
            onError = { err ->
                isLoading = false
                btnKirim.isEnabled = true
                btnKirim.text = "📤  Kirim ke Orang Tua"
                Toast.makeText(this, "Gagal: $err", Toast.LENGTH_LONG).show()
            }
        )
    }

    // ─── Bottom Navigation ────────────────────────────────────────
    private fun setupBottomNavigation() {
        // Gunakan menu bottom_nav_guru (sesuai app:menu di XML)
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
                R.id.nav_konten -> {
                    startActivity(Intent(this, KelolaKontenActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }
}