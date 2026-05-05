package com.example.nutripandacare.fragment.guru

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.nutripandacare.databinding.FragmentAduanMbgBinding
import com.example.nutripandacare.firebase.FirebaseHelper

class AduanMbgFragment : Fragment() {

    private var _binding: FragmentAduanMbgBinding? = null
    private val binding get() = _binding!!
    
    private var userNama: String = ""
    private var isLoading = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAduanMbgBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupSpinner()
        loadUserData()
        setupClickListeners()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupSpinner() {
        val kategori = arrayOf("Makanan", "Pelayanan", "Kebersihan", "Lainnya")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, kategori)
        binding.spinnerKategoriAduan.adapter = adapter
    }

    private fun loadUserData() {
        FirebaseHelper.getDataUser(FirebaseHelper.uid,
            onSuccess = { data ->
                userNama = data["nama"] as? String ?: "Guru"
            },
            onError = { /* Silently fail or use default */ }
        )
    }

    private fun setupClickListeners() {
        binding.btnKirimAduan.setOnClickListener {
            val judul = binding.etJudulAduan.text.toString().trim()
            val isi = binding.etIsiAduan.text.toString().trim()
            val kategori = binding.spinnerKategoriAduan.selectedItem.toString()

            if (judul.isEmpty()) {
                binding.etJudulAduan.error = "Judul tidak boleh kosong"
                return@setOnClickListener
            }
            if (isi.isEmpty()) {
                binding.etIsiAduan.error = "Isi aduan tidak boleh kosong"
                return@setOnClickListener
            }

            kirimAduan(judul, isi, kategori)
        }
    }

    private fun kirimAduan(judul: String, isi: String, kategori: String) {
        if (isLoading) return
        isLoading = true
        binding.btnKirimAduan.isEnabled = false
        binding.btnKirimAduan.text = "Mengirim..."

        FirebaseHelper.kirimAduan(
            judul = judul,
            isi = isi,
            kategori = kategori,
            pengirimNama = userNama,
            pengirimRole = "guru",
            onSuccess = {
                isLoading = false
                Toast.makeText(requireContext(), "Aduan berhasil terkirim", Toast.LENGTH_SHORT).show()
                requireActivity().onBackPressedDispatcher.onBackPressed()
            },
            onError = { err ->
                isLoading = false
                binding.btnKirimAduan.isEnabled = true
                binding.btnKirimAduan.text = "Kirim Aduan Sekarang"
                Toast.makeText(requireContext(), "Gagal: $err", Toast.LENGTH_LONG).show()
            }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
