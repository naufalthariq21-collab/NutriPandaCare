package com.example.nutripandacare

import android.content.Intent
import android.os.Bundle
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class WaitingVerificationActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ROLE  = "extra_role"
        const val EXTRA_NAMA  = "extra_nama"
        const val EXTRA_EMAIL = "extra_email"
    }

    // Views sesuai ID di activity_waiting_verification.xml
    // ID yang tersedia: btnBack, tvSelectedRole, btnKembaliLogin
    private lateinit var btnBack: ImageButton
    private lateinit var tvSelectedRole: TextView
    private lateinit var btnKembaliLogin: Button

    // Data dari RoleSelectionActivity
    private var selectedRole = ""
    private var userName     = ""
    private var userEmail    = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_waiting_verification)

        intent?.let {
            selectedRole = it.getStringExtra(EXTRA_ROLE)  ?: "Guru"
            userName     = it.getStringExtra(EXTRA_NAMA)  ?: ""
            userEmail    = it.getStringExtra(EXTRA_EMAIL) ?: ""
        }

        initViews()
        populateData()
        setupClickListeners()
        startPulseAnimation()
    }

    private fun initViews() {
        btnBack        = findViewById(R.id.btnBack)
        tvSelectedRole = findViewById(R.id.tvSelectedRole)
        btnKembaliLogin = findViewById(R.id.btnKembaliLogin)
    }

    private fun populateData() {
        // Tampilkan role yang dipilih di status card
        tvSelectedRole.text = selectedRole
    }

    private fun setupClickListeners() {

        btnBack.setOnClickListener {
            @Suppress("DEPRECATION")
            onBackPressed()
        }

        btnKembaliLogin.setOnClickListener {
            navigateToLogin()
        }
    }

    /**
     * Animasi pulse (alpha berulang) pada seluruh konten
     * sebagai indikasi "sedang menunggu"
     */
    private fun startPulseAnimation() {
        // Cari FrameLayout berisi ikon jam via tag atau langsung via ID parent
        // Karena layout tidak punya ID khusus untuk FrameLayout jam,
        // kita animasikan tvSelectedRole sebagai indikator status
        val pulseAnim = AlphaAnimation(0.4f, 1.0f).apply {
            duration    = 1200
            repeatMode  = Animation.REVERSE
            repeatCount = Animation.INFINITE
        }
        tvSelectedRole.startAnimation(pulseAnim)
    }

    /**
     * Kembali ke Login dan clear back stack sepenuhnya.
     * User tidak bisa back ke proses registrasi karena masih pending.
     */
    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
    }
}