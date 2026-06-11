package com.example.nutripandacare.fragment.guru

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.nutripandacare.R
import com.example.nutripandacare.databinding.FragmentBuatKontenBinding
import com.example.nutripandacare.firebase.FirebaseHelper
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class BuatKontenFragment : Fragment() {

    private var _binding: FragmentBuatKontenBinding? = null
    private val binding get() = _binding!!

    private var thumbnailUri: Uri? = null
    private var cameraImageUri: Uri? = null
    private var isLoading = false
    private var artikelId: String? = null

    private val kategoriList = listOf("Pilih kategori...") + FirebaseHelper.KATEGORI_ARTIKEL

    // FIX: Salin ke cache agar content URI tidak kehilangan permission saat upload Storage
    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                val cached = copyUriToCache(requireContext(), it, "thumbnail")
                thumbnailUri = cached ?: it
                binding.ivThumbnailPreview.setImageURI(thumbnailUri)
                binding.ivThumbnailPreview.visibility    = View.VISIBLE
                binding.layoutUploadThumbnail.visibility = View.GONE
            }
        }

    private val takePictureLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success && cameraImageUri != null) {
                thumbnailUri = cameraImageUri
                binding.ivThumbnailPreview.setImageURI(cameraImageUri)
                binding.ivThumbnailPreview.visibility    = View.VISIBLE
                binding.layoutUploadThumbnail.visibility = View.GONE
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBuatKontenBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        artikelId = arguments?.getString("artikel_id")
        setupSpinner()
        binding.toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
        binding.layoutUploadThumbnail.setOnClickListener { showImageSourceDialog() }
        binding.ivThumbnailPreview.setOnClickListener    { showImageSourceDialog() }
        binding.btnPublish.setOnClickListener            { publishKonten() }

        if (artikelId != null) {
            binding.toolbar.title   = "Edit Konten"
            binding.btnPublish.text = "Simpan Perubahan"
            loadArtikelUntukEdit(artikelId!!)
        } else {
            binding.toolbar.title   = "Buat Konten Baru"
            binding.btnPublish.text = "Publish"
        }
    }

    private fun setupSpinner() {
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, kategoriList)
            .also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        binding.spinnerKategoriKonten.adapter = adapter
    }

    private fun loadArtikelUntukEdit(id: String) {
        binding.btnPublish.isEnabled = false
        FirebaseHelper.getArtikelById(id,
            onSuccess = { data ->
                if (_binding == null) return@getArtikelById
                binding.etJudul.setText(data["judul"] as? String ?: "")
                binding.etDeskripsiKonten.setText(data["deskripsi"] as? String ?: "")
                binding.etIsiKonten.setText(data["isi_konten"] as? String ?: "")
                binding.etMenitBaca.setText((data["menit_baca"] as? Number)?.toInt()?.toString() ?: "")
                val idx = kategoriList.indexOf(data["kategori"] as? String ?: "")
                if (idx >= 0) binding.spinnerKategoriKonten.setSelection(idx)
                val thumbUrl = data["thumbnail_url"] as? String ?: ""
                if (thumbUrl.isNotEmpty()) {
                    binding.ivThumbnailPreview.visibility    = View.VISIBLE
                    binding.layoutUploadThumbnail.visibility = View.GONE
                    com.bumptech.glide.Glide.with(this).load(thumbUrl).into(binding.ivThumbnailPreview)
                }
                binding.btnPublish.isEnabled = true
            },
            onError = { err ->
                if (_binding == null) return@getArtikelById
                Toast.makeText(requireContext(), "Gagal memuat artikel: $err", Toast.LENGTH_SHORT).show()
                binding.btnPublish.isEnabled = true
            }
        )
    }

    private fun showImageSourceDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Pilih Sumber Gambar")
            .setItems(arrayOf("📷  Ambil dari Kamera", "🖼️  Pilih dari Galeri")) { _, which ->
                when (which) {
                    0 -> openCamera()
                    1 -> pickImageLauncher.launch("image/*")
                }
            }
            .show()
    }

    private fun openCamera() {
        val photoFile = File(requireContext().cacheDir, "thumbnail_${System.currentTimeMillis()}.jpg")
        cameraImageUri = FileProvider.getUriForFile(
            requireContext(), "${requireContext().packageName}.fileprovider", photoFile
        )
        takePictureLauncher.launch(cameraImageUri)
    }

    private fun publishKonten() {
        if (isLoading) return
        val judul     = binding.etJudul.text.toString().trim()
        val katPos    = binding.spinnerKategoriKonten.selectedItemPosition
        val deskripsi = binding.etDeskripsiKonten.text.toString().trim()
        val isi       = binding.etIsiKonten.text.toString().trim()
        val menit     = binding.etMenitBaca.text.toString().toIntOrNull() ?: 5
        val kategori  = kategoriList[katPos]

        if (judul.isEmpty())     { binding.etJudul.error = "Judul wajib diisi"; return }
        if (katPos == 0)         { Toast.makeText(requireContext(), "Pilih kategori", Toast.LENGTH_SHORT).show(); return }
        if (deskripsi.isEmpty()) { binding.etDeskripsiKonten.error = "Deskripsi wajib diisi"; return }
        if (isi.isEmpty())       { binding.etIsiKonten.error = "Isi konten wajib diisi"; return }

        isLoading = true
        setLoading(true)

        if (thumbnailUri != null) {
            uploadThumbnail(thumbnailUri!!) { url ->
                if (artikelId != null) editArtikel(judul, kategori, deskripsi, isi, menit, url)
                else simpanArtikel(judul, kategori, deskripsi, isi, menit, url)
            }
        } else {
            if (artikelId != null) editArtikel(judul, kategori, deskripsi, isi, menit, null)
            else simpanArtikel(judul, kategori, deskripsi, isi, menit, null)
        }
    }

    // FIX: Pakai putBytes agar tidak ada SecurityException pada content URI
    private fun uploadThumbnail(uri: Uri, onComplete: (String?) -> Unit) {
        val ref = FirebaseStorage.getInstance().reference
            .child("thumbnail_artikel/${UUID.randomUUID()}.jpg")
        try {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            if (inputStream == null) { onComplete(null); return }
            val bytes = inputStream.readBytes()
            inputStream.close()

            ref.putBytes(bytes)
                .addOnSuccessListener {
                    ref.downloadUrl
                        .addOnSuccessListener { url -> onComplete(url.toString()) }
                        .addOnFailureListener { onComplete(null) }
                }
                .addOnFailureListener { e ->
                    if (_binding != null)
                        Toast.makeText(requireContext(), "Gagal upload thumbnail, konten tetap disimpan", Toast.LENGTH_SHORT).show()
                    onComplete(null)
                }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error baca foto: ${e.message}", Toast.LENGTH_SHORT).show()
            onComplete(null)
        }
    }

    private fun simpanArtikel(
        judul: String, kategori: String, deskripsi: String,
        isi: String, menit: Int, thumbnailUrl: String?
    ) {
        // FIX #3 & #4: tambahkan is_published = true agar muncul di orang tua
        FirebaseHelper.tambahArtikel(
            judul        = judul,
            deskripsi    = deskripsi,
            isiKonten    = isi,
            kategori     = kategori,
            thumbnailUrl = thumbnailUrl ?: "",
            menitBaca    = menit,
            onSuccess    = { newArtikelId ->
                isLoading = false
                if (_binding == null) return@tambahArtikel
                Toast.makeText(requireContext(), "Konten berhasil dipublikasikan! 🎉", Toast.LENGTH_SHORT).show()
                // FIX #8: navigate ke preview, back dari preview ke konten edukasi
                val args = Bundle().apply { putString("artikel_id", newArtikelId) }
                try {
                    findNavController().navigate(R.id.nav_preview_konten, args)
                } catch (e: Exception) {
                    findNavController().popBackStack(R.id.nav_konten_edukasi, false)
                }
            },
            onError = { err ->
                isLoading = false
                if (_binding == null) return@tambahArtikel
                setLoading(false)
                Toast.makeText(requireContext(), "Gagal publish: $err", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun editArtikel(
        judul: String, kategori: String, deskripsi: String,
        isi: String, menit: Int, thumbnailUrl: String?
    ) {
        val updates = mutableMapOf<String, Any>(
            "judul"      to judul,
            "kategori"   to kategori,
            "deskripsi"  to deskripsi,
            "isi_konten" to isi,
            "menit_baca" to menit
        )
        if (thumbnailUrl != null) updates["thumbnail_url"] = thumbnailUrl

        FirebaseHelper.updateArtikel(
            artikelId  = artikelId!!,
            dataUpdate = updates,
            onSuccess  = {
                isLoading = false
                if (_binding == null) return@updateArtikel
                Toast.makeText(requireContext(), "Konten berhasil diperbarui! ✅", Toast.LENGTH_SHORT).show()
                requireActivity().onBackPressedDispatcher.onBackPressed()
            },
            onError = { err ->
                isLoading = false
                if (_binding == null) return@updateArtikel
                setLoading(false)
                Toast.makeText(requireContext(), "Gagal update: $err", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun setLoading(loading: Boolean) {
        binding.btnPublish.isEnabled = !loading
        binding.btnPublish.text = when {
            loading           -> "Memproses..."
            artikelId != null -> "Simpan Perubahan"
            else              -> "Publish"
        }
    }

    private fun copyUriToCache(context: Context, uri: Uri, prefix: String): Uri? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val outFile = File(context.cacheDir, "${prefix}_${System.currentTimeMillis()}.jpg")
            FileOutputStream(outFile).use { out -> inputStream.copyTo(out) }
            inputStream.close()
            Uri.fromFile(outFile)
        } catch (e: Exception) {
            null
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}