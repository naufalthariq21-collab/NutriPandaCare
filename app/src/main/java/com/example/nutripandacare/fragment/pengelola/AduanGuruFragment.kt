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

    // [FIX] Reload setiap kali halaman kembali aktif
    override fun onResume() {
        super.onResume()
        loadAllAduan()
    }

    private fun setupRecyclerView() {
        adapter = AduanAdapter(aduanList) { id, judul, balasanLama ->
            showBalasDialog(id, judul, balasanLama)
        }
        binding.rvAduan.layoutManager = LinearLayoutManager(requireContext())
        binding.rvAduan.adapter = adapter
    }

    private fun loadAllAduan() {
        if (_binding == null) return
        binding.progressBar.visibility = View.VISIBLE
        binding.tvEmpty.visibility = View.GONE

        FirebaseHelper.getAllAduan(
            onSuccess = { list ->
                if (_binding == null) return@getAllAduan
                binding.progressBar.visibility = View.GONE
                adapter.updateData(list)
                binding.tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
            },
            onError = { err ->
                if (_binding == null) return@getAllAduan
                binding.progressBar.visibility = View.GONE
                // [FIX] Tampilkan error detail supaya mudah debug
                Toast.makeText(requireContext(), "Gagal muat aduan: $err", Toast.LENGTH_SHORT).show()
                binding.tvEmpty.visibility = View.VISIBLE
            }
        )
    }

    private fun showBalasDialog(aduanId: String, judul: String, balasanLama: String) {
        val input = EditText(requireContext()).apply {
            hint = "Tulis balasan di sini..."
            setText(balasanLama)
            setSelection(balasanLama.length)
        }

        AlertDialog.Builder(requireContext())
            .setTitle(if (balasanLama.isEmpty()) "Balas Aduan" else "Ubah Balasan")
            .setMessage("Membalas: $judul")
            .setView(input)
            .setPositiveButton("Kirim") { _, _ ->
                val teks = input.text.toString().trim()
                if (teks.isNotEmpty()) balasAduan(aduanId, teks)
                else Toast.makeText(requireContext(), "Balasan tidak boleh kosong", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun balasAduan(aduanId: String, balasan: String) {
        FirebaseHelper.balasAduan(aduanId, balasan,
            onSuccess = {
                if (_binding == null) return@balasAduan
                Toast.makeText(requireContext(), "Balasan terkirim! ✅", Toast.LENGTH_SHORT).show()
                loadAllAduan()
            },
            onError = { err ->
                if (_binding == null) return@balasAduan
                Toast.makeText(requireContext(), "Gagal: $err", Toast.LENGTH_SHORT).show()
            }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
