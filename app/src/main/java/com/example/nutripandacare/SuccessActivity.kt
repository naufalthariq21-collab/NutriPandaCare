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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySuccessBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Ambil data dari intent (dikirim dari WaitingVerificationActivity)
        val role  = intent.getStringExtra("ROLE")  ?: ""
        val nama  = intent.getStringExtra("NAMA")  ?: ""
        val email = intent.getStringExtra("EMAIL") ?: ""
        val noHp  = intent.getStringExtra("NO_HP") ?: ""

        tampilkanDataUser(nama, email, noHp, role)
        setupTombolMulai(role)
        setupTombolKembaliLogin()

        // Auto-redirect ke dashboard setelah 5 detik
        Handler(Looper.getMainLooper()).postDelayed({
            // Cek apakah activity masih aktif sebelum redirect
            if (!isFinishing) {
                navigasiKeDashboard(role)
            }
        }, 5000)
    }

    // ─────────────────────────────────────────────
    // TAMPILKAN DATA USER DI KARTU
    // XML id: tvUserName, tvUserRole, tvUserEmail, tvUserPhone
    // ─────────────────────────────────────────────
    private fun tampilkanDataUser(nama: String, email: String, noHp: String, role: String) {
        // Jika data ada dari intent, tampilkan langsung
        if (nama.isNotEmpty()) {
            binding.tvUserName.text = nama
        }
        if (email.isNotEmpty()) {
            binding.tvUserEmail.text = email
        }
        if (noHp.isNotEmpty()) {
            binding.tvUserPhone.text = noHp
        }

        val roleTampil = when (role) {
            "orang_tua" -> "Orang Tua / Siswa"
            "guru"      -> "Guru"
            "pengelola" -> "Pengelola MBG"
            else        -> "-"
        }
        binding.tvUserRole.text = roleTampil

        // Jika data kosong → ambil dari Firestore sebagai fallback
        if (nama.isEmpty() || email.isEmpty()) {
            val uid = FirebaseHelper.auth.currentUser?.uid ?: return
            FirebaseHelper.getDataUser(
                uid       = uid,
                onSuccess = { data ->
                    binding.tvUserName.text  = data["nama"]?.toString()  ?: "-"
                    binding.tvUserEmail.text = data["email"]?.toString() ?: "-"
                    binding.tvUserPhone.text = data["no_hp"]?.toString() ?: "-"
                    val roleDb = data["role"]?.toString() ?: ""
                    binding.tvUserRole.text = when (roleDb) {
                        "orang_tua" -> "Orang Tua / Siswa"
                        "guru"      -> "Guru"
                        "pengelola" -> "Pengelola MBG"
                        else        -> "-"
                    }
                },
                onError = { /* biarkan data kosong */ }
            )
        }
    }

    // ─────────────────────────────────────────────
    // TOMBOL MULAI SEKARANG
    // XML id: btnMulai
    // ─────────────────────────────────────────────
    private fun setupTombolMulai(role: String) {
        binding.btnMulai.setOnClickListener {
            navigasiKeDashboard(role)
        }
    }

    // ─────────────────────────────────────────────
    // TOMBOL KEMBALI KE LOGIN
    // XML id: btnKembaliLogin
    // ─────────────────────────────────────────────
    private fun setupTombolKembaliLogin() {
        binding.btnKembaliLogin.setOnClickListener {
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
        // Jika role sudah diketahui dari intent, langsung navigate
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