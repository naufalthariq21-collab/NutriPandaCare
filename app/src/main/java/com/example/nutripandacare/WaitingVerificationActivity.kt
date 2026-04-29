package com.example.nutripandacare

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.nutripandacare.databinding.ActivityWaitingVerificationBinding
import com.example.nutripandacare.firebase.FirebaseHelper

class WaitingVerificationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWaitingVerificationBinding

    // Handler untuk auto-cek verifikasi email tiap 4 detik
    private val handler  = Handler(Looper.getMainLooper())
    private val interval = 4000L

    private val cekVerifikasi = object : Runnable {
        override fun run() {
            FirebaseHelper.cekEmailVerified { sudahVerified ->
                if (sudahVerified) {
                    // Email sudah diverifikasi → ke halaman sukses
                    startActivity(
                        Intent(this@WaitingVerificationActivity, SuccessActivity::class.java).apply {
                            putExtra("ROLE",  intent.getStringExtra("ROLE")  ?: "")
                            putExtra("NAMA",  intent.getStringExtra("NAMA")  ?: "")
                            putExtra("EMAIL", intent.getStringExtra("EMAIL") ?: "")
                            putExtra("NO_HP", intent.getStringExtra("NO_HP") ?: "")
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                                    Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                    )
                    finish()
                } else {
                    // Belum verified → cek lagi setelah interval
                    handler.postDelayed(this, interval)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWaitingVerificationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Tampilkan role yang dipilih
        // XML id: tvSelectedRole
        val role = intent.getStringExtra("ROLE") ?: ""
        val roleTampil = when (role) {
            "orang_tua" -> "Orang Tua / Siswa"
            "guru"      -> "Guru"
            "pengelola" -> "Pengelola MBG"
            else        -> "-"
        }
        binding.tvSelectedRole.text = roleTampil

        setupTombolBack()
        setupTombolKembaliLogin()
    }

    // Mulai auto-cek saat activity tampil
    override fun onResume() {
        super.onResume()
        handler.post(cekVerifikasi)
    }

    // Stop auto-cek saat activity tidak tampil (hemat baterai)
    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(cekVerifikasi)
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
    // TOMBOL BACK
    // XML id: btnBack
    // ─────────────────────────────────────────────
    private fun setupTombolBack() {
        binding.btnBack.setOnClickListener {
            // Konfirmasi logout sebelum kembali
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Keluar?")
                .setMessage("Kamu akan keluar dan harus login ulang nanti.")
                .setPositiveButton("Keluar") { _, _ ->
                    FirebaseHelper.logout()
                    startActivity(
                        Intent(this, LoginActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                                    Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                    )
                    finish()
                }
                .setNegativeButton("Batal", null)
                .show()
        }
    }
}