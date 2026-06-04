package com.example.nutripandacare

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.nutripandacare.databinding.ActivityRoleSelectionBinding
import com.example.nutripandacare.firebase.FirebaseHelper

class RoleSelectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRoleSelectionBinding
    private var roleTerpilih = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRoleSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupPilihRole()
        setupTombolLanjutkan()
        setupTombolBack()
    }

    private fun setupPilihRole() {
        binding.cardOrangTua.setOnClickListener  { roleTerpilih = "orang_tua"; updateTampilanRole() }
        binding.cardGuru.setOnClickListener      { roleTerpilih = "guru";      updateTampilanRole() }
        binding.cardPengelola.setOnClickListener { roleTerpilih = "pengelola"; updateTampilanRole() }
    }

    private fun updateTampilanRole() {
        // Reset semua ke default
        binding.cardOrangTua.background  = ContextCompat.getDrawable(this, R.drawable.selector_role_card)
        binding.cardGuru.background      = ContextCompat.getDrawable(this, R.drawable.selector_role_card)
        binding.cardPengelola.background = ContextCompat.getDrawable(this, R.drawable.selector_role_card)
        binding.radioOrangTua.background = ContextCompat.getDrawable(this, R.drawable.bg_role_icon)
        binding.radioGuru.background     = ContextCompat.getDrawable(this, R.drawable.bg_clock_circle)
        binding.radioPengelola.background = ContextCompat.getDrawable(this, R.drawable.bg_clock_circle)

        // Highlight yang dipilih
        when (roleTerpilih) {
            "orang_tua" -> {
                binding.cardOrangTua.background  = ContextCompat.getDrawable(this, R.drawable.bg_role_card_selected)
                binding.radioOrangTua.background = ContextCompat.getDrawable(this, R.drawable.bg_success_circle)
            }
            "guru" -> {
                binding.cardGuru.background  = ContextCompat.getDrawable(this, R.drawable.bg_role_card_selected)
                binding.radioGuru.background = ContextCompat.getDrawable(this, R.drawable.bg_success_circle)
            }
            "pengelola" -> {
                binding.cardPengelola.background  = ContextCompat.getDrawable(this, R.drawable.bg_role_card_selected)
                binding.radioPengelola.background = ContextCompat.getDrawable(this, R.drawable.bg_success_circle)
            }
        }
    }

    private fun setupTombolLanjutkan() {
        binding.btnLanjutkan.setOnClickListener {
            if (roleTerpilih.isEmpty()) {
                Toast.makeText(this, "Pilih peran kamu dulu!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val uid   = intent.getStringExtra("UID")
                ?: FirebaseHelper.auth.currentUser?.uid
                ?: return@setOnClickListener
            val nama  = intent.getStringExtra("NAMA")  ?: ""
            val email = intent.getStringExtra("EMAIL") ?: ""
            val noHp  = intent.getStringExtra("NO_HP") ?: ""

            setLoading(true)

            // Pengelola: simpan role + langsung is_verified = true
            // Guru/OrangTua: simpan role + is_verified = false (menunggu pengelola)
            FirebaseHelper.simpanRole(
                uid        = uid,
                role       = roleTerpilih,
                isVerified = (roleTerpilih == "pengelola"),
                onSuccess  = {
                    setLoading(false)

                    if (roleTerpilih == "pengelola") {
                        // Langsung masuk dashboard pengelola
                        startActivity(Intent(this, DashboardPengelolaActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        })
                    } else {
                        // Guru / Orang Tua → tunggu verifikasi pengelola
                        startActivity(Intent(this, WaitingVerificationActivity::class.java).apply {
                            putExtra("ROLE",  roleTerpilih)
                            putExtra("NAMA",  nama)
                            putExtra("EMAIL", email)
                            putExtra("NO_HP", noHp)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        })
                    }
                    finish()
                },
                onError = {
                    setLoading(false)
                    Toast.makeText(this, "Gagal menyimpan role. Coba lagi.", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    private fun setupTombolBack() {
        binding.btnBack.setOnClickListener { finish() }
    }

    private fun setLoading(loading: Boolean) {
        binding.btnLanjutkan.isEnabled = !loading
        binding.btnLanjutkan.text = if (loading) "Menyimpan..." else getString(R.string.lanjutkan)
    }
}