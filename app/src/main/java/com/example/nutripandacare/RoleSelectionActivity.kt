package com.example.nutripandacare

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class RoleSelectionActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_NAMA     = "extra_nama"
        const val EXTRA_EMAIL    = "extra_email"
        const val EXTRA_WHATSAPP = "extra_whatsapp"
        const val EXTRA_PASSWORD = "extra_password"
        const val EXTRA_ROLE     = "extra_role"

        const val ROLE_ORANG_TUA = "Orang Tua"
        const val ROLE_GURU      = "Guru"
        const val ROLE_PENGELOLA = "Pengelola MBG"
    }

    // Views sesuai ID di activity_role_selection.xml
    private lateinit var btnBack: ImageButton
    private lateinit var cardOrangTua: LinearLayout
    private lateinit var cardGuru: LinearLayout
    private lateinit var cardPengelola: LinearLayout
    private lateinit var radioOrangTua: View
    private lateinit var radioGuru: View
    private lateinit var radioPengelola: View
    private lateinit var btnLanjutkan: Button

    // Data diterima dari RegistrationActivity
    private var nama     = ""
    private var email    = ""
    private var whatsapp = ""
    private var password = ""

    // Role yang sedang dipilih user
    private var selectedRole = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_role_selection)

        // Terima data dari RegistrationActivity
        intent?.let {
            nama     = it.getStringExtra(EXTRA_NAMA)     ?: ""
            email    = it.getStringExtra(EXTRA_EMAIL)    ?: ""
            whatsapp = it.getStringExtra(EXTRA_WHATSAPP) ?: ""
            password = it.getStringExtra(EXTRA_PASSWORD) ?: ""
        }

        initViews()
        setupClickListeners()
    }

    private fun initViews() {
        btnBack       = findViewById(R.id.btnBack)
        cardOrangTua  = findViewById(R.id.cardOrangTua)
        cardGuru      = findViewById(R.id.cardGuru)
        cardPengelola = findViewById(R.id.cardPengelola)
        radioOrangTua  = findViewById(R.id.radioOrangTua)
        radioGuru      = findViewById(R.id.radioGuru)
        radioPengelola = findViewById(R.id.radioPengelola)
        btnLanjutkan  = findViewById(R.id.btnLanjutkan)

        // Tombol lanjutkan disabled dulu sampai user pilih role
        setLanjutkanEnabled(false)
    }

    private fun setupClickListeners() {

        btnBack.setOnClickListener {
            @Suppress("DEPRECATION")
            onBackPressed()
        }

        cardOrangTua.setOnClickListener {
            selectRole(ROLE_ORANG_TUA)
        }

        cardGuru.setOnClickListener {
            selectRole(ROLE_GURU)
        }

        cardPengelola.setOnClickListener {
            selectRole(ROLE_PENGELOLA)
        }

        btnLanjutkan.setOnClickListener {
            if (selectedRole.isNotEmpty()) {
                performRegistration()
            } else {
                Toast.makeText(this, "Silakan pilih peran terlebih dahulu", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ─── Logika pemilihan role ────────────────────────────────────────────────

    private fun selectRole(role: String) {
        selectedRole = role
        resetAllCards()

        when (role) {
            ROLE_ORANG_TUA -> {
                cardOrangTua.background = ContextCompat.getDrawable(this, R.drawable.bg_role_card_selected)
                radioOrangTua.background = ContextCompat.getDrawable(this, R.drawable.bg_success_circle)
                animatePick(cardOrangTua)
            }
            ROLE_GURU -> {
                cardGuru.background = ContextCompat.getDrawable(this, R.drawable.bg_role_card_selected)
                radioGuru.background = ContextCompat.getDrawable(this, R.drawable.bg_success_circle)
                animatePick(cardGuru)
            }
            ROLE_PENGELOLA -> {
                cardPengelola.background = ContextCompat.getDrawable(this, R.drawable.bg_role_card_selected)
                radioPengelola.background = ContextCompat.getDrawable(this, R.drawable.bg_success_circle)
                animatePick(cardPengelola)
            }
        }

        setLanjutkanEnabled(true)
    }

    private fun resetAllCards() {
        val defaultCard  = ContextCompat.getDrawable(this, R.drawable.bg_role_card)
        val defaultRadio = ContextCompat.getDrawable(this, R.drawable.bg_clock_circle)

        cardOrangTua.background  = defaultCard
        cardGuru.background      = defaultCard
        cardPengelola.background = defaultCard

        radioOrangTua.background  = defaultRadio
        radioGuru.background      = defaultRadio
        radioPengelola.background = defaultRadio
    }

    /** Animasi bounce kecil saat kartu dipilih */
    private fun animatePick(card: View) {
        card.animate()
            .scaleX(0.96f).scaleY(0.96f)
            .setDuration(90)
            .withEndAction {
                card.animate()
                    .scaleX(1f).scaleY(1f)
                    .setDuration(90)
                    .start()
            }
            .start()
    }

    private fun setLanjutkanEnabled(enabled: Boolean) {
        btnLanjutkan.isEnabled = enabled
        btnLanjutkan.alpha     = if (enabled) 1.0f else 0.5f
    }

    // ─── Proses registrasi ────────────────────────────────────────────────────

    private fun performRegistration() {
        // Tampilkan loading state di tombol
        setLanjutkanEnabled(false)
        btnLanjutkan.text = getString(R.string.mendaftar)

        // Simulasi API call — ganti dengan Retrofit/Ktor di production
        Handler(Looper.getMainLooper()).postDelayed({
            btnLanjutkan.text = getString(R.string.lanjutkan)
            setLanjutkanEnabled(true)

            when (selectedRole) {
                ROLE_GURU, ROLE_PENGELOLA -> navigateToWaitingVerification()
                ROLE_ORANG_TUA            -> navigateToSuccess()
            }
        }, 1500)
    }

    // ─── Navigasi ─────────────────────────────────────────────────────────────

    private fun navigateToWaitingVerification() {
        val intent = Intent(this, WaitingVerificationActivity::class.java).apply {
            putExtra(WaitingVerificationActivity.EXTRA_ROLE,  selectedRole)
            putExtra(WaitingVerificationActivity.EXTRA_NAMA,  nama)
            putExtra(WaitingVerificationActivity.EXTRA_EMAIL, email)
        }
        startActivity(intent)
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
    }

    private fun navigateToSuccess() {
        val intent = Intent(this, SuccessActivity::class.java).apply {
            putExtra(SuccessActivity.EXTRA_NAMA,  nama)
            putExtra(SuccessActivity.EXTRA_ROLE,  selectedRole)
            putExtra(SuccessActivity.EXTRA_EMAIL, email)
            putExtra(SuccessActivity.EXTRA_PHONE, whatsapp)
        }
        startActivity(intent)
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
    }
}