package com.example.nutripandacare

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.example.nutripandacare.databinding.ActivitySplashBinding
import com.example.nutripandacare.firebase.FirebaseHelper

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Handler(Looper.getMainLooper()).postDelayed({ cekStatusLogin() }, 2500)
    }

    private fun cekStatusLogin() {
        val currentUser = FirebaseHelper.auth.currentUser
        if (currentUser == null) {
            goTo(LoginActivity::class.java)
            return
        }

        // Cek status dari Firestore — bukan isEmailVerified Firebase
        FirebaseHelper.db.collection("users").document(currentUser.uid).get()
            .addOnSuccessListener { doc ->
                val role       = doc.getString("role")          ?: ""
                val isVerified = doc.getBoolean("is_verified")  ?: false
                val statusAkun = doc.getString("status_akun")   ?: "aktif"

                when {
                    role.isEmpty() -> {
                        // Belum pilih role
                        startActivity(Intent(this, RoleSelectionActivity::class.java).apply {
                            putExtra("UID", currentUser.uid)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        })
                        finish()
                    }
                    role == "pengelola" -> goTo(DashboardPengelolaActivity::class.java)
                    isVerified && statusAkun == "aktif" -> {
                        // Guru/orang tua sudah diverifikasi pengelola
                        val dest = when (role) {
                            "orang_tua" -> DashboardOrangTuaActivity::class.java
                            "guru"      -> DashboardGuruActivity::class.java
                            else        -> LoginActivity::class.java
                        }
                        goTo(dest)
                    }
                    statusAkun == "ditolak" -> {
                        // Akun ditolak — logout paksa
                        FirebaseHelper.logout()
                        goTo(LoginActivity::class.java)
                    }
                    else -> {
                        // Menunggu verifikasi pengelola
                        startActivity(Intent(this, WaitingVerificationActivity::class.java).apply {
                            putExtra("ROLE", role)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        })
                        finish()
                    }
                }
            }
            .addOnFailureListener { goTo(LoginActivity::class.java) }
    }

    private fun <T> goTo(dest: Class<T>) {
        startActivity(Intent(this, dest).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }
}