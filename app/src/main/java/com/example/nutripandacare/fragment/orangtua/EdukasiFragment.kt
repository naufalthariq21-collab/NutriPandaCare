package com.example.nutripandacare.fragment.orangtua

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
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
    private val artikelList    = mutableListOf<Pair<String, Map<String, Any?>>>()
    private val allArtikelList = mutableListOf<Pair<String, Map<String, Any?>>>()
    private var selectedKategori = "Semua"

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
        setupChipListeners()
        setupSearchBar()
        loadArtikel("Semua")
    }

    override fun onResume() {
        super.onResume()
        // Refresh setiap kali kembali (misal guru baru upload artikel)
        loadArtikel(selectedKategori)
    }

    private fun setupRecyclerView() {
        adapter = ArtikelAdapter(artikelList) { id, _ ->
            val args = bundleOf("artikel_id" to id)
            findNavController().navigate(R.id.action_edukasi_to_preview_konten, args)
        }
        binding.rvEdukasi.layoutManager = LinearLayoutManager(requireContext())
        binding.rvEdukasi.adapter = adapter
    }

    private fun setupChipListeners() {
        // Kategori konsisten dengan FirebaseHelper.KATEGORI_ARTIKEL:
        // "Stunting", "Resep Sehat", "Gizi", "Tumbuh Kembang"
        binding.chipSemua.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) { selectedKategori = "Semua"; loadArtikel("Semua") }
        }
        binding.chipStunting.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) { selectedKategori = "Stunting"; loadArtikel("Stunting") }
        }
        binding.chipResepSehat.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) { selectedKategori = "Resep Sehat"; loadArtikel("Resep Sehat") }
        }
        binding.chipGizi.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) { selectedKategori = "Gizi"; loadArtikel("Gizi") }
        }
        binding.chipTumbuhKembang.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) { selectedKategori = "Tumbuh Kembang"; loadArtikel("Tumbuh Kembang") }
        }
    }

    private fun setupSearchBar() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterByQuery(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun filterByQuery(query: String) {
        val filtered = if (query.isBlank()) {
            allArtikelList.toList()
        } else {
            allArtikelList.filter { (_, data) ->
                val judul    = (data["judul"]    as? String ?: "").lowercase()
                val kategori = (data["kategori"] as? String ?: "").lowercase()
                judul.contains(query.lowercase()) || kategori.contains(query.lowercase())
            }
        }
        adapter.updateData(filtered)
        updateEmptyState(filtered.isEmpty())
        updateJumlahLabel(filtered.size)
    }

    private fun loadArtikel(kategori: String) {
        binding.progressLoading.visibility  = View.VISIBLE
        binding.rvEdukasi.visibility        = View.GONE
        binding.layoutEmptyState.visibility = View.GONE

        FirebaseHelper.getArtikel(kategori,
            onSuccess = { list ->
                if (_binding == null) return@getArtikel
                binding.progressLoading.visibility = View.GONE

                allArtikelList.clear()
                allArtikelList.addAll(list)

                val query = binding.etSearch.text?.toString() ?: ""
                filterByQuery(query)

                binding.rvEdukasi.visibility = View.VISIBLE
            },
            onError = { err ->
                if (_binding == null) return@getArtikel
                binding.progressLoading.visibility = View.GONE
                binding.rvEdukasi.visibility        = View.VISIBLE
                Toast.makeText(requireContext(), "Gagal memuat artikel: $err", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        binding.layoutEmptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.rvEdukasi.visibility        = if (isEmpty) View.GONE   else View.VISIBLE
    }

    private fun updateJumlahLabel(count: Int) {
        binding.tvJumlahArtikel.text = when {
            count == 0 -> "Tidak ada artikel ditemukan"
            count == 1 -> "Menampilkan 1 artikel"
            else       -> "Menampilkan $count artikel"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
