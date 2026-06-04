package com.example.nutripandacare

import android.content.Intent
import android.os.Bundle
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
        setupTombolMasuk()
        setupLupaPassword()
        setupTombolGoogle()
        setupDaftarLink()
        setupTombolBack()
    }

    private fun setupGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    private fun setupTombolGoogle() {
        binding.btnGoogle.setOnClickListener {
            setLoading(true)
            googleSignInClient.signOut().addOnCompleteListener {
                googleLauncher.launch(googleSignInClient.signInIntent)
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        FirebaseHelper.auth.signInWithCredential(credential)
            .addOnSuccessListener { result ->
                val uid       = result.user?.uid ?: return@addOnSuccessListener
                val isNewUser = result.additionalUserInfo?.isNewUser ?: false
                val nama      = result.user?.displayName ?: ""
                val email     = result.user?.email ?: ""
                val fotoUrl   = result.user?.photoUrl?.toString() ?: ""

                if (isNewUser) {
                    simpanUserBaruGoogle(uid, nama, email, fotoUrl)
                } else {
                    cekStatusDanNavigate(uid)
                }
            }
            .addOnFailureListener { e ->
                setLoading(false)
                Toast.makeText(this, "Login Google gagal: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun simpanUserBaruGoogle(uid: String, nama: String, email: String, fotoUrl: String) {
        val data = hashMapOf(
            "nama"        to nama,
            "email"       to email,
            "no_hp"       to "",
            "role"        to "",
            "foto_url"    to fotoUrl,
            "is_verified" to true,          // Google user auto-verified
            "status_akun" to "aktif",
            "created_at"  to com.google.firebase.Timestamp.now(),
            "updated_at"  to com.google.firebase.Timestamp.now()
        )
        FirebaseHelper.db.collection("users").document(uid)
            .set(data)
            .addOnSuccessListener {
                setLoading(false)
                startActivity(Intent(this, RoleSelectionActivity::class.java).apply {
                    putExtra("UID",   uid)
                    putExtra("NAMA",  nama)
                    putExtra("EMAIL", email)
                })
                finish()
            }
            .addOnFailureListener {
                setLoading(false)
                Toast.makeText(this, "Gagal simpan data. Coba lagi.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupTombolMasuk() {
        binding.btnMasuk.setOnClickListener {
            val email    = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (email.isEmpty()) {
                binding.etEmail.error = "Email wajib diisi"; binding.etEmail.requestFocus(); return@setOnClickListener
            }
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.etEmail.error = "Format email tidak valid"; binding.etEmail.requestFocus(); return@setOnClickListener
            }
            if (password.isEmpty()) {
                binding.etPassword.error = "Password wajib diisi"; binding.etPassword.requestFocus(); return@setOnClickListener
            }

            setLoading(true)
            FirebaseHelper.login(
                email     = email,
                password  = password,
                onSuccess = { uid -> cekStatusDanNavigate(uid) },
                onError   = { pesan ->
                    setLoading(false)
                    val pesanRamah = when {
                        pesan.contains("password", ignoreCase = true) -> "Password salah. Coba lagi."
                        pesan.contains("no user",  ignoreCase = true) -> "Email tidak terdaftar."
                        pesan.contains("network",  ignoreCase = true) -> "Tidak ada koneksi internet."
                        else -> "Login gagal. Periksa email & password kamu."
                    }
                    Toast.makeText(this, pesanRamah, Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    /**
     * Setelah auth berhasil, cek Firestore untuk tentukan ke mana navigate:
     * - Belum pilih role            → RoleSelectionActivity
     * - Pengelola                   → DashboardPengelolaActivity
     * - Guru/OrangTua + verified    → Dashboard masing-masing
     * - Guru/OrangTua + not verified→ WaitingVerificationActivity
     * - Akun ditolak                → Toast + logout
     */
    private fun cekStatusDanNavigate(uid: String) {
        FirebaseHelper.db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                setLoading(false)
                val role       = doc.getString("role")          ?: ""
                val isVerified = doc.getBoolean("is_verified")  ?: false
                val statusAkun = doc.getString("status_akun")   ?: "aktif"

                when {
                    role.isEmpty() -> {
                        startActivity(Intent(this, RoleSelectionActivity::class.java).apply {
                            putExtra("UID", uid)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        })
                        finish()
                    }
                    statusAkun == "ditolak" -> {
                        val alasan = doc.getString("alasan_tolak") ?: ""
                        val pesan  = if (alasan.isNotEmpty()) "Akun ditolak: $alasan" else "Akun kamu ditolak oleh pengelola."
                        Toast.makeText(this, pesan, Toast.LENGTH_LONG).show()
                        FirebaseHelper.logout()
                    }
                    statusAkun == "nonaktif" -> {
                        Toast.makeText(this, "Akun kamu dinonaktifkan. Hubungi pengelola.", Toast.LENGTH_LONG).show()
                        FirebaseHelper.logout()
                    }
                    role == "pengelola" -> {
                        startActivity(Intent(this, DashboardPengelolaActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        })
                        finish()
                    }
                    isVerified -> {
                        // Guru atau orang tua yang sudah diverifikasi pengelola
                        val dest = when (role) {
                            "orang_tua" -> DashboardOrangTuaActivity::class.java
                            "guru"      -> DashboardGuruActivity::class.java
                            else        -> LoginActivity::class.java
                        }
                        startActivity(Intent(this, dest).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        })
                        finish()
                    }
                    else -> {
                        // Belum diverifikasi pengelola — masuk ke waiting
                        startActivity(Intent(this, WaitingVerificationActivity::class.java).apply {
                            putExtra("ROLE", role)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        })
                        finish()
                    }
                }
            }
            .addOnFailureListener { e ->
                setLoading(false)
                Toast.makeText(this, "Gagal cek status: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupLupaPassword() {
        binding.tvForgotPassword.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            if (email.isEmpty()) {
                binding.etEmail.error = "Masukkan email kamu dulu"
                binding.etEmail.requestFocus(); return@setOnClickListener
            }
            FirebaseHelper.resetPassword(
                email     = email,
                onSuccess = { Toast.makeText(this, "Link reset password dikirim ke $email", Toast.LENGTH_LONG).show() },
                onError   = { Toast.makeText(this, "Email tidak ditemukan.", Toast.LENGTH_SHORT).show() }
            )
        }
    }

    private fun setupDaftarLink() {
        binding.tvDaftarSekarang.setOnClickListener {
            startActivity(Intent(this, RegistrationActivity::class.java))
        }
    }

    private fun setupTombolBack() {
        binding.btnBack.setOnClickListener { finish() }
    }

    private fun setLoading(loading: Boolean) {
        binding.btnMasuk.isEnabled  = !loading
        binding.btnGoogle.isEnabled = !loading
        binding.btnMasuk.text  = if (loading) "Memproses..." else getString(R.string.masuk)
        binding.btnGoogle.text = if (loading) "Memproses..." else "Masuk dengan Google"
    }
}