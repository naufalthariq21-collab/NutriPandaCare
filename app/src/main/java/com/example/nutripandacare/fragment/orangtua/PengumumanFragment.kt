package com.example.nutripandacare.fragment.orangtua

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.nutripandacare.databinding.FragmentPengumumanBinding
import com.example.nutripandacare.firebase.FirebaseHelper

class PengumumanFragment : Fragment() {

    private var _binding: FragmentPengumumanBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: PengumumanAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPengumumanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = PengumumanAdapter(emptyList())
        binding.rvPengumuman.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPengumuman.adapter = adapter

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        loadPengumuman()
    }

    private fun loadPengumuman() {
        FirebaseHelper.getPengumumanTerbaru(20,
            onSuccess = { list ->
                if (_binding != null) {
                    adapter.updateData(list)
                }
            },
            onError = { err ->
                Toast.makeText(requireContext(), err, Toast.LENGTH_SHORT).show()
            }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
