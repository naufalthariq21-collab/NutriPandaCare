package com.example.nutripandacare

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.method.PasswordTransformationMethod
import android.text.method.SingleLineTransformationMethod
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class RegistrationActivity : AppCompatActivity() {

    private lateinit var etNamaLengkap: EditText
    private lateinit var etEmail: EditText
    private lateinit var etWhatsapp: EditText
    private lateinit var etPassword: EditText
    private lateinit var etKonfirmasiPassword: EditText
    private lateinit var btnTogglePass: ImageButton
    private lateinit var btnToggleConfirmPass: ImageButton
    private lateinit var btnDaftar: Button
    private lateinit var tvMasukLink: TextView
    private lateinit var btnBack: ImageButton

    private var isPasswordVisible = false
    private var isConfirmPasswordVisible = false

    // Data yang akan dikirim ke RoleSelectionActivity
    private var registrationData = RegistrationData()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registration)

        initViews()
        setupClickListeners()
        setupRealTimeValidation()
    }

    private fun initViews() {
        etNamaLengkap = findViewById(R.id.etNamaLengkap)
        etEmail = findViewById(R.id.etEmail)
        etWhatsapp = findViewById(R.id.etWhatsapp)
        etPassword = findViewById(R.id.etPassword)
        etKonfirmasiPassword = findViewById(R.id.etKonfirmasiPassword)
        btnTogglePass = findViewById(R.id.btnTogglePass)
        btnToggleConfirmPass = findViewById(R.id.btnToggleConfirmPass)
        btnDaftar = findViewById(R.id.btnDaftar)
        tvMasukLink = findViewById(R.id.tvMasukLink)
        btnBack = findViewById(R.id.btnBack)
    }

    private fun setupClickListeners() {

        // Tombol Back
        btnBack.setOnClickListener {
            onBackPressed()
        }

        // Toggle Password
        btnTogglePass.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            togglePasswordVisibility(etPassword, isPasswordVisible)
            updateToggleIcon(btnTogglePass, isPasswordVisible)
        }

        // Toggle Konfirmasi Password
        btnToggleConfirmPass.setOnClickListener {
            isConfirmPasswordVisible = !isConfirmPasswordVisible
            togglePasswordVisibility(etKonfirmasiPassword, isConfirmPasswordVisible)
            updateToggleIcon(btnToggleConfirmPass, isConfirmPasswordVisible)
        }

        // Tombol Daftar
        btnDaftar.setOnClickListener {
            if (validateInputs()) {
                collectRegistrationData()
                navigateToRoleSelection()
            }
        }

        // Link Masuk (sudah punya akun)
        tvMasukLink.setOnClickListener {
            navigateToLogin()
        }
    }

    private fun togglePasswordVisibility(editText: EditText, isVisible: Boolean) {
        if (isVisible) {
            editText.transformationMethod = SingleLineTransformationMethod.getInstance()
        } else {
            editText.transformationMethod = PasswordTransformationMethod.getInstance()
        }
        editText.setSelection(editText.text.length)
    }

    private fun updateToggleIcon(button: ImageButton, isVisible: Boolean) {
        if (isVisible) {
            button.setImageResource(R.drawable.ic_eye_on)
        } else {
            button.setImageResource(R.drawable.ic_eye_off)
        }
    }

    /**
     * Setup validasi real-time saat pengguna mengetik
     */
    private fun setupRealTimeValidation() {

        // Format nomor WhatsApp otomatis
        etWhatsapp.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val text = s?.toString() ?: ""
                // Tambahkan +62 jika user mengetik 08...
                if (text.startsWith("08") && text.length == 2) {
                    etWhatsapp.removeTextChangedListener(this)
                    etWhatsapp.setText("+62 8")
                    etWhatsapp.setSelection(etWhatsapp.text.length)
                    etWhatsapp.addTextChangedListener(this)
                }
            }
        })

        // Validasi konfirmasi password real-time
        etKonfirmasiPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val password = etPassword.text.toString()
                val confirmPassword = s?.toString() ?: ""
                if (confirmPassword.isNotEmpty() && password != confirmPassword) {
                    etKonfirmasiPassword.error = "Password tidak cocok"
                } else {
                    etKonfirmasiPassword.error = null
                }
            }
        })
    }

    /**
     * Validasi semua input field
     */
    private fun validateInputs(): Boolean {
        val nama = etNamaLengkap.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val whatsapp = etWhatsapp.text.toString().trim()
        val password = etPassword.text.toString()
        val konfirmasiPassword = etKonfirmasiPassword.text.toString()

        // Validasi Nama
        if (nama.isEmpty()) {
            etNamaLengkap.error = "Nama lengkap tidak boleh kosong"
            etNamaLengkap.requestFocus()
            return false
        }
        if (nama.length < 3) {
            etNamaLengkap.error = "Nama minimal 3 karakter"
            etNamaLengkap.requestFocus()
            return false
        }

        // Validasi Email
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

        // Validasi WhatsApp
        if (whatsapp.isEmpty()) {
            etWhatsapp.error = "Nomor WhatsApp tidak boleh kosong"
            etWhatsapp.requestFocus()
            return false
        }
        val cleanPhone = whatsapp.replace(Regex("[^0-9+]"), "")
        if (cleanPhone.length < 10) {
            etWhatsapp.error = "Nomor WhatsApp tidak valid (minimal 10 digit)"
            etWhatsapp.requestFocus()
            return false
        }

        // Validasi Password
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

        // Validasi Konfirmasi Password
        if (konfirmasiPassword.isEmpty()) {
            etKonfirmasiPassword.error = "Konfirmasi password tidak boleh kosong"
            etKonfirmasiPassword.requestFocus()
            return false
        }
        if (password != konfirmasiPassword) {
            etKonfirmasiPassword.error = "Password tidak cocok"
            etKonfirmasiPassword.requestFocus()
            return false
        }

        return true
    }

    /**
     * Kumpulkan data registrasi untuk dikirim ke activity berikutnya
     */
    private fun collectRegistrationData() {
        registrationData = RegistrationData(
            namaLengkap = etNamaLengkap.text.toString().trim(),
            email = etEmail.text.toString().trim(),
            whatsapp = etWhatsapp.text.toString().trim(),
            password = etPassword.text.toString()
        )
    }

    private fun navigateToRoleSelection() {
        val intent = Intent(this, RoleSelectionActivity::class.java).apply {
            putExtra(RoleSelectionActivity.EXTRA_NAMA, registrationData.namaLengkap)
            putExtra(RoleSelectionActivity.EXTRA_EMAIL, registrationData.email)
            putExtra(RoleSelectionActivity.EXTRA_WHATSAPP, registrationData.whatsapp)
            putExtra(RoleSelectionActivity.EXTRA_PASSWORD, registrationData.password)
        }
        startActivity(intent)
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
    }

    private fun navigateToLogin() {
        finish()
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
    }

    /**
     * Data class untuk menyimpan data registrasi sementara
     */
    data class RegistrationData(
        val namaLengkap: String = "",
        val email: String = "",
        val whatsapp: String = "",
        val password: String = ""
    )
}