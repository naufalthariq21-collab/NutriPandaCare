package com.example.nutripandacare.guru

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.nutripandacare.R
import com.example.nutripandacare.firebase.FirebaseHelper
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.Timestamp
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID

class BuatKontenActivity : AppCompatActivity() {

    // ─── Views ───────────────────────────────────────────────────
    private lateinit var ivBack: ImageView
    private lateinit var etJudul: EditText
    private lateinit var spinnerKategori: Spinner
    private lateinit var etDeskripsi: EditText
    private lateinit var etIsiKonten: EditText
    private lateinit var etMenitBaca: EditText
    private lateinit var layoutUploadThumbnail: LinearLayout
    private lateinit var ivThumbnailPreview: ImageView
    private lateinit var btnPublish: Button
    private lateinit var bottomNavigation: BottomNavigationView

    // ─── State ───────────────────────────────────────────────────
    private var thumbnailUri: Uri? = null
    private var isLoading = false

    private val kategoriList = listOf("Pilih kategori...", "Stunting", "Resep", "Vitamin", "MPASI")

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                thumbnailUri = it
                ivThumbnailPreview.setImageURI(it)
                ivThumbnailPreview.visibility    = android.view.View.VISIBLE
                layoutUploadThumbnail.visibility = android.view.View.GONE
            }
        }

    // ═════════════════════════════════════════════════════════════
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_buat_konten)

        initViews()
        setupSpinner()
        setupClickListeners()
        setupBottomNavigation()
    }

    private fun initViews() {
        ivBack                = findViewById(R.id.ivBack)
        etJudul               = findViewById(R.id.etJudul)
        spinnerKategori       = findViewById(R.id.spinnerKategoriKonten)
        etDeskripsi           = findViewById(R.id.etDeskripsiKonten)
        etIsiKonten           = findViewById(R.id.etIsiKonten)
        etMenitBaca           = findViewById(R.id.etMenitBaca)
        layoutUploadThumbnail = findViewById(R.id.layoutUploadThumbnail)
        ivThumbnailPreview    = findViewById(R.id.ivThumbnailPreview)
        btnPublish            = findViewById(R.id.btnPublish)
        bottomNavigation      = findViewById(R.id.bottomNavigation)
    }

    private fun setupSpinner() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, kategoriList)
            .also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        spinnerKategori.adapter = adapter
    }

    private fun setupClickListeners() {
        ivBack.setOnClickListener { finish() }

        layoutUploadThumbnail.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        btnPublish.setOnClickListener { publishKonten() }
    }

    // ─── Publish Konten ───────────────────────────────────────────
    private fun publishKonten() {
        if (isLoading) return

        val judul     = etJudul.text.toString().trim()
        val katPos    = spinnerKategori.selectedItemPosition
        val deskripsi = etDeskripsi.text.toString().trim()
        val isi       = etIsiKonten.text.toString().trim()
        val menitStr  = etMenitBaca.text.toString().trim()

        if (judul.isEmpty())       { etJudul.error = "Judul wajib diisi"; return }
        if (katPos == 0)           { Toast.makeText(this, "Pilih kategori", Toast.LENGTH_SHORT).show(); return }
        if (deskripsi.isEmpty())   { etDeskripsi.error = "Deskripsi wajib diisi"; return }
        if (isi.isEmpty())         { etIsiKonten.error = "Isi konten wajib diisi"; return }

        val menit    = menitStr.toIntOrNull() ?: 5
        val kategori = kategoriList[katPos]

        isLoading       = true
        btnPublish.isEnabled = false
        btnPublish.text = "Mempublikasikan..."

        if (thumbnailUri != null) {
            uploadThumbnail(thumbnailUri!!) { url ->
                simpanArtikel(judul, kategori, deskripsi, isi, menit, url)
            }
        } else {
            simpanArtikel(judul, kategori, deskripsi, isi, menit, null)
        }
    }

    private fun uploadThumbnail(uri: Uri, onComplete: (String?) -> Unit) {
        val artikelId = UUID.randomUUID().toString()
        val ref       = FirebaseStorage.getInstance().reference
            .child("thumbnail_artikel/$artikelId.jpg")

        ref.putFile(uri)
            .continueWithTask { ref.downloadUrl }
            .addOnSuccessListener  { url -> onComplete(url.toString()) }
            .addOnFailureListener  { onComplete(null) }
    }

    private fun simpanArtikel(
        judul: String, kategori: String, deskripsi: String,
        isi: String, menit: Int, thumbnailUrl: String?
    ) {
        FirebaseHelper.tambahArtikel(
            judul         = judul,
            deskripsi     = deskripsi,
            isiKonten     = isi,
            kategori      = kategori,
            thumbnailUrl  = thumbnailUrl ?: "",
            menitBaca     = menit,
            penulis       = "Tim NutriPandaCare",
            onSuccess     = { _ ->
                isLoading = false
                Toast.makeText(this, "Konten berhasil dipublikasikan!", Toast.LENGTH_SHORT).show()
                finish()
            },
            onError = { err ->
                isLoading       = false
                btnPublish.isEnabled = true
                btnPublish.text = "Publish"
                Toast.makeText(this, err, Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home    -> { startActivity(Intent(this, DashboardGuruActivity::class.java)); true }
                R.id.nav_laporan -> { startActivity(Intent(this, LaporanMbgActivity::class.java)); true }
                R.id.nav_konten  -> { finish(); true }
                else             -> false
            }
        }
    }
}