package com.example.nutripandacare.fragment.guru

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.nutripandacare.R
import com.example.nutripandacare.databinding.FragmentKirimPengumumanBinding
import com.example.nutripandacare.firebase.FirebaseHelper

class KirimPengumumanFragment : Fragment() {

    private var _binding: FragmentKirimPengumumanBinding? = null
    private val binding get() = _binding!!

    private val MAX_CHAR  = 300
    private var isLoading = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentKirimPengumumanBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arguments?.getString("judul_artikel")?.let { judulArtikel ->
            if (judulArtikel.isNotEmpty()) {
                binding.etPesanNotif.setText(
                    "Baca artikel terbaru: \"$judulArtikel\" di aplikasi NutriPanda Care! 📚"
                )
            }
        }

        setupCharCounter()

        binding.ivBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnKirim.setOnClickListener { kirimPengumumanKeOrtu() }
    }

    private fun setupCharCounter() {
        binding.etPesanNotif.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            @SuppressLint("SetTextI18n")
            override fun afterTextChanged(s: Editable?) {
                val len = s?.length ?: 0
                binding.tvCharCount.text = "$len / $MAX_CHAR karakter"
                binding.tvCharCount.setTextColor(
                    if (len > MAX_CHAR) requireContext().getColor(R.color.badge_red)
                    else requireContext().getColor(R.color.text_hint)
                )
            }
        })
    }

    @SuppressLint("SetTextI18n")
    private fun kirimPengumumanKeOrtu() {
        if (isLoading) return

        val pesan = binding.etPesanNotif.text.toString().trim()
        if (pesan.isEmpty()) { binding.etPesanNotif.error = "Pesan tidak boleh kosong"; return }
        if (pesan.length > MAX_CHAR) {
            Toast.makeText(requireContext(), "Pesan melebihi $MAX_CHAR karakter", Toast.LENGTH_SHORT).show()
            return
        }

        isLoading = true
        binding.btnKirim.isEnabled = false
        binding.btnKirim.text      = "Mengirim..."

        // FIX #1: Gunakan kirimPengumuman agar masuk ke list Pengumuman di Orang Tua
        FirebaseHelper.kirimPengumuman(
            judul      = "Pesan dari Guru 📢",
            isi        = pesan,
            targetRole = "orang_tua",
            onSuccess  = {
                if (_binding == null) return@kirimPengumuman
                isLoading = false
                Toast.makeText(requireContext(), "Berhasil mengirim pesan ke orang tua! ✅", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack(R.id.nav_konten_edukasi, false)
            },
            onError = { err ->
                if (_binding == null) return@kirimPengumuman
                isLoading = false
                binding.btnKirim.isEnabled = true
                binding.btnKirim.text      = "📤  Kirim ke Orang Tua"
                Toast.makeText(requireContext(), "Gagal: $err", Toast.LENGTH_LONG).show()
            }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}