package com.example.nutripandacare.fragment.pengelola

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.nutripandacare.databinding.FragmentAduanGuruBinding
import com.example.nutripandacare.firebase.FirebaseHelper

class AduanGuruFragment : Fragment() {

    private var _binding: FragmentAduanGuruBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: AduanAdapter
    private val aduanList = mutableListOf<Pair<String, Map<String, Any?>>>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAduanGuruBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        loadAllAduan()
        
        binding.ivBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupRecyclerView() {
        adapter = AduanAdapter(aduanList) { id, judul ->
            showBalasDialog(id, judul)
        }
        binding.rvAduan.layoutManager = LinearLayoutManager(requireContext())
        binding.rvAduan.adapter = adapter
    }

    private fun loadAllAduan() {
        FirebaseHelper.getAllAduan(
            onSuccess = { list ->
                if (_binding == null) return@getAllAduan
                adapter.updateData(list)
            },
            onError = { err ->
                Toast.makeText(requireContext(), err, Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun showBalasDialog(aduanId: String, judul: String) {
        val input = EditText(requireContext())
        input.hint = "Tulis balasan di sini..."
        
        AlertDialog.Builder(requireContext())
            .setTitle("Balas Aduan")
            .setMessage("Membalas: $judul")
            .setView(input)
            .setPositiveButton("Kirim") { _, _ ->
                val balasan = input.text.toString().trim()
                if (balasan.isNotEmpty()) {
                    balasAduan(aduanId, balasan)
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun balasAduan(aduanId: String, balasan: String) {
        FirebaseHelper.balasAduan(aduanId, balasan,
            onSuccess = {
                Toast.makeText(requireContext(), "Balasan terkirim!", Toast.LENGTH_SHORT).show()
                loadAllAduan()
            },
            onError = { err ->
                Toast.makeText(requireContext(), "Gagal: $err", Toast.LENGTH_SHORT).show()
            }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
