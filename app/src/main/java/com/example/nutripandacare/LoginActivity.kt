package com.example.nutripandacare

import android.content.Intent
import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import android.text.method.SingleLineTransformationMethod
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnMasuk: Button
    private lateinit var btnTogglePassword: ImageButton
    private lateinit var btnBack: ImageButton
    private lateinit var tvForgotPassword: TextView
    private lateinit var tvDaftarSekarang: TextView
    private lateinit var btnGoogle: Button
    private lateinit var btnFacebook: Button

    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        initViews()
        setupClickListeners()
        setupInputFocusEffects()
    }

    private fun initViews() {
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnMasuk = findViewById(R.id.btnMasuk)
        btnTogglePassword = findViewById(R.id.btnTogglePassword)
        btnBack = findViewById(R.id.btnBack)
        tvForgotPassword = findViewById(R.id.tvForgotPassword)
        tvDaftarSekarang = findViewById(R.id.tvDaftarSekarang)
        btnGoogle = findViewById(R.id.btnGoogle)
    }

    private fun setupClickListeners() {

        // Tombol Back
        btnBack.setOnClickListener {
            onBackPressed()
        }

        // Toggle Password Visibility
        btnTogglePassword.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            if (isPasswordVisible) {
                // Tampilkan password
                etPassword.transformationMethod = SingleLineTransformationMethod.getInstance()
                btnTogglePassword.setImageResource(R.drawable.ic_eye_on)
            } else {
                // Sembunyikan password
                etPassword.transformationMethod = PasswordTransformationMethod.getInstance()
                btnTogglePassword.setImageResource(R.drawable.ic_eye_off)
            }
            // Pindahkan cursor ke akhir teks
            etPassword.setSelection(etPassword.text.length)
        }

        // Lupa Password
        tvForgotPassword.setOnClickListener {
            // TODO: Navigate ke ForgotPasswordActivity
            Toast.makeText(this, "Fitur lupa password akan segera hadir", Toast.LENGTH_SHORT).show()
        }

        // Tombol Masuk
        btnMasuk.setOnClickListener {
            if (validateInputs()) {
                performLogin()
            }
        }

        // Login Google
        btnGoogle.setOnClickListener {
            Toast.makeText(this, "Login dengan Google", Toast.LENGTH_SHORT).show()
            // TODO: Implementasi Google Sign-In SDK
        }

        // Login Facebook
        btnFacebook.setOnClickListener {
            Toast.makeText(this, "Login dengan Facebook", Toast.LENGTH_SHORT).show()
            // TODO: Implementasi Facebook Login SDK
        }

        // Daftar Sekarang
        tvDaftarSekarang.setOnClickListener {
            navigateToRegistration()
        }
    }

    /**
     * Setup efek visual ketika input field difokus
     */
    private fun setupInputFocusEffects() {
        etEmail.setOnFocusChangeListener { view, hasFocus ->
            if (hasFocus) {
                view.background = getDrawable(R.drawable.bg_input_field_focused)
            } else {
                view.background = getDrawable(R.drawable.bg_input_field)
            }
        }

        // Untuk field di dalam FrameLayout, kita perlu set ke parent-nya
        etPassword.setOnFocusChangeListener { _, _ ->
            // Handled by parent FrameLayout background change
        }
    }

    /**
     * Validasi input login
     */
    private fun validateInputs(): Boolean {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString()

        if (email.isEmpty()) {
            etEmail.error = "Email tidak boleh kosong"
            etEmail.requestFocus()
            return false
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.error = "Format email tidak valid"
            etEmail.requestFocus()
            return false
        }

        if (password.isEmpty()) {
            etPassword.error = "Password tidak boleh kosong"
            etPassword.requestFocus()
            return false
        }

        if (password.length < 8) {
            etPassword.error = "Password minimal 8 karakter"
            etPassword.requestFocus()
            return false
        }

        return true
    }

    /**
     * Proses login - kirim ke API atau validasi lokal
     */
    private fun performLogin() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString()

        // Tampilkan loading state
        btnMasuk.isEnabled = false
        btnMasuk.text = "Memproses..."

        // TODO: Ganti dengan API call sesungguhnya
        // Simulasi delay API call
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            btnMasuk.isEnabled = true
            btnMasuk.text = getString(R.string.masuk)

            // Simulasi login berhasil - navigate ke dashboard
            // Dalam implementasi nyata, cek response dari API
            Toast.makeText(this, "Login berhasil!", Toast.LENGTH_SHORT).show()
            navigateToDashboard()

        }, 1500)
    }

    private fun navigateToRegistration() {
        val intent = Intent(this, RegistrationActivity::class.java)
        startActivity(intent)
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
    }

    private fun navigateToDashboard() {
        // TODO: Ganti dengan DashboardActivity ketika sudah dibuat
        Toast.makeText(this, "Menuju Dashboard...", Toast.LENGTH_SHORT).show()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
    }
}