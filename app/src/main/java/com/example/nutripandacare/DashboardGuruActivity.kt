package com.example.nutripandacare.guru

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.nutripandacare.R
import com.example.nutripandacare.firebase.FirebaseHelper
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.card.MaterialCardView

class DashboardGuruActivity : AppCompatActivity() {

    // ─── Views ───────────────────────────────────────────────────
    private lateinit var tvWelcome: TextView
    private lateinit var ivBell: ImageView
    private lateinit var tvStudentsPresent: TextView

    // Quick-action cards (id sesuai activity_dashboard.xml)
    private lateinit var cardKirimNotifikasi: MaterialCardView   // Frame 6
    private lateinit var cardRekapGizi: MaterialCardView         // Frame 8

    private lateinit var bottomNavigation: BottomNavigationView

    // ─── State ───────────────────────────────────────────────────
    private var namaGuru = ""

    // ═════════════════════════════════════════════════════════════
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        initViews()
        setupClickListeners()
        setupBottomNavigation()
        loadDataGuru()
    }

    // ─── Init ─────────────────────────────────────────────────────
    private fun initViews() {
        tvWelcome         = findViewById(R.id.tvWelcome)
        ivBell            = findViewById(R.id.ivBell)
        tvStudentsPresent = findViewById(R.id.tvStudentsPresent)
        cardKirimNotifikasi = findViewById(R.id.cardKirimNotifikasi)
        cardRekapGizi       = findViewById(R.id.cardRekapGizi)
        bottomNavigation    = findViewById(R.id.bottomNavigation)
    }

    // ─── Load profil guru dari Firestore ──────────────────────────
    private fun loadDataGuru() {
        FirebaseHelper.getDataUser(
            uid       = FirebaseHelper.uid,
            onSuccess = { data ->
                namaGuru = data["nama"] as? String ?: "Guru"
                tvWelcome.text = "Welcome, $namaGuru 👋"
            },
            onError   = { /* biarkan default text */ }
        )
    }

    // ─── Click Listeners ──────────────────────────────────────────
    private fun setupClickListeners() {
        // Bell → (opsional: NotifikasiActivity)
        ivBell.setOnClickListener {
            // TODO: buka daftar notifikasi jika sudah dibuat
        }

        // Kirim Notifikasi Card → Frame 6
        cardKirimNotifikasi.setOnClickListener {
            startActivity(Intent(this, KirimNotifikasiActivity::class.java))
        }

        // Rekap Gizi Card → Frame 8
        cardRekapGizi.setOnClickListener {
            startActivity(Intent(this, RekapGiziKelasActivity::class.java))
        }
    }

    // ─── Bottom Navigation ────────────────────────────────────────
    private fun setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home      -> true   // sudah di sini
                R.id.nav_laporan   -> {
                    startActivity(Intent(this, LaporanMbgActivity::class.java))
                    true
                }
                R.id.nav_konten    -> {
                    startActivity(Intent(this, KelolaKontenActivity::class.java))
                    true
                }
                else -> false
            }
        }
        bottomNavigation.selectedItemId = R.id.nav_home
    }
}