package com.example.nutripandacare.fragment.guru

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.example.nutripandacare.R
import com.example.nutripandacare.databinding.FragmentBuatKontenBinding
import com.example.nutripandacare.firebase.FirebaseHelper
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID

class BuatKontenFragment : Fragment() {

    private var _binding: FragmentBuatKontenBinding? = null
    private val binding get() = _binding!!

    private var thumbnailUri: Uri? = null
    private var isLoading = false

    private val kategoriList = listOf("Pilih kategori...", "Stunting", "Resep", "Vitamin", "MPASI")

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                thumbnailUri = it
                binding.ivThumbnailPreview.setImageURI(it)
                binding.ivThumbnailPreview.visibility   = View.VISIBLE
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

        setupSpinner()
        setupClickListeners()

        binding.toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupSpinner() {
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, kategoriList)
            .also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        binding.spinnerKategoriKonten.adapter = adapter
    }

    private fun setupClickListeners() {
        binding.layoutUploadThumbnail.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }
        binding.btnPublish.setOnClickListener { publishKonten() }
    }

    private fun publishKonten() {
        if (isLoading) return

        val judul     = binding.etJudul.text.toString().trim()
        val katPos    = binding.spinnerKategoriKonten.selectedItemPosition
        val deskripsi = binding.etDeskripsiKonten.text.toString().trim()
        val isi       = binding.etIsiKonten.text.toString().trim()
        val menitStr  = binding.etMenitBaca.text.toString().trim()

        if (judul.isEmpty()) {
            binding.etJudul.error = "Judul wajib diisi"
            return
        }
        if (katPos == 0) {
            Toast.makeText(requireContext(), "Pilih kategori", Toast.LENGTH_SHORT).show()
            return
        }
        if (deskripsi.isEmpty()) {
            binding.etDeskripsiKonten.error = "Deskripsi wajib diisi"
            return
        }
        if (isi.isEmpty()) {
            binding.etIsiKonten.error = "Isi konten wajib diisi"
            return
        }

        val menit    = menitStr.toIntOrNull() ?: 5
        val kategori = kategoriList[katPos]

        isLoading = true
        binding.btnPublish.isEnabled = false
        binding.btnPublish.text = "Mempublikasikan..."

        if (thumbnailUri != null) {
            uploadThumbnail(thumbnailUri!!) { url ->
                // url bisa null jika upload gagal, lanjut simpan dengan url kosong
                simpanArtikel(judul, kategori, deskripsi, isi, menit, url)
            }
        } else {
            simpanArtikel(judul, kategori, deskripsi, isi, menit, null)
        }
    }

    private fun uploadThumbnail(uri: Uri, onComplete: (String?) -> Unit) {
        val artikelId = UUID.randomUUID().toString()
        val ref = FirebaseStorage.getInstance().reference
            .child("thumbnail_artikel/$artikelId.jpg")

        ref.putFile(uri)
            .addOnSuccessListener {
                // Ambil download URL setelah upload sukses
                ref.downloadUrl
                    .addOnSuccessListener { url -> onComplete(url.toString()) }
                    .addOnFailureListener { onComplete(null) }
            }
            .addOnFailureListener {
                // Upload gagal, tetap lanjut tanpa thumbnail
                Toast.makeText(requireContext(), "Gagal upload thumbnail, artikel tetap disimpan", Toast.LENGTH_SHORT).show()
                onComplete(null)
            }
    }

    private fun simpanArtikel(
        judul: String, kategori: String, deskripsi: String,
        isi: String, menit: Int, thumbnailUrl: String?
    ) {
        FirebaseHelper.tambahArtikel(
            judul        = judul,
            deskripsi    = deskripsi,
            isiKonten    = isi,
            kategori     = kategori,
            thumbnailUrl = thumbnailUrl ?: "",
            menitBaca    = menit,
            onSuccess    = { _ ->
                isLoading = false
                Toast.makeText(requireContext(), "Konten berhasil dipublikasikan!", Toast.LENGTH_SHORT).show()
                requireActivity().onBackPressedDispatcher.onBackPressed()
            },
            onError = { err ->
                isLoading = false
                binding.btnPublish.isEnabled = true
                binding.btnPublish.text = "Publish"
                Toast.makeText(requireContext(), err, Toast.LENGTH_SHORT).show()
            }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}