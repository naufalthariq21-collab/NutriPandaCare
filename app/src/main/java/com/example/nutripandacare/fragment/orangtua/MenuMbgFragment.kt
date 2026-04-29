package com.example.nutripandacare.fragment.orangtua

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.nutripandacare.databinding.FragmentMenuMbgBinding
import com.example.nutripandacare.firebase.FirebaseHelper
import java.text.SimpleDateFormat
import java.util.*

class MenuMbgFragment : Fragment() {

    private var _binding: FragmentMenuMbgBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMenuMbgBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadMenuHariIni()
        setupClickListeners()
    }

    private fun loadMenuHariIni() {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        FirebaseHelper.getMenuHariIni(today,
            onSuccess = { data ->
                if (_binding == null) return@getMenuHariIni
                if (data != null) {
                    binding.tvMenuHariIni.text = data["nama_menu"] as? String ?: "-"
                    // Update data nutrisi lainnya jika field-nya ada di binding
                }
            },
            onError = { err ->
                Toast.makeText(requireContext(), "Gagal memuat menu: $err", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun setupClickListeners() {
        // Logika untuk menampilkan detail menu atau navigasi lainnya
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
