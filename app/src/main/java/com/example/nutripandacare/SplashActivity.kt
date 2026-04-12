package com.example.nutripandacare

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.appcompat.app.AppCompatActivity
import com.example.nutripandacare.databinding.ActivitySplashBinding

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        startAnimation()
    }

    private fun startAnimation() {
        val ease      = AccelerateDecelerateInterpolator()
        val overshoot = OvershootInterpolator(1.2f)

        // Logo: scale dari kecil + fade in dengan bounce
        binding.imgLogo.apply {
            scaleX = 0.4f
            scaleY = 0.4f
            animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(700)
                .setInterpolator(overshoot)
                .start()
        }

        // Nama app: slide dari bawah + fade in
        binding.tvAppName.apply {
            translationY = 30f
            animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(500)
                .setDuration(500)
                .setInterpolator(ease)
                .start()
        }

        // Tagline: fade in
        binding.tvTagline.animate()
            .alpha(1f)
            .setStartDelay(700)
            .setDuration(400)
            .setInterpolator(ease)
            .start()

        // Progress bar muncul
        binding.progressBar.animate()
            .alpha(1f)
            .setStartDelay(900)
            .setDuration(300)
            .start()

        // Navigasi setelah 2.5 detik
        handler.postDelayed({ navigateToNext() }, 2500)
    }

    private fun navigateToNext() {
        val prefs      = getSharedPreferences("nutripanda_prefs", MODE_PRIVATE)
        val isLoggedIn = prefs.getBoolean("is_logged_in", false)

        val destination = if (isLoggedIn) {
            // TODO: ganti sesuai dashboard role masing-masing
            // val role = prefs.getString("user_role", "")
            // when (role) { "guru" -> GuruDashboardActivity::class.java ... }
            MainActivity::class.java
        } else {
            LoginActivity::class.java
        }

        startActivity(Intent(this, destination))
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}