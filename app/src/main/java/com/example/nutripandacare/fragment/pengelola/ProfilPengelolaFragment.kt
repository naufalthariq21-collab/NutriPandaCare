package com.example.nutripandacare.fragment.pengelola

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.nutripandacare.LoginActivity
import com.example.nutripandacare.R
import com.example.nutripandacare.databinding.FragmentProfilPengelolaBinding
import com.example.nutripandacare.firebase.FirebaseHelper
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID

class ProfilPengelolaFragment : Fragment() {

    private var _binding: FragmentProfilPengelolaBinding? = null
    private val binding get() = _binding!!

    private var fotoUri: Uri? = null
    private var existingFotoUrl = ""

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                fotoUri = it
                binding.ivProfil.setImageURI(it)
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
                if (existingFotoUrl.isNotEmpty()) {
                    Glide.with(this).load(existingFotoUrl)
                        .placeholder(R.drawable.ic_placeholder_avatar)
                        .circleCrop()
                        .into(b.ivProfil)
                }
            },
            onError = {}
        )
    }

    private fun setupClickListeners() {
        // FIX #2: Edit profil
        binding.btnEditProfil.setOnClickListener { showEditProfilDialog() }

        // Ganti foto langsung tap avatar
        binding.ivProfil.setOnClickListener { pickImageLauncher.launch("image/*") }

        binding.btnNotifikasi.setOnClickListener {
            val nav = findNavController()
            if (nav.currentDestination?.id == R.id.fragment_profil_pengelola) {
                nav.navigate(R.id.action_profil_to_notifikasi)
            }
        }

        binding.btnLogout.setOnClickListener { confirmLogout() }
    }

    // ─── EDIT PROFIL ─────────────────────────────────────────────────────────

    private fun showEditProfilDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_edit_profil, null)

        val etNama = dialogView.findViewById<TextInputEditText>(R.id.etEditNama)
        val etHp   = dialogView.findViewById<TextInputEditText>(R.id.etEditNoHp)

        // Isi dengan data saat ini
        etNama.setText(binding.tvNama.text.toString())

        AlertDialog.Builder(requireContext())
            .setTitle("Edit Profil")
            .setView(dialogView)
            .setPositiveButton("Simpan") { _, _ ->
                val namaBaru = etNama.text.toString().trim()
                val hpBaru   = etHp.text.toString().trim()
                if (namaBaru.isEmpty()) {
                    Toast.makeText(requireContext(), "Nama tidak boleh kosong", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                simpanProfil(namaBaru, hpBaru)
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun simpanProfil(nama: String, noHp: String) {
        val uid = FirebaseHelper.uid
        if (uid.isEmpty()) return

        if (fotoUri != null) {
            // Upload foto baru dulu
            val ref = FirebaseStorage.getInstance().reference
                .child("profil/$uid/${UUID.randomUUID()}.jpg")
            ref.putFile(fotoUri!!)
                .addOnSuccessListener {
                    ref.downloadUrl.addOnSuccessListener { url ->
                        updateDataUser(nama, noHp, url.toString())
                    }
                }
                .addOnFailureListener {
                    // Gagal upload foto tapi tetap update nama
                    updateDataUser(nama, noHp, existingFotoUrl)
                }
        } else {
            updateDataUser(nama, noHp, existingFotoUrl)
        }
    }

    private fun updateDataUser(nama: String, noHp: String, fotoUrl: String) {
        val uid = FirebaseHelper.uid
        val updates = mutableMapOf<String, Any>("nama" to nama)
        if (noHp.isNotEmpty()) updates["no_hp"] = noHp
        if (fotoUrl.isNotEmpty()) updates["foto_url"] = fotoUrl

        FirebaseHelper.db.collection("users").document(uid)
            .update(updates)
            .addOnSuccessListener {
                if (_binding == null) return@addOnSuccessListener
                Toast.makeText(requireContext(), "Profil berhasil diperbarui ✅", Toast.LENGTH_SHORT).show()
                binding.tvNama.text = nama
                existingFotoUrl = fotoUrl
                if (fotoUrl.isNotEmpty()) {
                    Glide.with(this).load(fotoUrl).circleCrop().into(binding.ivProfil)
                }
                fotoUri = null
            }
            .addOnFailureListener { e ->
                if (_binding == null) return@addOnFailureListener
                Toast.makeText(requireContext(), "Gagal update: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun confirmLogout() {
        AlertDialog.Builder(requireContext())
            .setTitle("Logout")
            .setMessage("Yakin ingin keluar?")
            .setPositiveButton("Ya") { _, _ ->
                FirebaseHelper.logout()
                startActivity(
                    Intent(requireContext(), LoginActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                )
            }
            .setNegativeButton("Tidak", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}