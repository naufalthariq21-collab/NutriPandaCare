package com.example.nutripandacare.fragment.pengelola

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.nutripandacare.databinding.FragmentNotifikasiBinding
import com.example.nutripandacare.firebase.FirebaseHelper

class NotifikasiFragment : Fragment() {

    private var _binding: FragmentNotifikasiBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: NotifikasiAdapter
    private val notifList = mutableListOf<Pair<String, Map<String, Any?>>>()

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
        adapter = NotifikasiAdapter(notifList) { id ->
            FirebaseHelper.tandaiDibaca(id)
        }
        binding.rvNotifikasi.layoutManager = LinearLayoutManager(requireContext())
        binding.rvNotifikasi.adapter = adapter
    }

    private fun observeNotifikasi() {
        FirebaseHelper.listenNotifikasi(
            onUpdate = { list ->
                if (_binding == null) return@listenNotifikasi
                adapter.updateData(list)
            },
            onError = { err ->
                Toast.makeText(requireContext(), err, Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun tandaiSemuaDibaca() {
        FirebaseHelper.tandaiSemuaDibaca {
            Toast.makeText(requireContext(), "Semua ditandai dibaca", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
