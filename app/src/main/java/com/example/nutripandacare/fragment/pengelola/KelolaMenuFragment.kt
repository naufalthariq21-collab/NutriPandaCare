package com.example.nutripandacare.fragment.pengelola

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
import com.example.nutripandacare.databinding.FragmentKelolaMenuBinding
import com.example.nutripandacare.firebase.FirebaseHelper
import java.text.SimpleDateFormat
import java.util.*

class KelolaMenuFragment : Fragment() {

    private var _binding: FragmentKelolaMenuBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: MenuAdapter
    private val menuList = mutableListOf<Map<String, Any?>>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentKelolaMenuBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        loadDaftarMenu()
        setupClickListeners()
    }

    private fun setupRecyclerView() {
        adapter = MenuAdapter(menuList) { tanggal ->
            konfirmasiHapus(tanggal)
        }
        binding.rvMenuList.layoutManager = LinearLayoutManager(requireContext())
        binding.rvMenuList.adapter = adapter
    }

    private fun loadDaftarMenu() {
        // Ambil menu untuk 30 hari ke depan
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val start = sdf.format(Date())
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, 30)
        val end = sdf.format(cal.time)

        FirebaseHelper.getDaftarMenu(start, end,
            onSuccess = { list ->
                if (_binding == null) return@getDaftarMenu
                adapter.updateData(list)
            },
            onError = { err ->
                Toast.makeText(requireContext(), err, Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun konfirmasiHapus(tanggal: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Hapus Menu")
            .setMessage("Yakin ingin menghapus menu untuk tanggal $tanggal?")
            .setPositiveButton("Hapus") { _, _ ->
                FirebaseHelper.hapusMenuMbg(tanggal,
                    onSuccess = {
                        Toast.makeText(requireContext(), "Menu dihapus", Toast.LENGTH_SHORT).show()
                        loadDaftarMenu()
                    },
                    onError = { err ->
                        Toast.makeText(requireContext(), "Gagal hapus: $err", Toast.LENGTH_SHORT).show()
                    }
                )
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun setupClickListeners() {
        binding.btnTambahMenu.setOnClickListener {
            findNavController().navigate(R.id.action_fragment_menu_mbg_to_tambahMenuFragment)
        }
        
        binding.toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
