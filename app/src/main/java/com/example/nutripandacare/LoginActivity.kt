package com.example.nutripandacare

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.nutripandacare.databinding.ActivityLoginBinding
import com.example.nutripandacare.firebase.FirebaseHelper
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var googleSignInClient: GoogleSignInClient
    private var isPasswordVisible = false

    // ─────────────────────────────────────────────
    // Launcher ActivityResult untuk Google Sign-In
    // ─────────────────────────────────────────────
    private val googleLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                try {
                    val account = task.getResult(ApiException::class.java)
                    val idToken = account?.idToken
                    if (idToken != null) {
                        prosesGoogleLogin(idToken)
                    } else {
                        setLoading(false)
                        Toast.makeText(this, "Gagal mendapatkan token Google.", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: ApiException) {
                    setLoading(false)
                    Toast.makeText(this, "Google Sign-In dibatalkan.", Toast.LENGTH_SHORT).show()
                }
            } else {
                setLoading(false)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupGoogleSignIn()
        setupTogglePassword()
        setupTombolMasuk()
        setupLupaPassword()
        setupDaftarLink()
        setupTombolBack()
        setupTombolGoogle()
    }

    // ─────────────────────────────────────────────
    // INISIALISASI GOOGLE SIGN-IN CLIENT
    // Web client ID dari google-services.json /
    // Firebase Console → Authentication → Google
    // ─────────────────────────────────────────────
    private fun setupGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id)) // auto-generated oleh Firebase plugin
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    // ─────────────────────────────────────────────
    // TOMBOL GOOGLE
    // XML id: btnGoogle
    // ─────────────────────────────────────────────
    private fun setupTombolGoogle() {
        binding.btnGoogle.setOnClickListener {
            setLoading(true)
            // Sign out dulu agar user bisa pilih akun Google berbeda
            googleSignInClient.signOut().addOnCompleteListener {
                googleLauncher.launch(googleSignInClient.signInIntent)
            }
        }
    }

    // ─────────────────────────────────────────────
    // PROSES LOGIN GOOGLE → FIREBASE
    // Dipanggil setelah dapat idToken dari launcher
    // ─────────────────────────────────────────────
    private fun prosesGoogleLogin(idToken: String) {
        FirebaseHelper.loginWithGoogle(
            idToken   = idToken,
            onSuccess = { uid, isNewUser ->
                if (isNewUser) {
                    // User baru via Google → pilih role dulu
                    setLoading(false)
                    startActivity(
                        Intent(this, RoleSelectionActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                    )
                    finish()
                } else {
                    // User lama → cek role, lanjut ke dashboard
                    FirebaseHelper.getRole(
                        uid       = uid,
                        onSuccess = { role ->
                            setLoading(false)
                            val dest = when (role) {
                                "orang_tua" -> DashboardOrangTuaActivity::class.java
                                "guru"      -> DashboardGuruActivity::class.java
                                "pengelola" -> DashboardPengelolaActivity::class.java
                                else        -> RoleSelectionActivity::class.java // role kosong
                            }
                            startActivity(
                                Intent(this, dest).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                }
                            )
                            finish()
                        },
                        onError = { pesan ->
                            setLoading(false)
                            Toast.makeText(this, pesan, Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            },
            onError = { pesan ->
                setLoading(false)
                Toast.makeText(this, "Google login gagal: $pesan", Toast.LENGTH_LONG).show()
            }
        )
    }

    // ─────────────────────────────────────────────
    // TOGGLE SHOW/HIDE PASSWORD
    // XML id: btnTogglePassword, etPassword
    // ─────────────────────────────────────────────
    private fun setupTogglePassword() {
        binding.btnTogglePassword.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            binding.etPassword.transformationMethod = if (isPasswordVisible)
                HideReturnsTransformationMethod.getInstance()
            else
                PasswordTransformationMethod.getInstance()
            binding.btnTogglePassword.setImageResource(
                if (isPasswordVisible) R.drawable.ic_eye_on else R.drawable.ic_eye_off
            )
            binding.etPassword.setSelection(binding.etPassword.text.length)
        }
    }

    // ─────────────────────────────────────────────
    // TOMBOL MASUK (email & password)
    // XML id: btnMasuk, etEmail, etPassword
    // ─────────────────────────────────────────────
    private fun setupTombolMasuk() {
        binding.btnMasuk.setOnClickListener {
            val email    = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (email.isEmpty()) {
                binding.etEmail.error = "Email wajib diisi"; return@setOnClickListener
            }
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.etEmail.error = "Format email tidak valid"; return@setOnClickListener
            }
            if (password.isEmpty()) {
                binding.etPassword.error = "Password wajib diisi"; return@setOnClickListener
            }

            setLoading(true)

            FirebaseHelper.login(
                email    = email,
                password = password,
                onSuccess = { uid ->
                    FirebaseHelper.getRole(
                        uid       = uid,
                        onSuccess = { role ->
                            setLoading(false)
                            val dest = when (role) {
                                "orang_tua" -> DashboardOrangTuaActivity::class.java
                                "guru"      -> DashboardGuruActivity::class.java
                                "pengelola" -> DashboardPengelolaActivity::class.java
                                else        -> RoleSelectionActivity::class.java
                            }
                            startActivity(Intent(this, dest).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                                        Intent.FLAG_ACTIVITY_CLEAR_TASK
                            })
                            finish()
                        },
                        onError = { pesan ->
                            setLoading(false)
                            Toast.makeText(this, pesan, Toast.LENGTH_SHORT).show()
                        }
                    )
                },
                onError = { pesan ->
                    setLoading(false)
                    val pesanRamah = when {
                        pesan.contains("password", ignoreCase = true) ->
                            "Password salah. Coba lagi."
                        pesan.contains("no user", ignoreCase = true) ->
                            "Email tidak terdaftar."
                        pesan.contains("network", ignoreCase = true) ->
                            "Tidak ada koneksi internet."
                        pesan.contains("verified", ignoreCase = true) ->
                            "Email belum diverifikasi. Cek inbox kamu."
                        else -> "Login gagal. Periksa email & password kamu."
                    }
                    Toast.makeText(this, pesanRamah, Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    // ─────────────────────────────────────────────
    // LUPA PASSWORD
    // XML id: tvForgotPassword
    // ─────────────────────────────────────────────
    private fun setupLupaPassword() {
        binding.tvForgotPassword.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            if (email.isEmpty()) {
                binding.etEmail.error = "Masukkan email kamu dulu"
                binding.etEmail.requestFocus()
                return@setOnClickListener
            }
            FirebaseHelper.resetPassword(
                email     = email,
                onSuccess = {
                    Toast.makeText(this,
                        "Link reset password dikirim ke $email",
                        Toast.LENGTH_LONG).show()
                },
                onError = {
                    Toast.makeText(this, "Email tidak ditemukan.", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    // ─────────────────────────────────────────────
    // LINK DAFTAR SEKARANG
    // XML id: tvDaftarSekarang
    // ─────────────────────────────────────────────
    private fun setupDaftarLink() {
        binding.tvDaftarSekarang.setOnClickListener {
            startActivity(Intent(this, RegistrationActivity::class.java))
        }
    }

    // ─────────────────────────────────────────────
    // TOMBOL BACK
    // XML id: btnBack
    // ─────────────────────────────────────────────
    private fun setupTombolBack() {
        binding.btnBack.setOnClickListener { finish() }
    }

    // ─────────────────────────────────────────────
    // LOADING STATE — disable semua tombol saat proses
    // ─────────────────────────────────────────────
    private fun setLoading(loading: Boolean) {
        binding.btnMasuk.isEnabled  = !loading
        binding.btnGoogle.isEnabled = !loading
        binding.btnMasuk.text  = if (loading) "Memproses..." else getString(R.string.masuk)
        binding.btnGoogle.text = if (loading) "Memproses..." else getString(R.string.masuk_dengan_google)
    }
}