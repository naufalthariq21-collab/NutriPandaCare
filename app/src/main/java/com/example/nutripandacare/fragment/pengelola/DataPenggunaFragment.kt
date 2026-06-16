package com.example.nutripandacare.fragment.pengelola

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.nutripandacare.R
import com.example.nutripandacare.databinding.FragmentDataPenggunaBinding
import com.example.nutripandacare.firebase.FirebaseHelper
import com.google.android.material.tabs.TabLayout

class DataPenggunaFragment : Fragment() {

    private var _binding: FragmentDataPenggunaBinding? = null
    private val binding get() = _binding!!

    private lateinit var pendingAdapter: UserAdapter
    private lateinit var semuaAdapter: UserAdapter

    // Daftar asli untuk menampung data dari Firebase
    private var fullPendingList = listOf<Pair<String, Map<String, Any?>>>()
    private var fullSemuaList   = listOf<Pair<String, Map<String, Any?>>>()

    private var currentFilterRole = "Semua"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDataPenggunaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerViews()
        setupTabs()
        setupSearchAndFilter()
        refreshAllData()

        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun refreshAllData() {
        loadPendingUsers()
        loadSemuaUsers()
    }

    private fun setupRecyclerViews() {
        pendingAdapter = UserAdapter(
            userList          = mutableListOf(),
            showVerifikasiBtn = true,
            onVerifikasiClick = { uid -> showKonfirmasiVerifikasi(uid) },
            onTolakClick      = { uid -> showTolakDialog(uid) }
        )
        binding.rvPendingList.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPendingList.adapter       = pendingAdapter

        semuaAdapter = UserAdapter(
            userList          = mutableListOf(),
            showVerifikasiBtn = false,
            onVerifikasiClick = {},
            onTolakClick      = { uid -> showToggleAktifDialog(uid) }
        )
        binding.rvSemuaList.layoutManager = LinearLayoutManager(requireContext())
        binding.rvSemuaList.adapter       = semuaAdapter
    }

    private fun setupSearchAndFilter() {
        // Logika Pencarian Real-time
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { applyFilters() }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Logika Filter Role
        binding.btnFilter.setOnClickListener {
            val roles = arrayOf("Semua", "Guru", "Orang Tua")
            AlertDialog.Builder(requireContext())
                .setTitle("Filter Peran")
                .setItems(roles) { _, which ->
                    currentFilterRole = roles[which]
                    applyFilters()
                    Toast.makeText(context, "Filter: $currentFilterRole", Toast.LENGTH_SHORT).show()
                }
                .show()
        }
    }

