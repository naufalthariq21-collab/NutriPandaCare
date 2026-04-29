package com.example.nutripandacare.fragment.orangtua

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.nutripandacare.R
import com.example.nutripandacare.databinding.FragmentEdukasiBinding
import com.example.nutripandacare.firebase.FirebaseHelper

class EdukasiFragment : Fragment() {

    private var _binding: FragmentEdukasiBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: ArtikelAdapter
    private val artikelList = mutableListOf<Pair<String, Map<String, Any?>>>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEdukasiBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        loadArtikel("Semua")
        setupChipListeners()
    }

    private fun setupRecyclerView() {
        adapter = ArtikelAdapter(artikelList) { id, _ ->
            // Detail artikel (opsional: navigasi ke PreviewKontenFragment)
            val bundle = Bundle().apply {
                putString("artikel_id", id)
            }
            findNavController().navigate(R.id.fragment_preview_konten, bundle)
        }
        binding.rvEdukasi.layoutManager = LinearLayoutManager(requireContext())
        binding.rvEdukasi.adapter = adapter
    }

    private fun setupChipListeners() {
        binding.chipGroupKategori.setOnCheckedStateChangeListener { group, checkedIds ->
            val checkedId = checkedIds.firstOrNull() ?: return@setOnCheckedStateChangeListener
            val kategori = when (checkedId) {
                // Di XML Chip ke-0 adalah Semua, ke-1 Stunting, dst.
                // Sebaiknya pakai ID statis di XML agar lebih aman, tapi ini fallback logikanya:
                group.getChildAt(1).id -> "Stunting"
                group.getChildAt(2).id -> "Resep Sehat"
                else -> "Semua"
            }
            loadArtikel(kategori)
        }
    }

    private fun loadArtikel(kategori: String) {
        FirebaseHelper.getArtikel(kategori,
            onSuccess = { list ->
                if (_binding == null) return@getArtikel
                adapter.updateData(list)
            },
            onError = { err ->
                if (_binding == null) return@getArtikel
                Toast.makeText(requireContext(), "Gagal memuat artikel: $err", Toast.LENGTH_SHORT).show()
            }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
