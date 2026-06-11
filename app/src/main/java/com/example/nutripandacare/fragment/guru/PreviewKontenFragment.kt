package com.example.nutripandacare.fragment.guru

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.nutripandacare.R
import com.example.nutripandacare.databinding.FragmentPreviewKontenBinding
import com.example.nutripandacare.firebase.FirebaseHelper

class PreviewKontenFragment : Fragment() {

    private var _binding: FragmentPreviewKontenBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPreviewKontenBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Tombol back → pop ke konten edukasi (bukan ke form buat konten)
        // FIX #8: dari preview back langsung ke daftar konten
        binding.toolbar.setNavigationOnClickListener {
            try {
                val popped = findNavController().popBackStack(R.id.nav_konten_edukasi, false)
                if (!popped) requireActivity().onBackPressedDispatcher.onBackPressed()
            } catch (e: Exception) {
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        }

        val artikelId = arguments?.getString("artikel_id")
        if (!artikelId.isNullOrEmpty()) {
            loadDetailArtikel(artikelId)
        } else {
            Toast.makeText(requireContext(), "ID artikel tidak ditemukan", Toast.LENGTH_SHORT).show()
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun loadDetailArtikel(id: String) {
        FirebaseHelper.db.collection("artikel").document(id).get()
            .addOnSuccessListener { doc ->
                if (_binding == null) return@addOnSuccessListener
                if (doc.exists()) {
                    binding.tvJudul.text     = doc.getString("judul")    ?: "-"
                    binding.tvKategori.text  = doc.getString("kategori") ?: "-"
                    binding.tvIsiKonten.text = doc.getString("isi_konten") ?: "-"
                    binding.tvWaktuBaca.text = "${doc.getLong("menit_baca") ?: 0} menit baca"
                    val thumb = doc.getString("thumbnail_url") ?: ""
                    if (thumb.isNotEmpty()) {
                        Glide.with(this).load(thumb).into(binding.ivThumbnail)
                    }
                } else {
                    Toast.makeText(requireContext(), "Artikel tidak ditemukan", Toast.LENGTH_SHORT).show()
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                }
            }
            .addOnFailureListener { e ->
                if (_binding == null) return@addOnFailureListener
                Toast.makeText(requireContext(), "Gagal memuat konten: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}