    private fun applyFilters() {
        val query = binding.etSearch.text.toString().lowercase().trim()

        // Filter untuk Tab Menunggu
        val filteredPending = fullPendingList.filter { user ->
            val data  = user.second
            val nama  = (data["nama"] as? String ?: "").lowercase()
            val email = (data["email"] as? String ?: "").lowercase()
            val role  = data["role"] as? String ?: ""

            val matchQuery = nama.contains(query) || email.contains(query)
            val matchRole  = when(currentFilterRole) {
                "Guru" -> role == "guru"
                "Orang Tua" -> role == "orang_tua"
                else -> true
            }
            matchQuery && matchRole
        }
        pendingAdapter.updateData(filteredPending)
        binding.tvPendingEmpty.visibility = if (filteredPending.isEmpty()) View.VISIBLE else View.GONE

        // Filter untuk Tab Semua
        val filteredSemua = fullSemuaList.filter { user ->
            val data  = user.second
            val nama  = (data["nama"] as? String ?: "").lowercase()
            val email = (data["email"] as? String ?: "").lowercase()
            val role  = data["role"] as? String ?: ""

            val matchQuery = nama.contains(query) || email.contains(query)
            val matchRole  = when(currentFilterRole) {
                "Guru" -> role == "guru"
                "Orang Tua" -> role == "orang_tua"
                else -> true
            }
            matchQuery && matchRole
        }
        semuaAdapter.updateData(filteredSemua)
        binding.tvSemuaEmpty.visibility = if (filteredSemua.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun loadPendingUsers() {
        FirebaseHelper.getPendaftarBaru(
            onSuccess = { list ->
                if (_binding == null) return@getPendaftarBaru
                fullPendingList = list
                applyFilters() // Terapkan pencarian/filter yang mungkin sedang aktif

                val tab = binding.tabLayout.getTabAt(0)
                tab?.text = if (list.isEmpty()) "Menunggu" else "Menunggu (${list.size})"
            },
            onError = { err ->
                context?.let { Toast.makeText(it, "Gagal memuat: $err", Toast.LENGTH_SHORT).show() }
            }
        )
    }

    private fun loadSemuaUsers() {
        FirebaseHelper.getAllPengguna(
            onSuccess = { list ->
                if (_binding == null) return@getAllPengguna
                fullSemuaList = list
                applyFilters()

                val tab = binding.tabLayout.getTabAt(1)
                tab?.text = if (list.isEmpty()) "Semua Pengguna" else "Semua (${list.size})"
            },
            onError = { err ->
                context?.let { Toast.makeText(it, "Gagal memuat: $err", Toast.LENGTH_SHORT).show() }
            }
        )
    }

    private fun setupTabs() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> binding.panelPending.visibility = View.VISIBLE.also { binding.panelSemua.visibility = View.GONE }
                    1 -> binding.panelSemua.visibility = View.VISIBLE.also { binding.panelPending.visibility = View.GONE }
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    // --- DIALOGS (Verifikasi, Tolak, Toggle) tetap sama seperti sebelumnya ---
    private fun showKonfirmasiVerifikasi(uid: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Verifikasi Akun")
            .setMessage("Verifikasi akun ini? Pengguna akan bisa login ke dashboard.")
            .setPositiveButton("Verifikasi") { _, _ ->
                FirebaseHelper.verifikasiAkun(uid,
                    onSuccess = {
                        Toast.makeText(requireContext(), "Akun diverifikasi! ✅", Toast.LENGTH_SHORT).show()
                        refreshAllData()
                    },
                    onError = { Toast.makeText(requireContext(), "Gagal: $it", Toast.LENGTH_SHORT).show() }
                )
            }
            .setNegativeButton("Batal", null).show()
    }

    private fun showTolakDialog(uid: String) {
        val input = EditText(requireContext()).apply { hint = "Alasan (opsional)"; setPadding(48, 32, 48, 32) }
        AlertDialog.Builder(requireContext())
            .setTitle("Tolak Pendaftaran")
            .setView(input)
            .setPositiveButton("Tolak") { _, _ ->
                val alasan = input.text.toString().trim().ifEmpty { "Tidak memenuhi syarat" }
                FirebaseHelper.tolakAkun(uid, alasan,
                    onSuccess = { refreshAllData() },
                    onError = { Toast.makeText(requireContext(), "Gagal: $it", Toast.LENGTH_SHORT).show() }
                )
            }
            .setNegativeButton("Batal", null).show()
    }

    private fun showToggleAktifDialog(uid: String) {
        val userData = fullSemuaList.find { it.first == uid }?.second
        val status   = userData?.get("status_akun") as? String ?: "aktif"
        val nama     = userData?.get("nama") as? String ?: "Pengguna"

        if (status == "nonaktif") {
            AlertDialog.Builder(requireContext()).setTitle("Aktifkan Akun")
                .setMessage("Aktifkan kembali akun $nama?").setPositiveButton("Aktifkan") { _, _ ->
                    FirebaseHelper.aktifkanAkun(uid, onSuccess = { refreshAllData() }, onError = {})
                }.setNegativeButton("Batal", null).show()
        } else if (status == "aktif") {
            AlertDialog.Builder(requireContext()).setTitle("Nonaktifkan Akun")
                .setMessage("Nonaktifkan akun $nama?").setPositiveButton("Nonaktifkan") { _, _ ->
                    FirebaseHelper.nonaktifkanAkun(uid, onSuccess = { refreshAllData() }, onError = {})
                }.setNegativeButton("Batal", null).show()
        } else {
            showKonfirmasiVerifikasi(uid) // Untuk status 'ditolak'
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}