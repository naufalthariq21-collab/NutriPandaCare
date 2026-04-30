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
        FirebaseHelper.getDataUser(uid,
            onSuccess = { data ->
                if (_binding == null) return@getDataUser
                binding.tvNama.text = data["nama"] as? String ?: "Admin NutriPanda"
                binding.tvEmail.text = data["email"] as? String ?: ""
                
                val fotoUrl = data["foto_url"] as? String ?: ""
                if (fotoUrl.isNotEmpty()) {
                    Glide.with(this).load(fotoUrl).into(binding.ivProfil)
                }
            },
            onError = { }
        )
    }

    private fun setupClickListeners() {
        binding.btnNotifikasi.setOnClickListener {
            findNavController().navigate(R.id.fragment_notifikasi)
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
                val intent = Intent(requireContext(), LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }
            .setNegativeButton("Tidak", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
