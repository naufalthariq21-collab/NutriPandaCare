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

        // Tunda 2.5 detik lalu cek status login
        Handler(Looper.getMainLooper()).postDelayed({
            cekStatusLogin()
        }, 2500)
    }

    private fun cekStatusLogin() {
        val currentUser = FirebaseHelper.auth.currentUser

        // Belum login → ke Login
        if (currentUser == null) {
            goTo(LoginActivity::class.java)
            return
        }

        // Reload dulu supaya status email verified up to date
        currentUser.reload().addOnSuccessListener {
            val user = FirebaseHelper.auth.currentUser ?: run {
                goTo(LoginActivity::class.java)
                return@addOnSuccessListener
            }

            if (!user.isEmailVerified) {
                goTo(WaitingVerificationActivity::class.java)
                return@addOnSuccessListener
            }

            // Sudah login & verified → cek role
            FirebaseHelper.getRole(
                uid       = user.uid,
                onSuccess = { role ->
                    val dest = when (role) {
                        "orang_tua" -> DashboardOrangTuaActivity::class.java
                        "guru"      -> DashboardGuruActivity::class.java
                        "pengelola" -> DashboardPengelolaActivity::class.java
                        else        -> LoginActivity::class.java
                    }
                    goTo(dest)
                },
                onError = { goTo(LoginActivity::class.java) }
            )
        }.addOnFailureListener {
            goTo(LoginActivity::class.java)
        }
    }

    private fun <T> goTo(dest: Class<T>) {
        startActivity(Intent(this, dest).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }
}