package com.example.nutripandacare.fragment.pengelola

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.nutripandacare.databinding.FragmentDataPenggunaBinding
import com.example.nutripandacare.firebase.FirebaseHelper

class DataPenggunaFragment : Fragment() {

    private var _binding: FragmentDataPenggunaBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: UserAdapter
    private var fullList = mutableListOf<Pair<String, Map<String, Any?>>>()
    private var currentFilter = "Semua"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDataPenggunaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        loadData()
        setupListeners()
    }

    private fun setupRecyclerView() {
        adapter = UserAdapter(mutableListOf()) { uid ->
            verifikasiUser(uid)
        }
        binding.rvUserList.layoutManager = LinearLayoutManager(requireContext())
        binding.rvUserList.adapter = adapter
    }

    private fun loadData() {
        // Kita ambil semua pengguna dulu agar bisa difilter di lokal atau via query
        FirebaseHelper.getAllPengguna(
            onSuccess = { list ->
                if (_binding == null) return@getAllPengguna
                fullList.clear()
                fullList.addAll(list)
                applyFilterAndSearch()
            },
            onError = { err ->
                Toast.makeText(requireContext(), err, Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun setupListeners() {
        binding.toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        binding.chipGroupFilter.setOnCheckedChangeListener { _, checkedId ->
            currentFilter = when (checkedId) {
                binding.chipMenunggu.id -> "Menunggu"
                binding.chipTerverifikasi.id -> "Terverifikasi"
                else -> "Semua"
            }
            applyFilterAndSearch()
        }

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                applyFilterAndSearch()
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun applyFilterAndSearch() {
        val searchText = binding.etSearch.text.toString().lowercase()
        
        val filtered = fullList.filter { (_, data) ->
            val nama = (data["nama"] as? String ?: "").lowercase()
            val email = (data["email"] as? String ?: "").lowercase()
            val isVerified = data["is_verified"] as? Boolean ?: false
            
            val matchFilter = when (currentFilter) {
                "Menunggu" -> !isVerified
                "Terverifikasi" -> isVerified
                else -> true
            }
            
            val matchSearch = nama.contains(searchText) || email.contains(searchText)
            
            matchFilter && matchSearch
        }
        
        adapter.updateData(filtered)
    }

    private fun verifikasiUser(uid: String) {
        FirebaseHelper.verifikasiAkun(uid,
            onSuccess = {
                Toast.makeText(requireContext(), "Akun berhasil diverifikasi!", Toast.LENGTH_SHORT).show()
                loadData() // Refresh data
            },
            onError = { err ->
                Toast.makeText(requireContext(), "Gagal verifikasi: $err", Toast.LENGTH_SHORT).show()
            }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
