package com.example.nutripandacare.fragment.guru

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.nutripandacare.R
import com.example.nutripandacare.databinding.FragmentNotifikasiBinding
import com.example.nutripandacare.firebase.FirebaseHelper
import com.google.firebase.firestore.ListenerRegistration

class NotifikasiGuruFragment : Fragment() {

    private var _binding: FragmentNotifikasiBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: com.example.nutripandacare.fragment.pengelola.NotifikasiAdapter
    private val notifList = mutableListOf<Pair<String, Map<String, Any?>>>()

    private var listenerRegistration: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotifikasiBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeNotifikasi()

        binding.tvTandaiBaca.setOnClickListener { tandaiSemuaDibaca() }

        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupRecyclerView() {
        adapter = com.example.nutripandacare.fragment.pengelola.NotifikasiAdapter(notifList) { id, data ->
            // Tandai sudah dibaca
            FirebaseHelper.tandaiDibaca(id)
            
            // Tampilkan detail
            showNotifDetail(data)
        }
        binding.rvNotifikasi.layoutManager = LinearLayoutManager(requireContext())
        binding.rvNotifikasi.adapter = adapter
    }

    private fun showNotifDetail(data: Map<String, Any?>) {
        val judul = data["judul"] as? String ?: "Detail Notifikasi"
        val isi   = data["isi"] as? String ?: ""
        
        AlertDialog.Builder(requireContext())
            .setTitle(judul)
            .setMessage(isi)
            .setPositiveButton("Tutup", null)
            .show()
    }

    private fun observeNotifikasi() {
        listenerRegistration = FirebaseHelper.listenNotifikasi(
            onUpdate = { list ->
                if (_binding == null) return@listenNotifikasi
                adapter.updateData(list)
                binding.tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
            },
            onError = { err ->
                if (_binding == null) return@listenNotifikasi
                Toast.makeText(requireContext(), err, Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun tandaiSemuaDibaca() {
        FirebaseHelper.tandaiSemuaDibaca {
            if (_binding == null) return@tandaiSemuaDibaca
            Toast.makeText(requireContext(), "Semua notifikasi ditandai dibaca", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        listenerRegistration?.remove()
        listenerRegistration = null
        super.onDestroyView()
        _binding = null
    }
}