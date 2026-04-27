package com.example.nutripandacare

import android.content.Intent
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.nutripandacare.databinding.ActivityRegistrationBinding
import com.example.nutripandacare.firebase.FirebaseHelper

class RegistrationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegistrationBinding
    private var isPasswordVisible        = false
    private var isKonfirmasiVisible      = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegistrationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupTogglePassword()
        setupToggleKonfirmasi()
        setupTombolDaftar()
        setupMasukLink()
        setupTombolBack()
    }

    // ─────────────────────────────────────────────
    // TOGGLE PASSWORD
    // XML id: btnTogglePass, etPassword
    // ─────────────────────────────────────────────
    private fun setupTogglePassword() {
        binding.btnTogglePass.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            binding.etPassword.transformationMethod = if (isPasswordVisible)
                HideReturnsTransformationMethod.getInstance()
            else
                PasswordTransformationMethod.getInstance()
            binding.btnTogglePass.setImageResource(
                if (isPasswordVisible) R.drawable.ic_eye_on else R.drawable.ic_eye_off
            )
            binding.etPassword.setSelection(binding.etPassword.text.length)
        }
    }

    // ─────────────────────────────────────────────
    // TOGGLE KONFIRMASI PASSWORD
    // XML id: btnToggleConfirmPass, etKonfirmasiPassword
    // ─────────────────────────────────────────────
    private fun setupToggleKonfirmasi() {
        binding.btnToggleConfirmPass.setOnClickListener {
            isKonfirmasiVisible = !isKonfirmasiVisible
            binding.etKonfirmasiPassword.transformationMethod = if (isKonfirmasiVisible)
                HideReturnsTransformationMethod.getInstance()
            else
                PasswordTransformationMethod.getInstance()
            binding.btnToggleConfirmPass.setImageResource(
                if (isKonfirmasiVisible) R.drawable.ic_eye_on else R.drawable.ic_eye_off
            )
            binding.etKonfirmasiPassword.setSelection(binding.etKonfirmasiPassword.text.length)
        }
    }

    // ─────────────────────────────────────────────
    // TOMBOL DAFTAR
    // XML id: btnDaftar, etNamaLengkap, etEmail,
    //         etWhatsapp, etPassword, etKonfirmasiPassword
    // ─────────────────────────────────────────────
    private fun setupTombolDaftar() {
        binding.btnDaftar.setOnClickListener {
            val nama             = binding.etNamaLengkap.text.toString().trim()
            val email            = binding.etEmail.text.toString().trim()
            val noHp             = binding.etWhatsapp.text.toString().trim()
            val password         = binding.etPassword.text.toString().trim()
            val konfirmPassword  = binding.etKonfirmasiPassword.text.toString().trim()

            // Validasi semua field
            if (nama.isEmpty()) {
                binding.etNamaLengkap.error = "Nama wajib diisi"
                binding.etNamaLengkap.requestFocus(); return@setOnClickListener
            }
            if (email.isEmpty()) {
                binding.etEmail.error = "Email wajib diisi"
                binding.etEmail.requestFocus(); return@setOnClickListener
            }
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.etEmail.error = "Format email tidak valid"
                binding.etEmail.requestFocus(); return@setOnClickListener
            }
            if (noHp.isEmpty()) {
                binding.etWhatsapp.error = "Nomor WhatsApp wajib diisi"
                binding.etWhatsapp.requestFocus(); return@setOnClickListener
            }
            if (noHp.length < 10) {
                binding.etWhatsapp.error = "Nomor tidak valid"
                binding.etWhatsapp.requestFocus(); return@setOnClickListener
            }
            if (password.isEmpty()) {
                binding.etPassword.error = "Password wajib diisi"
                binding.etPassword.requestFocus(); return@setOnClickListener
            }
            if (password.length < 8) {
                binding.etPassword.error = "Password minimal 8 karakter"
                binding.etPassword.requestFocus(); return@setOnClickListener
            }
            if (konfirmPassword.isEmpty()) {
                binding.etKonfirmasiPassword.error = "Konfirmasi password wajib diisi"
                binding.etKonfirmasiPassword.requestFocus(); return@setOnClickListener
            }
            if (password != konfirmPassword) {
                binding.etKonfirmasiPassword.error = "Password tidak cocok"
                binding.etKonfirmasiPassword.requestFocus(); return@setOnClickListener
            }

            setLoading(true)

            // Register via FirebaseHelper
            FirebaseHelper.register(
                nama      = nama,
                email     = email,
                noHp      = noHp,
                password  = password,
                onSuccess = { uid ->
                    setLoading(false)
                    // Setelah register → ke pilih role
                    // Kirim uid & data user ke RoleSelectionActivity
                    startActivity(
                        Intent(this, RoleSelectionActivity::class.java).apply {
                            putExtra("UID",   uid)
                            putExtra("NAMA",  nama)
                            putExtra("EMAIL", email)
                            putExtra("NO_HP", noHp)
                        }
                    )
                    finish()
                },
                onError = { pesan ->
                    setLoading(false)
                    val pesanRamah = when {
                        pesan.contains("email", ignoreCase = true) &&
                                pesan.contains("already", ignoreCase = true) ->
                            "Email sudah terdaftar. Coba login."
                        pesan.contains("network", ignoreCase = true) ->
                            "Tidak ada koneksi internet."
                        pesan.contains("weak-password", ignoreCase = true) ->
                            "Password terlalu lemah."
                        else -> "Registrasi gagal. Coba lagi."
                    }
                    Toast.makeText(this, pesanRamah, Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    // ─────────────────────────────────────────────
    // LINK MASUK (sudah punya akun)
    // XML id: tvMasukLink
    // ─────────────────────────────────────────────
    private fun setupMasukLink() {
        binding.tvMasukLink.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    // ─────────────────────────────────────────────
    // TOMBOL BACK
    // XML id: btnBack
    // ─────────────────────────────────────────────
    private fun setupTombolBack() {
        binding.btnBack.setOnClickListener { finish() }
    }

    private fun setLoading(loading: Boolean) {
        binding.btnDaftar.isEnabled = !loading
        binding.btnDaftar.text = if (loading) "Mendaftarkan..." else getString(R.string.daftar)
    }
}