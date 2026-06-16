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
import com.google.firebase.Timestamp

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
                    // Langsung fetch Firestore — 1x saja
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
            "is_verified" to false,
            "status_akun" to "aktif",
            "created_at"  to Timestamp.now(),
            "updated_at"  to Timestamp.now()
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

            // Validasi input sebelum loading dimulai
            if (email.isEmpty()) {
                binding.etEmail.error = "Email wajib diisi"
                binding.etEmail.requestFocus()
                return@setOnClickListener
            }
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.etEmail.error = "Format email tidak valid"
                binding.etEmail.requestFocus()
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                binding.etPassword.error = "Password wajib diisi"
                binding.etPassword.requestFocus()
                return@setOnClickListener
            }

            setLoading(true)

            // FIX UTAMA: langsung signIn Firebase Auth, lalu fetch Firestore 1x saja
            // Sebelumnya: FirebaseHelper.login() → onSuccess → cekStatusDanNavigate() (2 network calls)
            // Sekarang: auth.signIn() → onSuccess → cekStatusDanNavigate() (1 auth + 1 Firestore = lebih cepat)
            FirebaseHelper.auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener { result ->
                    val uid = result.user?.uid ?: run {
                        setLoading(false)
                        Toast.makeText(this, "Login gagal. Coba lagi.", Toast.LENGTH_SHORT).show()
                        return@addOnSuccessListener
                    }
                    // Langsung cek Firestore — hanya 1x fetch
                    cekStatusDanNavigate(uid)
                }
                .addOnFailureListener { e ->
                    setLoading(false)
                    val pesanRamah = when {
                        e.message?.contains("password",  ignoreCase = true) == true ||
                                e.message?.contains("WRONG",     ignoreCase = true) == true ||
                                e.message?.contains("invalid",   ignoreCase = true) == true ->
                            "Password salah. Coba lagi."

                        e.message?.contains("no user",   ignoreCase = true) == true ||
                                e.message?.contains("USER_NOT",  ignoreCase = true) == true ||
                                e.message?.contains("no record", ignoreCase = true) == true ->
                            "Email tidak terdaftar."

                        e.message?.contains("network",   ignoreCase = true) == true ||
                                e.message?.contains("connection",ignoreCase = true) == true ->
                            "Tidak ada koneksi internet."

                        else -> "Login gagal. Periksa email & password kamu."
                    }
                    Toast.makeText(this, pesanRamah, Toast.LENGTH_LONG).show()
                }
        }
    }

    /**
     * Satu-satunya Firestore fetch setelah auth berhasil.
     * Tentukan navigasi berdasarkan role, is_verified, status_akun.
     *
     * - role kosong      → RoleSelectionActivity
     * - status ditolak   → Toast + logout
     * - status nonaktif  → Toast + logout
     * - pengelola        → DashboardPengelolaActivity
     * - verified         → Dashboard sesuai role
     * - belum verified   → WaitingVerificationActivity
     */
    private fun cekStatusDanNavigate(uid: String) {
        FirebaseHelper.db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                setLoading(false)

                if (!doc.exists()) {
                    Toast.makeText(this, "Data akun tidak ditemukan.", Toast.LENGTH_SHORT).show()
                    FirebaseHelper.logout()
                    return@addOnSuccessListener
                }

                val role       = doc.getString("role")         ?: ""
                val isVerified = doc.getBoolean("is_verified") ?: false
                val statusAkun = doc.getString("status_akun")  ?: "aktif"

                when {
                    // Belum pilih role → ke RoleSelection
                    role.isEmpty() -> {
                        startActivity(Intent(this, RoleSelectionActivity::class.java).apply {
                            putExtra("UID", uid)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        })
                        finish()
                    }

                    // Akun ditolak → tampilkan alasan, logout
                    statusAkun == "ditolak" -> {
                        val alasan = doc.getString("alasan_tolak") ?: ""
                        val pesan  = if (alasan.isNotEmpty()) "Akun ditolak: $alasan"
                        else "Akun kamu ditolak oleh pengelola."
                        Toast.makeText(this, pesan, Toast.LENGTH_LONG).show()
                        FirebaseHelper.logout()
                    }

                    // Akun nonaktif → tidak bisa login
                    statusAkun == "nonaktif" -> {
                        Toast.makeText(
                            this,
                            "Akun kamu dinonaktifkan oleh pengelola. Hubungi admin.",
                            Toast.LENGTH_LONG
                        ).show()
                        FirebaseHelper.logout()
                    }

                    // Pengelola langsung masuk
                    role == "pengelola" -> {
                        navigateTo(DashboardPengelolaActivity::class.java)
                    }

                    // Guru/OrangTua sudah diverifikasi
                    isVerified -> {
                        val dest = when (role) {
                            "orang_tua" -> DashboardOrangTuaActivity::class.java
                            "guru"      -> DashboardGuruActivity::class.java
                            else        -> {
                                Toast.makeText(this, "Role tidak dikenal.", Toast.LENGTH_SHORT).show()
                                FirebaseHelper.logout()
                                return@addOnSuccessListener
                            }
                        }
                        navigateTo(dest)
                    }

                    // Belum diverifikasi → tunggu verifikasi
                    else -> {
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
                FirebaseHelper.logout()
            }
    }

    private fun <T> navigateTo(dest: Class<T>) {
        startActivity(Intent(this, dest).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }

    /**
     * Lupa Password — UAT A-9 & A-10.
     * A-9 : kirim email reset → Toast konfirmasi
     * A-10: email kosong → error di field, tidak kirim
     */
    private fun setupLupaPassword() {
        binding.tvForgotPassword.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()

            // UAT A-10: email kosong → error field, jangan kirim
            if (email.isEmpty()) {
                binding.etEmail.error = "Masukkan email kamu dulu"
                binding.etEmail.requestFocus()
                return@setOnClickListener
            }
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.etEmail.error = "Format email tidak valid"
                binding.etEmail.requestFocus()
                return@setOnClickListener
            }

            // UAT A-9: kirim reset password
            FirebaseHelper.resetPassword(
                email     = email,
                onSuccess = {
                    Toast.makeText(
                        this,
                        "Link reset password dikirim ke $email. Cek inbox kamu.",
                        Toast.LENGTH_LONG
                    ).show()
                },
                onError = { pesan ->
                    val pesanRamah = when {
                        pesan.contains("no user",         ignoreCase = true) ||
                                pesan.contains("USER_NOT",        ignoreCase = true) ||
                                pesan.contains("no record",       ignoreCase = true) ||
                                pesan.contains("not found",       ignoreCase = true) ->
                            "Email tidak ditemukan. Pastikan email sudah terdaftar."

                        pesan.contains("network",         ignoreCase = true) ||
                                pesan.contains("connection",      ignoreCase = true) ->
                            "Tidak ada koneksi internet. Coba lagi."

                        pesan.contains("badly formatted", ignoreCase = true) ||
                                pesan.contains("invalid email",   ignoreCase = true) ->
                            "Format email tidak valid."

                        else -> "Gagal mengirim email reset. Coba lagi."
                    }
                    Toast.makeText(this, pesanRamah, Toast.LENGTH_LONG).show()
                }
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
        binding.tvForgotPassword.isEnabled = !loading
        binding.btnMasuk.text  = if (loading) "Memproses..." else getString(R.string.masuk)
        binding.btnGoogle.text = if (loading) "Memproses..." else "Masuk dengan Google"
    }
}