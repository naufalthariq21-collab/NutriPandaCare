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
import com.example.nutripandacare.R
import com.example.nutripandacare.databinding.FragmentDataPenggunaBinding
import com.example.nutripandacare.firebase.FirebaseHelper

class DataPenggunaFragment : Fragment() {

    private var _binding: FragmentDataPenggunaBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: UserAdapter
    private var allUsers = mutableListOf<Pair<String, Map<String, Any?>>>()
    private var filteredUsers = mutableListOf<Pair<String, Map<String, Any?>>>()

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
        setupFilters()
        setupSearch()
        loadAllUsers()
        
        binding.toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupRecyclerView() {
        adapter = UserAdapter(filteredUsers) { uid ->
            verifikasiUser(uid)
        }
        binding.rvUserList.layoutManager = LinearLayoutManager(requireContext())
        binding.rvUserList.adapter = adapter
    }

    private fun setupFilters() {
        binding.chipGroupFilter.setOnCheckedChangeListener { _, checkedId ->
            applyFilterAndSearch()
        }
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                applyFilterAndSearch()
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun loadAllUsers() {
        FirebaseHelper.getAllPengguna(
            onSuccess = { list ->
                if (_binding == null) return@getAllPengguna
                allUsers.clear()
                allUsers.addAll(list)
                applyFilterAndSearch()
            },
            onError = { err ->
                Toast.makeText(requireContext(), err, Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun applyFilterAndSearch() {
        val query = binding.etSearch.text.toString().lowercase()
        val checkedId = binding.chipGroupFilter.checkedChipId

        val filtered = allUsers.filter { (_, data) ->
            val nama = (data["nama"] as? String ?: "").lowercase()
            val email = (data["email"] as? String ?: "").lowercase()
            val matchesSearch = nama.contains(query) || email.contains(query)

            val isVerified = data["is_verified"] as? Boolean ?: false
            val matchesFilter = when (checkedId) {
                R.id.chipMenunggu -> !isVerified
                R.id.chipTerverifikasi -> isVerified
                else -> true // chipSemua
            }

            matchesSearch && matchesFilter
        }

        adapter.updateData(filtered)
    }

    private fun verifikasiUser(uid: String) {
        FirebaseHelper.verifikasiAkun(uid,
            onSuccess = {
                Toast.makeText(requireContext(), "Akun berhasil diverifikasi!", Toast.LENGTH_SHORT).show()
                loadAllUsers()
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
