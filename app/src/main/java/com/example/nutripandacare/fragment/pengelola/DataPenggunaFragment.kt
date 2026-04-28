package com.example.nutripandacare.fragment.pengelola

import android.os.Bundle
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
            requireActivity().onBackPressedDispatcher.onBackPressed()
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
                if (_binding == null) return@getPendaftarBaru
                adapter.updateData(list)
            },
            onError = { err ->
                Toast.makeText(requireContext(), err, Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun verifikasiUser(uid: String) {
        FirebaseHelper.verifikasiAkun(uid,
            onSuccess = {
                Toast.makeText(requireContext(), "Akun berhasil diverifikasi!", Toast.LENGTH_SHORT).show()
                loadPendaftarBaru()
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
