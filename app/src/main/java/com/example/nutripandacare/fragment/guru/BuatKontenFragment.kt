package com.example.nutripandacare.fragment.guru

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
import java.util.UUID

class BuatKontenFragment : Fragment() {

    private var _binding: FragmentBuatKontenBinding? = null
    private val binding get() = _binding!!

    private var thumbnailUri: Uri? = null
    private var cameraImageUri: Uri? = null
    private var currentThumbnailUrl: String? = null
    private var isLoading = false
    private var artikelId: String? = null

    private val kategoriList = listOf("Pilih kategori...") + FirebaseHelper.KATEGORI_ARTIKEL

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            thumbnailUri = it
            binding.ivThumbnailPreview.setImageURI(it)
            binding.ivThumbnailPreview.visibility = View.VISIBLE
            binding.layoutUploadThumbnail.visibility = View.GONE
        }
    }

    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && cameraImageUri != null) {
            thumbnailUri = cameraImageUri
            binding.ivThumbnailPreview.setImageURI(cameraImageUri)
            binding.ivThumbnailPreview.visibility = View.VISIBLE
            binding.layoutUploadThumbnail.visibility = View.GONE
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentBuatKontenBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        artikelId = arguments?.getString("artikel_id")

        setupSpinner()
        setupClickListeners()

        binding.toolbar.setNavigationOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }

        if (artikelId != null) {
            binding.toolbar.title = "Edit Konten"
            binding.btnPublish.text = "Simpan Perubahan"
            loadArtikelUntukEdit(artikelId!!)
        }
    }

    private fun setupSpinner() {
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, kategoriList)
            .also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        binding.spinnerKategoriKonten.adapter = adapter
    }

    private fun setupClickListeners() {
        binding.layoutUploadThumbnail.setOnClickListener { showImageSourceDialog() }
        binding.ivThumbnailPreview.setOnClickListener { showImageSourceDialog() }
        binding.btnPublish.setOnClickListener { publishKonten() }
        binding.btnPreview.setOnClickListener { showPreviewDialog() }
    }

    private fun showPreviewDialog() {
        val judul = binding.etJudul.text.toString().trim()
        val isi = binding.etIsiKonten.text.toString().trim()
        val kategori = binding.spinnerKategoriKonten.selectedItem.toString()

        if (judul.isEmpty() || isi.isEmpty() || kategori == kategoriList[0]) {
            Toast.makeText(requireContext(), "Lengkapi judul, isi, dan kategori untuk preview", Toast.LENGTH_SHORT).show()
            return
        }

        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.fragment_preview_konten, null)
        val tvJudul = dialogView.findViewById<TextView>(R.id.tvJudul)
        val tvIsi = dialogView.findViewById<TextView>(R.id.tvIsiKonten)
        val ivThumb = dialogView.findViewById<ImageView>(R.id.ivThumbnail)
        val tvKat = dialogView.findViewById<TextView>(R.id.tvKategori)

        tvJudul.text = judul
        tvIsi.text = isi
        tvKat.text = kategori
        
        if (thumbnailUri != null) {
            ivThumb.setImageURI(thumbnailUri)
        } else if (!currentThumbnailUrl.isNullOrEmpty()) {
            com.bumptech.glide.Glide.with(this).load(currentThumbnailUrl).into(ivThumb)
        }

        AlertDialog.Builder(requireContext(), android.R.style.Theme_Material_Light_NoActionBar_Fullscreen)
            .setView(dialogView)
            .setPositiveButton("Tutup", null)
            .show()
    }

    private fun publishKonten() {
        if (isLoading) return
        val judul = binding.etJudul.text.toString().trim()
        val katPos = binding.spinnerKategoriKonten.selectedItemPosition
        val deskripsi = binding.etDeskripsiKonten.text.toString().trim()
        val isi = binding.etIsiKonten.text.toString().trim()
        val menit = binding.etMenitBaca.text.toString().toIntOrNull() ?: 5

        if (judul.isEmpty() || katPos == 0 || isi.isEmpty()) {
            Toast.makeText(requireContext(), "Harap lengkapi semua data", Toast.LENGTH_SHORT).show()
            return
        }

        isLoading = true
        setLoading(true)

        if (thumbnailUri != null) {
            uploadThumbnail(thumbnailUri!!) { url ->
                if (artikelId != null) editArtikel(judul, kategoriList[katPos], deskripsi, isi, menit, url)
                else simpanArtikel(judul, kategoriList[katPos], deskripsi, isi, menit, url)
            }
        } else {
            if (artikelId != null) editArtikel(judul, kategoriList[katPos], deskripsi, isi, menit, null)
            else simpanArtikel(judul, kategoriList[katPos], deskripsi, isi, menit, null)
        }
    }

    private fun uploadThumbnail(uri: Uri, onComplete: (String?) -> Unit) {
        val fileName = UUID.randomUUID().toString()
        val ref = FirebaseStorage.getInstance().reference.child("thumbnail_artikel/$fileName.jpg")
        ref.putFile(uri).addOnSuccessListener {
            ref.downloadUrl.addOnSuccessListener { url -> onComplete(url.toString()) }.addOnFailureListener { onComplete(null) }
        }.addOnFailureListener { onComplete(null) }
    }

    private fun simpanArtikel(judul: String, kategori: String, deskripsi: String, isi: String, menit: Int, url: String?) {
        FirebaseHelper.tambahArtikel(judul, deskripsi, isi, kategori, url ?: "", menit,
            onSuccess = {
                Toast.makeText(requireContext(), "Konten berhasil dipublikasikan! 🎉", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            },
            onError = { Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show(); setLoading(false); isLoading = false }
        )
    }

    private fun editArtikel(judul: String, kategori: String, deskripsi: String, isi: String, menit: Int, url: String?) {
        val updates = mutableMapOf<String, Any>("judul" to judul, "kategori" to kategori, "deskripsi" to deskripsi, "isi_konten" to isi, "menit_baca" to menit)
        if (url != null) updates["thumbnail_url"] = url
        FirebaseHelper.updateArtikel(artikelId!!, updates,
            onSuccess = {
                Toast.makeText(requireContext(), "Berhasil diperbarui", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            },
            onError = { Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show(); setLoading(false); isLoading = false }
        )
    }

    private fun loadArtikelUntukEdit(id: String) {
        FirebaseHelper.getArtikelById(id, onSuccess = { data ->
            if (_binding == null) return@getArtikelById
            binding.etJudul.setText(data["judul"] as? String)
            binding.etDeskripsiKonten.setText(data["deskripsi"] as? String)
            binding.etIsiKonten.setText(data["isi_konten"] as? String)
            binding.etMenitBaca.setText((data["menit_baca"] as? Number)?.toString())
            currentThumbnailUrl = data["thumbnail_url"] as? String
            if (!currentThumbnailUrl.isNullOrEmpty()) {
                binding.ivThumbnailPreview.visibility = View.VISIBLE
                binding.layoutUploadThumbnail.visibility = View.GONE
                com.bumptech.glide.Glide.with(this).load(currentThumbnailUrl).into(binding.ivThumbnailPreview)
            }
        }, onError = {})
    }

    private fun showImageSourceDialog() {
        val options = arrayOf("📷 Kamera", "🖼️ Galeri")
        AlertDialog.Builder(requireContext()).setTitle("Pilih Gambar").setItems(options) { _, w ->
            if (w == 0) openCamera() else pickImageLauncher.launch("image/*")
        }.show()
    }

    private fun openCamera() {
        val photoFile = File(requireContext().cacheDir, "thumb_${System.currentTimeMillis()}.jpg")
        cameraImageUri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.fileprovider", photoFile)
        takePictureLauncher.launch(cameraImageUri)
    }

    private fun setLoading(loading: Boolean) {
        binding.btnPublish.isEnabled = !loading
        binding.btnPublish.text = if (loading) "Memproses..." else if (artikelId != null) "Simpan" else "Publish"
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}