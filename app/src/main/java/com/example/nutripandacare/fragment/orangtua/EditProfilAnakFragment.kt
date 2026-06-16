package com.example.nutripandacare.fragment.orangtua

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.nutripandacare.R
import com.example.nutripandacare.databinding.FragmentEditProfilAnakBinding
import com.example.nutripandacare.firebase.FirebaseHelper
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class EditProfilAnakFragment : Fragment() {

    private var _binding: FragmentEditProfilAnakBinding? = null
    private val binding get() = _binding!!

    private var fotoUri: Uri? = null
    private var cameraImageUri: Uri? = null
    private var existingFotoUrl = ""

    private var anakId: String? = null
    private var isAnakBaru = false

    // ── Galeri: salin ke cache agar permission tidak expired saat upload ──────
    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                val cached = copyUriToCache(requireContext(), it, "foto_anak")
                fotoUri = cached ?: it
                tampilkanPreviewFoto(fotoUri!!)
            }
        }

    // ── Kamera ───────────────────────────────────────────────────────────────
    private val takePictureLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success && cameraImageUri != null) {
                fotoUri = cameraImageUri
                tampilkanPreviewFoto(fotoUri!!)
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditProfilAnakBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
        loadDataAnak()
    }

    private fun tampilkanPreviewFoto(source: Any) {
        if (_binding == null) return
        Glide.with(this)
            .load(source)
            .placeholder(R.drawable.ic_child_avatar)
            .circleCrop()
            .into(binding.ivFotoAnak)
    }

    private fun setupClickListeners() {
        binding.toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
        binding.ivFotoAnak.setOnClickListener    { showImageSourceDialog() }
        binding.btnPilihFoto.setOnClickListener  { showImageSourceDialog() }
        binding.btnSimpan.setOnClickListener     { simpanProfil() }
    }

    private fun showImageSourceDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Pilih Sumber Foto")
            .setItems(arrayOf("📷  Ambil dari Kamera", "🖼️  Pilih dari Galeri")) { _, which ->
                if (which == 0) openCamera() else pickImageLauncher.launch("image/*")
            }
            .show()
    }

    private fun openCamera() {
        val photoFile = File(requireContext().cacheDir, "foto_anak_${System.currentTimeMillis()}.jpg")
        cameraImageUri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            photoFile
        )
        takePictureLauncher.launch(cameraImageUri)
    }

    private fun loadDataAnak() {
        FirebaseHelper.getDataAnak(
            onSuccess = { id, data ->
                if (_binding == null) return@getDataAnak
                anakId     = id
                isAnakBaru = false

                binding.etNamaAnak.setText(data["nama_anak"]     as? String ?: "")
                binding.etUsiaAnak.setText(data["usia_anak"]     as? String ?: "")
                binding.etUsiaBulan.setText(
                    when (val v = data["usia_bulan"]) {
                        is Long, is Int, is Number -> (v as Number).toInt().let { if (it > 0) it.toString() else "" }
                        is String -> v.ifEmpty { "" }
                        else -> ""
                    }
                )
                binding.etSekolahAnak.setText(data["sekolah_anak"] as? String ?: "")
                binding.etKelasAnak.setText(data["kelas"]          as? String ?: "")

                val jk = data["jenis_kelamin"] as? String ?: ""
                if (jk.equals("Laki-laki", true))  binding.rbLakiLaki.isChecked  = true
                else if (jk.equals("Perempuan", true)) binding.rbPerempuan.isChecked = true

                existingFotoUrl = data["foto_anak"] as? String ?: ""
                if (existingFotoUrl.isNotEmpty()) {
                    tampilkanPreviewFoto(existingFotoUrl)
                }
            },
            onError = {
                if (_binding == null) return@getDataAnak
                // Belum ada data anak → mode tambah baru
                isAnakBaru = true
                binding.btnSimpan.text = "Simpan Data Anak"
            }
        )
    }

    private fun simpanProfil() {
        val nama = binding.etNamaAnak.text.toString().trim()
        if (nama.isEmpty()) {
            binding.etNamaAnak.error = "Nama anak wajib diisi"
            return
        }
        setLoading(true)

        if (fotoUri != null) {
            // Upload foto baru, lalu simpan ke Firestore
            uploadFoto(fotoUri!!) { url ->
                simpanKeFirestore(url ?: existingFotoUrl)
            }
        } else {
            // Tidak ada foto baru, simpan dengan foto lama (atau kosong)
            simpanKeFirestore(existingFotoUrl)
        }
    }

    // ── Upload foto pakai putBytes — hindari SecurityException content URI ────
    private fun uploadFoto(uri: Uri, onComplete: (String?) -> Unit) {
        val uid = FirebaseHelper.uid
        val ref = FirebaseStorage.getInstance().reference
            .child("foto_anak/$uid/${UUID.randomUUID()}.jpg")
        try {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            if (inputStream == null) {
                Toast.makeText(requireContext(), "Tidak bisa membaca file foto", Toast.LENGTH_SHORT).show()
                onComplete(null)
                return
            }
            val bytes = inputStream.readBytes()
            inputStream.close()

            ref.putBytes(bytes)
                .addOnSuccessListener {
                    ref.downloadUrl
                        .addOnSuccessListener { downloadUri ->
                            onComplete(downloadUri.toString())
                        }
                        .addOnFailureListener {
                            Toast.makeText(requireContext(), "Gagal ambil URL foto", Toast.LENGTH_SHORT).show()
                            onComplete(null)
                        }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Gagal upload foto: ${e.message}", Toast.LENGTH_SHORT).show()
                    onComplete(null)
                }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error baca foto: ${e.message}", Toast.LENGTH_SHORT).show()
            onComplete(null)
        }
    }

    private fun simpanKeFirestore(fotoUrl: String) {
        val jk = when {
            binding.rbLakiLaki.isChecked  -> "Laki-laki"
            binding.rbPerempuan.isChecked -> "Perempuan"
            else                          -> ""
        }

        val updates = mutableMapOf<String, Any>(
            "nama_anak"     to binding.etNamaAnak.text.toString().trim(),
            "usia_anak"     to binding.etUsiaAnak.text.toString().trim(),
            "usia_bulan"    to (binding.etUsiaBulan.text.toString().toIntOrNull() ?: 0),
            "jenis_kelamin" to jk,
            "sekolah_anak"  to binding.etSekolahAnak.text.toString().trim(),
            "kelas"         to binding.etKelasAnak.text.toString().trim()
        )
        if (fotoUrl.isNotEmpty()) updates["foto_anak"] = fotoUrl

        if (isAnakBaru) {
            FirebaseHelper.tambahDataAnak(
                namaAnak     = updates["nama_anak"]     as String,
                usiaAnak     = updates["usia_anak"]     as String,
                usiaBulan    = updates["usia_bulan"]    as Int,
                jenisKelamin = jk,
                sekolahAnak  = updates["sekolah_anak"] as String,
                kelas        = updates["kelas"]         as String,
                fotoAnak     = fotoUrl,
                onSuccess    = { newId ->
                    if (_binding == null) return@tambahDataAnak
                    anakId     = newId
                    isAnakBaru = false
                    setLoading(false)
                    // Jika ada foto yang di-upload, update preview dengan URL
                    if (fotoUrl.isNotEmpty()) tampilkanPreviewFoto(fotoUrl)
                    Toast.makeText(requireContext(), "Data anak berhasil disimpan ✅", Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                },
                onError = { err ->
                    if (_binding == null) return@tambahDataAnak
                    setLoading(false)
                    Toast.makeText(requireContext(), "Gagal simpan: $err", Toast.LENGTH_SHORT).show()
                }
            )
        } else {
            val id = anakId ?: run {
                setLoading(false)
                Toast.makeText(requireContext(), "ID anak tidak ditemukan", Toast.LENGTH_SHORT).show()
                return
            }
            FirebaseHelper.updateDataAnak(
                anakId    = id,
                updates   = updates,
                onSuccess = {
                    if (_binding == null) return@updateDataAnak
                    setLoading(false)
                    // Update foto profil di UI jika ada foto baru
                    if (fotoUrl.isNotEmpty()) {
                        existingFotoUrl = fotoUrl
                        tampilkanPreviewFoto(fotoUrl)
                    }
                    Toast.makeText(requireContext(), "Profil anak berhasil diperbarui ✅", Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                },
                onError = { err ->
                    if (_binding == null) return@updateDataAnak
                    setLoading(false)
                    Toast.makeText(requireContext(), "Gagal simpan: $err", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    private fun setLoading(loading: Boolean) {
        if (_binding == null) return
        binding.btnSimpan.isEnabled = !loading
        binding.btnSimpan.text = when {
            loading    -> "Menyimpan..."
            isAnakBaru -> "Simpan Data Anak"
            else       -> "Simpan Perubahan"
        }
    }

    // ── Salin URI ke cache supaya permission tidak hilang saat upload ─────────
    private fun copyUriToCache(context: Context, uri: Uri, prefix: String): Uri? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val outFile     = File(context.cacheDir, "${prefix}_${System.currentTimeMillis()}.jpg")
            FileOutputStream(outFile).use { out -> inputStream.copyTo(out) }
            inputStream.close()
            Uri.fromFile(outFile)
        } catch (e: Exception) { null }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
