package com.example.nutripandacare

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
import com.google.firebase.auth.GoogleAuthProvider

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var googleSignInClient: GoogleSignInClient
    private var isPasswordVisible = false

    // ─────────────────────────────────────────────
    // Google Sign-In result launcher
    // ─────────────────────────────────────────────
    private val googleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account.idToken ?: run {
                setLoading(false)
                Toast.makeText(this, "Google Sign-In gagal. Coba lagi.", Toast.LENGTH_SHORT).show()
                return@registerForActivityResult
            }
            firebaseAuthWithGoogle(idToken)
        } catch (e: ApiException) {
            setLoading(false)
            Toast.makeText(this, "Google Sign-In dibatalkan.", Toast.LENGTH_SHORT).show()
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
        setupTombolGoogle()
        setupDaftarLink()
        setupTombolBack()
    }

    // ─────────────────────────────────────────────
    // SETUP GOOGLE SIGN-IN CLIENT
    // Web Client ID diambil dari strings.xml
    // nama string: default_web_client_id
    // (otomatis dibuat saat sync google-services.json)
    // ─────────────────────────────────────────────
    private fun setupGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
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
            // Sign out dulu agar selalu muncul picker akun Google
            googleSignInClient.signOut().addOnCompleteListener {
                googleLauncher.launch(googleSignInClient.signInIntent)
            }
        }
    }

    // ─────────────────────────────────────────────
    // AUTH GOOGLE TOKEN KE FIREBASE
    // ─────────────────────────────────────────────
    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)

        FirebaseHelper.auth.signInWithCredential(credential)
            .addOnSuccessListener { result ->
                val uid      = result.user?.uid ?: return@addOnSuccessListener
                val isNewUser = result.additionalUserInfo?.isNewUser ?: false
                val nama     = result.user?.displayName ?: ""
                val email    = result.user?.email ?: ""
                val fotoUrl  = result.user?.photoUrl?.toString() ?: ""

                if (isNewUser) {
                    // User baru via Google → simpan ke Firestore → pilih role
                    simpanUserBaruGoogle(uid, nama, email, fotoUrl)
                } else {
                    // User lama → langsung cek role & navigate
                    cekRoleDanNavigate(uid)
                }
            }
            .addOnFailureListener { e ->
                setLoading(false)
                Toast.makeText(this,
                    "Login Google gagal: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // ─────────────────────────────────────────────
    // SIMPAN USER BARU GOOGLE KE FIRESTORE
    // Path: users/{uid}
    // ─────────────────────────────────────────────
    private fun simpanUserBaruGoogle(
        uid: String, nama: String, email: String, fotoUrl: String
    ) {
        val data = hashMapOf(
            "nama"        to nama,
            "email"       to email,
            "no_hp"       to "",
            "role"        to "",
            "foto_url"    to fotoUrl,
            "is_verified" to true,      // Google sudah verified otomatis
            "status_akun" to "aktif",
            "created_at"  to com.google.firebase.Timestamp.now(),
            "updated_at"  to com.google.firebase.Timestamp.now()
        )

        FirebaseHelper.db.collection("users").document(uid)
            .set(data)
            .addOnSuccessListener {
                setLoading(false)
                // Arahkan ke pilih role
                startActivity(
                    Intent(this, RoleSelectionActivity::class.java).apply {
                        putExtra("UID",   uid)
                        putExtra("NAMA",  nama)
                        putExtra("EMAIL", email)
                    }
                )
                finish()
            }
            .addOnFailureListener {
                setLoading(false)
                Toast.makeText(this, "Gagal simpan data. Coba lagi.", Toast.LENGTH_SHORT).show()
            }
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
    // TOMBOL MASUK — Email & Password
    // XML id: btnMasuk, etEmail, etPassword
    // ─────────────────────────────────────────────
    private fun setupTombolMasuk() {
        binding.btnMasuk.setOnClickListener {
            val email    = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (email.isEmpty()) {
                binding.etEmail.error = "Email wajib diisi"
                binding.etEmail.requestFocus(); return@setOnClickListener
            }
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.etEmail.error = "Format email tidak valid"
                binding.etEmail.requestFocus(); return@setOnClickListener
            }
            if (password.isEmpty()) {
                binding.etPassword.error = "Password wajib diisi"
                binding.etPassword.requestFocus(); return@setOnClickListener
            }

            setLoading(true)

            FirebaseHelper.login(
                email    = email,
                password = password,
                onSuccess = { uid -> cekRoleDanNavigate(uid) },
                onError   = { pesan ->
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
    // CEK ROLE LALU NAVIGATE KE DASHBOARD
    // ─────────────────────────────────────────────
    private fun cekRoleDanNavigate(uid: String) {
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
                    if (dest == RoleSelectionActivity::class.java) {
                        putExtra("UID", uid)
                    }
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                })
                finish()
            },
            onError = { pesan ->
                setLoading(false)
                Toast.makeText(this, pesan, Toast.LENGTH_SHORT).show()
            }
        )
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
                binding.etEmail.requestFocus(); return@setOnClickListener
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
    // LOADING STATE — disable semua tombol
    // ─────────────────────────────────────────────
    private fun setLoading(loading: Boolean) {
        binding.btnMasuk.isEnabled  = !loading
        binding.btnGoogle.isEnabled = !loading
        binding.btnMasuk.text =
            if (loading) "Memproses..." else getString(R.string.masuk)
        binding.btnGoogle.text =
            if (loading) "Memproses..." else "Masuk dengan Google"
    }
}