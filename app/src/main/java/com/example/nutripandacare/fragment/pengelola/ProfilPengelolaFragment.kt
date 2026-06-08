package com.example.nutripandacare.fragment.pengelola

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.nutripandacare.LoginActivity
import com.example.nutripandacare.R
import com.example.nutripandacare.databinding.FragmentProfilPengelolaBinding
import com.example.nutripandacare.firebase.FirebaseHelper

class ProfilPengelolaFragment : Fragment() {

    private var _binding: FragmentProfilPengelolaBinding? = null
    private val binding get() = _binding!!

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
        binding.btnNotifikasi.setOnClickListener {
            val nav = findNavController()
            // Gunakan action ID yang sudah didefinisikan di nav_pengelola.xml
            if (nav.currentDestination?.id == R.id.fragment_profil_pengelola) {
                nav.navigate(R.id.action_profil_to_notifikasi)
            }
        }
        binding.btnLogout.setOnClickListener {
            confirmLogout()
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
