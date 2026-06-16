package com.example.nutripandacare

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.example.nutripandacare.databinding.ActivitySuccessBinding
import com.example.nutripandacare.firebase.FirebaseHelper

class SuccessActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySuccessBinding

    // GAP-4 FIX: simpan reference ke Handler dan Runnable agar bisa di-cancel
    // di onDestroy — mencegah memory leak & crash jika Activity sudah finish
    // sebelum 5 detik (misal user tap "Mulai Sekarang" duluan). UAT #31 terpenuhi.
    private val autoRedirectHandler = Handler(Looper.getMainLooper())
    private val autoRedirectRunnable = Runnable {
        if (!isFinishing && !isDestroyed) {
            val role = intent.getStringExtra("ROLE") ?: ""
            navigasiKeDashboard(role)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySuccessBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val role  = intent.getStringExtra("ROLE")  ?: ""
        val nama  = intent.getStringExtra("NAMA")  ?: ""
        val email = intent.getStringExtra("EMAIL") ?: ""
        val noHp  = intent.getStringExtra("NO_HP") ?: ""

        tampilkanDataUser(nama, email, noHp, role)
        setupTombolMulai(role)
        setupTombolKembaliLogin()

        // UAT #31: Auto-redirect ke dashboard setelah 5 detik
        autoRedirectHandler.postDelayed(autoRedirectRunnable, 5000)
    }

    // GAP-4 FIX: cancel Handler saat Activity di-destroy agar tidak crash
    override fun onDestroy() {
        autoRedirectHandler.removeCallbacks(autoRedirectRunnable)
        super.onDestroy()
    }

    // ─────────────────────────────────────────────
    // TAMPILKAN DATA USER DI KARTU
    // XML id: tvUserName, tvUserRole, tvUserEmail, tvUserPhone
    // ─────────────────────────────────────────────
    private fun tampilkanDataUser(nama: String, email: String, noHp: String, role: String) {
        if (nama.isNotEmpty())  binding.tvUserName.text  = nama
        if (email.isNotEmpty()) binding.tvUserEmail.text = email
        if (noHp.isNotEmpty())  binding.tvUserPhone.text = noHp

        binding.tvUserRole.text = roleTampil(role)

        // Fallback ke Firestore jika data tidak dikirim lewat Intent
        if (nama.isEmpty() || email.isEmpty()) {
            val uid = FirebaseHelper.auth.currentUser?.uid ?: return
            FirebaseHelper.getDataUser(
                uid       = uid,
                onSuccess = { data ->
                    binding.tvUserName.text  = data["nama"]?.toString()  ?: "-"
                    binding.tvUserEmail.text = data["email"]?.toString() ?: "-"
                    binding.tvUserPhone.text = data["no_hp"]?.toString() ?: "-"
                    binding.tvUserRole.text  = roleTampil(data["role"]?.toString() ?: "")
                },
                onError = { /* biarkan data kosong */ }
            )
        }
    }

    private fun roleTampil(role: String) = when (role) {
        "orang_tua" -> "Orang Tua / Siswa"
        "guru"      -> "Guru"
        "pengelola" -> "Pengelola MBG"
        else        -> "-"
    }

    // ─────────────────────────────────────────────
    // TOMBOL MULAI SEKARANG  (UAT #30)
    // ─────────────────────────────────────────────
    private fun setupTombolMulai(role: String) {
        binding.btnMulai.setOnClickListener {
            // Cancel auto-redirect agar tidak dobel navigate
            autoRedirectHandler.removeCallbacks(autoRedirectRunnable)
            navigasiKeDashboard(role)
        }
    }

    // ─────────────────────────────────────────────
    // TOMBOL KEMBALI KE LOGIN  (UAT #32)
    // ─────────────────────────────────────────────
    private fun setupTombolKembaliLogin() {
        binding.btnKembaliLogin.setOnClickListener {
            autoRedirectHandler.removeCallbacks(autoRedirectRunnable)
            FirebaseHelper.logout()
            startActivity(
                Intent(this, LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
            )
            finish()
        }
    }

    // ─────────────────────────────────────────────
    // NAVIGASI KE DASHBOARD SESUAI ROLE
    // ─────────────────────────────────────────────
    private fun navigasiKeDashboard(role: String) {
        if (role.isNotEmpty()) {
            val dest = when (role) {
                "orang_tua" -> DashboardOrangTuaActivity::class.java
                "guru"      -> DashboardGuruActivity::class.java
                "pengelola" -> DashboardPengelolaActivity::class.java
                else        -> LoginActivity::class.java
            }
            startActivity(Intent(this, dest).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()
            return
        }

        // Fallback: ambil role dari Firestore
        val uid = FirebaseHelper.auth.currentUser?.uid ?: run {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        FirebaseHelper.getRole(
            uid       = uid,
            onSuccess = { roleDb ->
                val dest = when (roleDb) {
                    "orang_tua" -> DashboardOrangTuaActivity::class.java
                    "guru"      -> DashboardGuruActivity::class.java
                    "pengelola" -> DashboardPengelolaActivity::class.java
                    else        -> LoginActivity::class.java
                }
                startActivity(Intent(this, dest).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                })
                finish()
            },
            onError = {
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
        )
    }
}
