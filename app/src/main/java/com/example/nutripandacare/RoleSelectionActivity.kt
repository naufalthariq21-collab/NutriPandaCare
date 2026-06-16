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

    // GAP-5 FIX: sebelumnya radioGuru dan radioPengelola di-reset ke bg_clock_circle,
    // sedangkan radioOrangTua ke bg_role_icon — inkonsisten dan rawan visual bug.
    // Sekarang semua radio di-reset ke bg_clock_circle (drawable default radio state),
    // lalu hanya yang terpilih yang berubah ke bg_success_circle. UAT #23 terpenuhi:
    // "visual kartu berubah sesuai peran yang terakhir dipilih; hanya satu peran aktif".
    private fun updateTampilanRole() {
        // Reset semua kartu ke state default
        binding.cardOrangTua.background  = ContextCompat.getDrawable(this, R.drawable.selector_role_card)
        binding.cardGuru.background      = ContextCompat.getDrawable(this, R.drawable.selector_role_card)
        binding.cardPengelola.background = ContextCompat.getDrawable(this, R.drawable.selector_role_card)

        // Reset semua radio icon ke drawable default (konsisten untuk ketiganya)
        binding.radioOrangTua.background  = ContextCompat.getDrawable(this, R.drawable.bg_clock_circle)
        binding.radioGuru.background      = ContextCompat.getDrawable(this, R.drawable.bg_clock_circle)
        binding.radioPengelola.background = ContextCompat.getDrawable(this, R.drawable.bg_clock_circle)

        // Highlight hanya kartu & radio yang terpilih
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
            // UAT #19: tombol Lanjutkan tanpa pilih peran → toast peringatan
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

            // UAT #22: pengelola → langsung dashboard (is_verified = true)
            // UAT #20/#21: guru/orang_tua → WaitingVerification (is_verified = false)
            FirebaseHelper.simpanRole(
                uid        = uid,
                role       = roleTerpilih,
                isVerified = (roleTerpilih == "pengelola"),
                onSuccess  = {
                    setLoading(false)

                    if (roleTerpilih == "pengelola") {
                        startActivity(Intent(this, DashboardPengelolaActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        })
                    } else {
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
