package com.example.nutripandacare

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.nutripandacare.databinding.ActivityRegistrationBinding
import com.example.nutripandacare.firebase.FirebaseHelper

class RegistrationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegistrationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegistrationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupTombolDaftar()
        setupMasukLink()
        setupTombolBack()
    }

    // ─────────────────────────────────────────────
    // TOMBOL DAFTAR
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

    private fun setupMasukLink() {
        binding.tvMasukLink.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun setupTombolBack() {
        binding.btnBack.setOnClickListener { finish() }
    }

    private fun setLoading(loading: Boolean) {
        binding.btnDaftar.isEnabled = !loading
        binding.btnDaftar.text = if (loading) "Mendaftarkan..." else getString(R.string.daftar)
    }
}
