package com.example.nutripandacare

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.nutripandacare.databinding.ActivityWaitingVerificationBinding
import com.example.nutripandacare.firebase.FirebaseHelper

class WaitingVerificationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWaitingVerificationBinding

    // Polling tiap 5 detik — cek field is_verified di Firestore (diset pengelola)
    private val handler  = Handler(Looper.getMainLooper())
    private val interval = 5000L

    private val cekVerifikasiPengelola = object : Runnable {
        override fun run() {
            val uid = FirebaseHelper.uid
            if (uid.isEmpty()) { goToLogin(); return }

            FirebaseHelper.db.collection("users").document(uid).get()
                .addOnSuccessListener { doc ->
                    if (isFinishing) return@addOnSuccessListener

                    val isVerified = doc.getBoolean("is_verified") ?: false
                    val statusAkun = doc.getString("status_akun")  ?: "aktif"
                    val role       = doc.getString("role")          ?: ""

                    when {
                        isVerified && statusAkun == "aktif" -> {
                            // Pengelola sudah approve → ke SuccessActivity
                            startActivity(
                                Intent(this@WaitingVerificationActivity, SuccessActivity::class.java).apply {
                                    putExtra("ROLE",  role)
                                    putExtra("NAMA",  intent.getStringExtra("NAMA")  ?: "")
                                    putExtra("EMAIL", intent.getStringExtra("EMAIL") ?: "")
                                    putExtra("NO_HP", intent.getStringExtra("NO_HP") ?: "")
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                }
                            )
                            finish()
                        }
                        statusAkun == "ditolak" -> {
                            // Pengelola reject → tampilkan alasan dan logout
                            val alasan = doc.getString("alasan_tolak") ?: "Tidak memenuhi syarat"
                            AlertDialog.Builder(this@WaitingVerificationActivity)
                                .setTitle("Pendaftaran Ditolak")
                                .setMessage("Maaf, pendaftaranmu ditolak oleh pengelola.\n\nAlasan: $alasan")
                                .setPositiveButton("OK") { _, _ ->
                                    FirebaseHelper.logout()
                                    goToLogin()
                                }
                                .setCancelable(false)
                                .show()
                        }
                        else -> handler.postDelayed(this, interval) // masih pending
                    }
                }
                .addOnFailureListener {
                    handler.postDelayed(this, interval) // coba lagi jika jaringan error
                }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWaitingVerificationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val role = intent.getStringExtra("ROLE") ?: ""
        binding.tvSelectedRole.text = when (role) {
            "orang_tua" -> "Orang Tua / Siswa"
            "guru"      -> "Guru"
            else        -> "-"
        }

        setupTombolKembaliLogin()
        setupTombolBack()
    }

    override fun onResume() {
        super.onResume()
        handler.post(cekVerifikasiPengelola)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(cekVerifikasiPengelola)
    }

    private fun setupTombolKembaliLogin() {
        binding.btnKembaliLogin.setOnClickListener {
            FirebaseHelper.logout()
            goToLogin()
        }
    }

    private fun setupTombolBack() {
        binding.btnBack.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Keluar?")
                .setMessage("Kamu akan keluar. Akun tetap dalam antrian verifikasi pengelola.")
                .setPositiveButton("Keluar") { _, _ ->
                    FirebaseHelper.logout()
                    goToLogin()
                }
                .setNegativeButton("Batal", null)
                .show()
        }
    }

    private fun goToLogin() {
        startActivity(Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }
}