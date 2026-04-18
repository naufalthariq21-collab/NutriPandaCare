package com.example.nutripandacare

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SuccessActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_NAMA  = "extra_nama"
        const val EXTRA_ROLE  = "extra_role"
        const val EXTRA_EMAIL = "extra_email"
        const val EXTRA_PHONE = "extra_phone"
    }

    // Views sesuai ID di activity_success.xml
    // ID yang tersedia: tvUserName, tvUserRole, tvUserEmail, tvUserPhone, btnMulai, btnKembaliLogin
    private lateinit var tvUserName: TextView
    private lateinit var tvUserRole: TextView
    private lateinit var tvUserEmail: TextView
    private lateinit var tvUserPhone: TextView
    private lateinit var btnMulai: Button
    private lateinit var btnKembaliLogin: Button

    // Data dari RoleSelectionActivity (via intent)
    private var userName  = ""
    private var userRole  = ""
    private var userEmail = ""
    private var userPhone = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_success)

        intent?.let {
            userName  = it.getStringExtra(EXTRA_NAMA)  ?: ""
            userRole  = it.getStringExtra(EXTRA_ROLE)  ?: ""
            userEmail = it.getStringExtra(EXTRA_EMAIL) ?: ""
            userPhone = it.getStringExtra(EXTRA_PHONE) ?: ""
        }

        initViews()
        populateUserData()
        setupClickListeners()
        playSuccessAnimation()
    }

    private fun initViews() {
        tvUserName      = findViewById(R.id.tvUserName)
        tvUserRole      = findViewById(R.id.tvUserRole)
        tvUserEmail     = findViewById(R.id.tvUserEmail)
        tvUserPhone     = findViewById(R.id.tvUserPhone)
        btnMulai        = findViewById(R.id.btnMulai)
        btnKembaliLogin = findViewById(R.id.btnKembaliLogin)
    }

    private fun populateUserData() {
        tvUserName.text  = userName.ifEmpty  { getString(R.string.user_name) }
        tvUserRole.text  = userRole.ifEmpty  { getString(R.string.user_role) }
        tvUserEmail.text = userEmail.ifEmpty { getString(R.string.user_email) }
        tvUserPhone.text = userPhone.ifEmpty { getString(R.string.user_phone) }
    }

    private fun setupClickListeners() {

        // Mulai Sekarang → Dashboard
        btnMulai.setOnClickListener {
            navigateToDashboard()
        }

        // Kembali ke Login → clear back stack
        btnKembaliLogin.setOnClickListener {
            navigateToLogin()
        }
    }

    /**
     * Animasi bounce pada lingkaran sukses hijau.
     * Tidak pakai try-catch karena ID sudah benar.
     */
    private fun playSuccessAnimation() {
        // Cari container lingkaran hijau via parent layout
        // Karena FrameLayout lingkaran tidak diberi ID di XML,
        // kita animasikan seluruh konten dengan fade-in dari bawah
        val rootView = findViewById<android.view.ViewGroup>(android.R.id.content)
        rootView.alpha = 0f
        rootView.translationY = 40f
        rootView.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(500)
            .setInterpolator(android.view.animation.DecelerateInterpolator())
            .start()
    }

    /**
     * Navigasi ke Dashboard utama.
     * Back stack dibersihkan agar user tidak bisa back ke registrasi.
     * TODO: ganti DashboardActivity::class.java saat sudah dibuat.
     */
    private fun navigateToDashboard() {
        // val intent = Intent(this, DashboardActivity::class.java).apply {
        //     flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        // }
        // startActivity(intent)
        // overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        // finish()

        // Sementara tampilkan Toast sampai DashboardActivity dibuat
        android.widget.Toast.makeText(
            this,
            "Selamat datang, $userName! Dashboard akan segera hadir.",
            android.widget.Toast.LENGTH_LONG
        ).show()
    }

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
        navigateToDashboard()
    }
}