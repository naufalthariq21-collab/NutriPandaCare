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

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                val cached = copyUriToCache(requireContext(), it, "foto_anak")
                fotoUri = cached ?: it
                binding.ivFotoAnak.setImageURI(fotoUri)
            }
        }

    private val takePictureLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success && cameraImageUri != null) {
                fotoUri = cameraImageUri
                binding.ivFotoAnak.setImageURI(cameraImageUri)
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
        anakId = arguments?.getString("anak_id")
        setupClickListeners()
        loadDataAnak()
    }

    private fun setupClickListeners() {
        binding.toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
        binding.ivFotoAnak.setOnClickListener  { showImageSourceDialog() }
        binding.btnPilihFoto.setOnClickListener { showImageSourceDialog() }
        binding.btnSimpan.setOnClickListener   { simpanProfil() }
    }

    private fun showImageSourceDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Pilih Sumber Foto")
            .setItems(arrayOf("📷  Ambil dari Kamera", "🖼️  Pilih dari Galeri")) { _, which ->
                when (which) {
                    0 -> openCamera()
                    1 -> pickImageLauncher.launch("image/*")
                }
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

    // GAP-7 FIX: loadDataAnak membaca field sesuai key yang DISIMPAN oleh tambahDataAnak()
    // di FirebaseHelper, yaitu: "nama_anak", "usia_anak", "sekolah_anak", "foto_anak"
    // Sebelumnya pakai "nama", "usia", "sekolah", "foto_url" — mismatch!
    private fun loadDataAnak() {
        val uid = FirebaseHelper.uid
        if (uid.isEmpty()) return

        val id = anakId ?: return
        FirebaseHelper.db
            .collection("users").document(uid)
            .collection("anak").document(id)
            .get()
            .addOnSuccessListener { doc ->
                if (_binding == null || !doc.exists()) return@addOnSuccessListener

                // Baca dengan key konsisten: "nama_anak", "usia_anak", "sekolah_anak", "foto_anak"
                binding.etNamaAnak.setText(doc.getString("nama_anak")    ?: "")
                binding.etUsiaAnak.setText(doc.getString("usia_anak")    ?: "")
                binding.etUsiaBulan.setText(
                    (doc.getLong("usia_bulan")?.toString()) ?: (doc.getString("usia_bulan") ?: "")
                )
                binding.etSekolahAnak.setText(doc.getString("sekolah_anak") ?: "")
                binding.etKelasAnak.setText(doc.getString("kelas")         ?: "")

                val jk = doc.getString("jenis_kelamin") ?: ""
                if (jk.equals("Laki-laki", true))  binding.rbLakiLaki.isChecked  = true
                else if (jk.equals("Perempuan", true)) binding.rbPerempuan.isChecked = true

                // Foto anak disimpan dengan key "foto_anak"
                existingFotoUrl = doc.getString("foto_anak") ?: ""
                if (existingFotoUrl.isNotEmpty()) {
                    Glide.with(this).load(existingFotoUrl)
                        .placeholder(R.drawable.ic_child_avatar)
                        .circleCrop()
                        .into(binding.ivFotoAnak)
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Gagal memuat data: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun simpanProfil() {
        val nama = binding.etNamaAnak.text.toString().trim()
        if (nama.isEmpty()) {
            binding.etNamaAnak.error = "Nama anak wajib diisi"
            return
        }

        setLoading(true)

        if (fotoUri != null) {
            uploadFoto(fotoUri!!) { url -> updateFirestore(url ?: existingFotoUrl) }
        } else {
            updateFirestore(existingFotoUrl)
        }
    }

    private fun uploadFoto(uri: Uri, onComplete: (String?) -> Unit) {
        val uid = FirebaseHelper.uid
        val ref = FirebaseStorage.getInstance().reference
            .child("foto_anak/$uid/${UUID.randomUUID()}.jpg")

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
                .addOnFailureListener { onComplete(null) }
        } catch (e: Exception) {
            onComplete(null)
        }
    }

    // GAP-6 & GAP-7 FIX: updateFirestore menyimpan dengan key SAMA seperti
    // tambahDataAnak() di FirebaseHelper dan yang dibaca HomeOrangTuaFragment:
    // "nama_anak", "usia_anak", "sekolah_anak", "foto_anak"
    // Sebelumnya menyimpan "nama", "usia", "sekolah" — mismatch dengan HomeOrangTuaFragment!
    private fun updateFirestore(fotoUrl: String) {
        val uid = FirebaseHelper.uid
        val id  = anakId ?: return

        val jk = when {
            binding.rbLakiLaki.isChecked  -> "Laki-laki"
            binding.rbPerempuan.isChecked -> "Perempuan"
            else                          -> ""
        }

        val updates = mutableMapOf<String, Any>(
            "nama_anak"      to binding.etNamaAnak.text.toString().trim(),
            "usia_anak"      to binding.etUsiaAnak.text.toString().trim(),
            "usia_bulan"     to (binding.etUsiaBulan.text.toString().toIntOrNull() ?: 0),
            "jenis_kelamin"  to jk,
            "sekolah_anak"   to binding.etSekolahAnak.text.toString().trim(),
            "kelas"          to binding.etKelasAnak.text.toString().trim()
        )
        if (fotoUrl.isNotEmpty()) updates["foto_anak"] = fotoUrl

        FirebaseHelper.db
            .collection("users").document(uid)
            .collection("anak").document(id)
            .update(updates)
            .addOnSuccessListener {
                if (_binding == null) return@addOnSuccessListener
                setLoading(false)
                Toast.makeText(requireContext(), "Profil anak berhasil disimpan ✅", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            }
            .addOnFailureListener { e ->
                if (_binding == null) return@addOnFailureListener
                setLoading(false)
                Toast.makeText(requireContext(), "Gagal simpan: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setLoading(loading: Boolean) {
        binding.btnSimpan.isEnabled = !loading
        binding.btnSimpan.text = if (loading) "Menyimpan..." else "Simpan Perubahan"
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
