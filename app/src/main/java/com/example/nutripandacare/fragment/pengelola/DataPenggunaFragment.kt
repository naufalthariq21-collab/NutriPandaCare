package com.example.nutripandacare.fragment.pengelola

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.nutripandacare.R
import com.example.nutripandacare.databinding.FragmentDataPenggunaBinding
import com.example.nutripandacare.firebase.FirebaseHelper

class DataPenggunaFragment : Fragment() {

    private var _binding: FragmentDataPenggunaBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: UserAdapter
    private val userList = mutableListOf<Pair<String, Map<String, Any?>>>()

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
        loadPendaftarBaru()
        
        binding.toolbar.setNavigationOnClickListener {
            // Gunakan findNavController untuk navigasi balik yang lebih aman
            if (!findNavController().navigateUp()) {
                // Jika tidak ada stack, paksa balik ke Home Pengelola
                findNavController().navigate(R.id.fragment_home_pengelola)
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = UserAdapter(userList) { uid ->
            verifikasiUser(uid)
        }
        binding.rvUserList.layoutManager = LinearLayoutManager(requireContext())
        binding.rvUserList.adapter = adapter
    }

    private fun loadPendaftarBaru() {
        FirebaseHelper.getPendaftarBaru(
            onSuccess = { list ->
                if (_binding != null) {
                    adapter.updateData(list)
                }
            },
            onError = { err ->
                context?.let {
                    Toast.makeText(it, "Gagal memuat data: $err", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun verifikasiUser(uid: String) {
        FirebaseHelper.verifikasiAkun(uid,
            onSuccess = {
                context?.let {
                    Toast.makeText(it, "Akun berhasil diverifikasi!", Toast.LENGTH_SHORT).show()
                }
                loadPendaftarBaru()
            },
            onError = { err ->
                context?.let {
                    Toast.makeText(it, "Gagal verifikasi: $err", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
