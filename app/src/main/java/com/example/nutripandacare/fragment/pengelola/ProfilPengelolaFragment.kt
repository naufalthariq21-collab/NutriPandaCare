package com.example.nutripandacare.fragment.pengelola

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

class ProfilPengelolaFragment : Fragment() {

    private var _binding: FragmentProfilPengelolaBinding? = null
    private val binding get() = _binding!!

    private var currentNama  = ""
    private var currentEmail = ""
    private var fotoUri: Uri? = null

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                fotoUri = it
                Glide.with(this).load(it)
                    .placeholder(R.drawable.ic_placeholder_avatar)
                    .circleCrop()
                    .into(binding.ivProfil)
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
                currentNama  = data["nama"]  as? String ?: ""
                currentEmail = data["email"] as? String ?: ""
                b.tvNama.text  = currentNama.ifEmpty { "Admin NutriPanda" }
                b.tvEmail.text = currentEmail

                val fotoUrl = data["foto_url"] as? String ?: ""
                if (fotoUrl.isNotEmpty()) {
                    Glide.with(this).load(fotoUrl)
                        .placeholder(R.drawable.ic_placeholder_avatar)
                        .circleCrop()
                        .into(b.ivProfil)
                }
            },
            onError = {}
        )
    }

    private fun setupClickListeners() {
        // FIX: Edit profil sekarang buka dialog edit
        binding.btnEditProfil.setOnClickListener {
            showEditProfilDialog()
        }

        binding.btnNotifikasi.setOnClickListener {
            val nav = findNavController()
            if (nav.currentDestination?.id == R.id.fragment_profil_pengelola) {
                nav.navigate(R.id.action_profil_to_notifikasi)
            }
        }

        binding.btnLogout.setOnClickListener {
            confirmLogout()
        }

        // Tap foto profil untuk ganti foto
        binding.ivProfil.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }
    }

    private fun showEditProfilDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_edit_profil, null)

        val etNama = dialogView.findViewById<TextInputEditText>(R.id.etNamaPengelola)
        etNama?.setText(currentNama)

        AlertDialog.Builder(requireContext())
            .setTitle("Edit Profil")
            .setView(dialogView)
            .setPositiveButton("Simpan") { _, _ ->
                val namaBaru = etNama?.text?.toString()?.trim() ?: ""
                if (namaBaru.isEmpty()) {
                    Toast.makeText(requireContext(), "Nama tidak boleh kosong", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                simpanProfil(namaBaru)
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun simpanProfil(namaBaru: String) {
        val uid = FirebaseHelper.uid
        val updates = mutableMapOf<String, Any>("nama" to namaBaru)

        if (fotoUri != null) {
            FirebaseHelper.uploadImage("profil/$uid.jpg", fotoUri!!,
                onSuccess = { url ->
                    updates["foto_url"] = url
                    updateFirestore(uid, updates, namaBaru)
                },
                onError = {
                    // Simpan tanpa foto jika upload gagal
                    updateFirestore(uid, updates, namaBaru)
                }
            )
        } else {
            updateFirestore(uid, updates, namaBaru)
        }
    }

    private fun updateFirestore(uid: String, updates: Map<String, Any>, namaBaru: String) {
        FirebaseHelper.updateDataUser(uid, updates,
            onSuccess = {
                if (_binding == null) return@updateDataUser
                currentNama = namaBaru
                binding.tvNama.text = namaBaru
                Toast.makeText(requireContext(), "Profil berhasil diperbarui ✅", Toast.LENGTH_SHORT).show()
                fotoUri = null
            },
            onError = { err ->
                if (_binding == null) return@updateDataUser
                Toast.makeText(requireContext(), "Gagal simpan: $err", Toast.LENGTH_SHORT).show()
            }
        )
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