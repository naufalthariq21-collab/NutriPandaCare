package com.example.nutripandacare.fragment.pengelola

import android.content.Context
import android.content.Intent
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
import com.example.nutripandacare.LoginActivity
import com.example.nutripandacare.R
import com.example.nutripandacare.supabase.SupabaseHelper
import com.example.nutripandacare.databinding.FragmentProfilPengelolaBinding
import com.example.nutripandacare.firebase.FirebaseHelper
import com.google.android.material.textfield.TextInputEditText
import java.io.File
import java.io.FileOutputStream

class ProfilPengelolaFragment : Fragment() {

    private var _binding: FragmentProfilPengelolaBinding? = null
    private val binding get() = _binding!!

    private var fotoUri: Uri? = null
    private var cameraImageUri: Uri? = null
    private var existingFotoUrl = ""
    private var isUploading = false

    // Launcher Galeri
    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                val cached = copyUriToCache(requireContext(), it, "profil_admin")
                fotoUri = cached ?: it
                updatePreview(fotoUri)
            }
        }

    // Launcher Kamera
    private val takePictureLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success && cameraImageUri != null) {
                fotoUri = cameraImageUri
                updatePreview(fotoUri)
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfilPengelolaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadUserData()
        setupClickListeners()
    }

    private fun loadUserData() {
        val uid = FirebaseHelper.uid
        if (uid.isEmpty()) return
        FirebaseHelper.getDataUser(uid,
            onSuccess = { data ->
                val b = _binding ?: return@getDataUser
                b.tvNama.text  = data["nama"]  as? String ?: "Admin NutriPanda"
                b.tvEmail.text = data["email"] as? String ?: ""
                existingFotoUrl = data["foto_url"] as? String ?: ""
                if (existingFotoUrl.isNotEmpty() && fotoUri == null) {
                    updatePreview(existingFotoUrl)
                }
            },
            onError = {}
        )
    }

    private fun updatePreview(source: Any?) {
        if (_binding == null) return
        Glide.with(this)
            .load(source)
            .placeholder(R.drawable.ic_placeholder_avatar)
            .circleCrop()
            .into(binding.ivProfil)
    }

    private fun setupClickListeners() {
        binding.btnEditProfil.setOnClickListener { showEditProfilDialog() }
        binding.ivProfil.setOnClickListener      { showImageSourceDialog() }
        binding.btnNotifikasi.setOnClickListener {
            findNavController().navigate(R.id.action_profil_to_notifikasi)
        }
        binding.btnLogout.setOnClickListener { confirmLogout() }
    }

    private fun showImageSourceDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Pilih Foto Profil")
            .setItems(arrayOf("📷  Ambil dari Kamera", "🖼️  Pilih dari Galeri")) { _, which ->
                if (which == 0) openCamera() else pickImageLauncher.launch("image/*")
            }.show()
    }

    private fun openCamera() {
        val photoFile = File(requireContext().cacheDir, "profil_admin_${System.currentTimeMillis()}.jpg")
        cameraImageUri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.fileprovider", photoFile)
        takePictureLauncher.launch(cameraImageUri)
    }

    private fun showEditProfilDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_profil, null)
        val etNama = dialogView.findViewById<TextInputEditText>(R.id.etEditNama)
        val etHp   = dialogView.findViewById<TextInputEditText>(R.id.etEditNoHp)

        etNama.setText(binding.tvNama.text.toString())

        AlertDialog.Builder(requireContext())
            .setTitle("Edit Profil")
            .setView(dialogView)
            .setPositiveButton("Simpan") { _, _ ->
                val nama = etNama.text.toString().trim()
                val hp   = etHp.text.toString().trim()
                if (nama.isEmpty()) {
                    Toast.makeText(requireContext(), "Nama wajib diisi", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                prosesSimpan(nama, hp)
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun prosesSimpan(nama: String, noHp: String) {
        if (isUploading) return
        isUploading = true
        Toast.makeText(requireContext(), "Menyimpan perubahan...", Toast.LENGTH_SHORT).show()

        if (fotoUri != null) {
            SupabaseHelper.uploadImage(
                uri       = fotoUri!!,
                context   = requireContext(),
                folder    = "profil_pengelola",
                onSuccess = { url -> updateDataUser(nama, noHp, url) },
                onError = {
                    isUploading = false
                    Toast.makeText(requireContext(), "Gagal upload foto, mencoba simpan tanpa foto baru", Toast.LENGTH_SHORT).show()
                    updateDataUser(nama, noHp, existingFotoUrl)
                }
            )
        } else {
            updateDataUser(nama, noHp, existingFotoUrl)
        }
    }

    private fun updateDataUser(nama: String, noHp: String, fotoUrl: String) {
        val uid = FirebaseHelper.uid
        val updates = mutableMapOf<String, Any>("nama" to nama, "foto_url" to fotoUrl)
        if (noHp.isNotEmpty()) updates["no_hp"] = noHp

        FirebaseHelper.db.collection("users").document(uid).update(updates)
            .addOnSuccessListener {
                isUploading = false
                if (_binding == null) return@addOnSuccessListener
                Toast.makeText(requireContext(), "Profil diperbarui ✅", Toast.LENGTH_SHORT).show()
                binding.tvNama.text = nama
                existingFotoUrl = fotoUrl
                fotoUri = null
            }
            .addOnFailureListener {
                isUploading = false
                Toast.makeText(requireContext(), "Gagal update: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun confirmLogout() {
        AlertDialog.Builder(requireContext())
            .setTitle("Logout")
            .setMessage("Yakin ingin keluar?")
            .setPositiveButton("Ya") { _, _ ->
                FirebaseHelper.logout()
                startActivity(Intent(requireContext(), LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                })
            }.setNegativeButton("Tidak", null).show()
    }

    private fun copyUriToCache(context: Context, uri: Uri, prefix: String): Uri? {
        return try {
            val stream = context.contentResolver.openInputStream(uri) ?: return null
            val file = File(context.cacheDir, "${prefix}_${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use { stream.copyTo(it) }
            Uri.fromFile(file)
        } catch (e: Exception) { null }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}