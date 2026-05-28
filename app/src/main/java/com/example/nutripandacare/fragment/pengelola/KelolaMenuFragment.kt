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

    override fun onResume() {
        super.onResume()
        // Reload setelah balik dari TambahMenu / EditMenu
        loadDaftarMenu()
    }

    private fun setupRecyclerView() {
        adapter = MenuAdapter(
            menuList      = menuList,
            onDeleteClick = { tanggal -> konfirmasiHapus(tanggal) },
            onEditClick   = { tanggal -> navigasiEdit(tanggal) }
        )
        binding.rvMenuList.layoutManager = LinearLayoutManager(requireContext())
        binding.rvMenuList.adapter       = adapter
    }

    private fun loadDaftarMenu() {
        binding.progressBar.visibility = View.VISIBLE
        val sdf   = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val start = sdf.format(Date())
        val cal   = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 30) }
        val end   = sdf.format(cal.time)

        FirebaseHelper.getDaftarMenu(start, end,
            onSuccess = { list ->
                if (_binding == null) return@getDaftarMenu
                binding.progressBar.visibility = View.GONE
                adapter.updateData(list)
                binding.tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
            },
            onError = { err ->
                if (_binding == null) return@getDaftarMenu
                binding.progressBar.visibility = View.GONE
                context?.let { Toast.makeText(it, "Gagal memuat menu: $err", Toast.LENGTH_SHORT).show() }
            }
        )
    }

    private fun konfirmasiHapus(tanggal: String) {
        context?.let { ctx ->
            AlertDialog.Builder(ctx)
                .setTitle("Hapus Menu")
                .setMessage("Yakin ingin menghapus menu untuk tanggal $tanggal?")
                .setPositiveButton("Hapus") { _, _ ->
                    FirebaseHelper.hapusMenuMbg(tanggal,
                        onSuccess = {
                            Toast.makeText(ctx, "Menu berhasil dihapus", Toast.LENGTH_SHORT).show()
                            loadDaftarMenu()
                        },
                        onError = { err ->
                            Toast.makeText(ctx, "Gagal hapus: $err", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
                .setNegativeButton("Batal", null)
                .show()
        }
    }

    private fun navigasiEdit(tanggal: String) {
        // Kirim tanggal sebagai argument ke TambahMenuFragment (mode edit)
        val bundle = Bundle().apply { putString("tanggal_edit", tanggal) }
        try {
            if (findNavController().currentDestination?.id == R.id.fragment_menu_mbg) {
                findNavController().navigate(
                    R.id.action_fragment_menu_mbg_to_tambahMenuFragment, bundle
                )
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Navigasi gagal: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupClickListeners() {
        binding.btnTambahMenu.setOnClickListener {
            try {
                if (findNavController().currentDestination?.id == R.id.fragment_menu_mbg) {
                    findNavController().navigate(R.id.action_fragment_menu_mbg_to_tambahMenuFragment)
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Navigasi gagal", Toast.LENGTH_SHORT).show()
            }
        }

        binding.toolbar.setNavigationOnClickListener {
            if (!findNavController().navigateUp()) {
                findNavController().navigate(R.id.fragment_home_pengelola)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}