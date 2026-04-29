package com.example.nutripandacare.fragment.guru

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.nutripandacare.R
import com.example.nutripandacare.databinding.FragmentKirimPengumumanBinding
import com.example.nutripandacare.firebase.FirebaseHelper

class KirimPengumumanFragment : Fragment() {

    private var _binding: FragmentKirimPengumumanBinding? = null
    private val binding get() = _binding!!

    private val MAX_CHAR = 300
    private var isLoading = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentKirimPengumumanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupCharCounter()
        setupClickListeners()
        
        binding.ivBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupCharCounter() {
        binding.etPesanNotif.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val len = s?.length ?: 0
                binding.tvCharCount.text = "$len / $MAX_CHAR karakter"

                val warna = if (len > MAX_CHAR)
                    requireContext().getColor(R.color.badge_red)
                else
                    requireContext().getColor(R.color.text_hint)
                binding.tvCharCount.setTextColor(warna)
            }
        })
    }

    private fun setupClickListeners() {
        binding.btnKirim.setOnClickListener { kirimNotifikasi() }
    }

    private fun kirimNotifikasi() {
        if (isLoading) return

        val pesan = binding.etPesanNotif.text.toString().trim()

        if (pesan.isEmpty()) {
            binding.etPesanNotif.error = "Pesan tidak boleh kosong"
            return
        }
        if (pesan.length > MAX_CHAR) {
            Toast.makeText(requireContext(), "Pesan melebihi $MAX_CHAR karakter", Toast.LENGTH_SHORT).show()
            return
        }

        isLoading = true
        binding.btnKirim.isEnabled = false
        binding.btnKirim.text = "Mengirim..."

        val judulNotif = "Pesan dari Guru 📢"

        FirebaseHelper.blastNotifikasi(
            judul = judulNotif,
            isi = pesan,
            tipe = "pengumuman",
            targetRole = "orang_tua",
            onSuccess = {
                isLoading = false
                Toast.makeText(requireContext(), "Notifikasi berhasil dikirim!", Toast.LENGTH_SHORT).show()
                requireActivity().onBackPressedDispatcher.onBackPressed()
            },
            onError = { err ->
                isLoading = false
                binding.btnKirim.isEnabled = true
                binding.btnKirim.text = "📤  Kirim ke Orang Tua"
                Toast.makeText(requireContext(), "Gagal: $err", Toast.LENGTH_LONG).show()
            }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
