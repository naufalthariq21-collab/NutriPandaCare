package com.example.nutripandacare.fragment.guru

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.nutripandacare.databinding.FragmentNotifikasiBinding
import com.example.nutripandacare.firebase.FirebaseHelper
import com.google.firebase.firestore.ListenerRegistration

/**
 * Fragment notifikasi untuk role GURU.
 * Re-use layout fragment_notifikasi dan NotifikasiAdapter dari pengelola.
 */
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

        binding.tvTandaiBaca.setOnClickListener {
            tandaiSemuaDibaca()
        }
    }

    private fun setupRecyclerView() {
        adapter = com.example.nutripandacare.fragment.pengelola.NotifikasiAdapter(notifList) { id ->
            FirebaseHelper.tandaiDibaca(id)
        }
        binding.rvNotifikasi.layoutManager = LinearLayoutManager(requireContext())
        binding.rvNotifikasi.adapter = adapter
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
