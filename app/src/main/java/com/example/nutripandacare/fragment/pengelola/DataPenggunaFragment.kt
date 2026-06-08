package com.example.nutripandacare.fragment.pengelola

import android.os.Bundle
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

    private val pendingList = mutableListOf<Pair<String, Map<String, Any?>>>()
    private val semuaList   = mutableListOf<Pair<String, Map<String, Any?>>>()

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
        loadPendingUsers()
        loadSemuaUsers()

        binding.toolbar.setNavigationOnClickListener {
            if (!findNavController().navigateUp()) {
                findNavController().navigate(R.id.fragment_home_pengelola)
            }
        }
    }

    private fun setupRecyclerViews() {
        pendingAdapter = UserAdapter(
            userList          = pendingList,
            showVerifikasiBtn = true,
            onVerifikasiClick = { uid -> konfirmasiVerifikasi(uid) },
            onTolakClick      = { uid -> showTolakDialog(uid) }
        )
        binding.rvPendingList.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPendingList.adapter       = pendingAdapter

        semuaAdapter = UserAdapter(
            userList          = semuaList,
            showVerifikasiBtn = false,
            onVerifikasiClick = {},
            onTolakClick      = { uid -> konfirmasiToggleAktif(uid) }
        )
        binding.rvSemuaList.layoutManager = LinearLayoutManager(requireContext())
        binding.rvSemuaList.adapter       = semuaAdapter
    }

    private fun setupTabs() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> showPanel(pending = true)
                    1 -> showPanel(pending = false)
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
        showPanel(pending = true)
    }

    private fun showPanel(pending: Boolean) {
        binding.panelPending.visibility = if (pending) View.VISIBLE else View.GONE
        binding.panelSemua.visibility   = if (pending) View.GONE   else View.VISIBLE
    }

    // ─── LOAD DATA ───────────────────────────────────────────────

    private fun loadPendingUsers() {
        FirebaseHelper.getPendaftarBaru(
            onSuccess = { list ->
                if (_binding == null) return@getPendaftarBaru
                pendingAdapter.updateData(list)
                binding.tvPendingEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE

                val tab = binding.tabLayout.getTabAt(0)
                tab?.text = if (list.isEmpty()) "Menunggu Verifikasi"
                else "Menunggu Verifikasi (${list.size})"
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
                semuaAdapter.updateData(list)
                binding.tvSemuaEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
            },
            onError = { err ->
                context?.let { Toast.makeText(it, "Gagal memuat: $err", Toast.LENGTH_SHORT).show() }
            }
        )
    }

    // ─── AKSI ────────────────────────────────────────────────────

    private fun konfirmasiVerifikasi(uid: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Verifikasi Akun")
            .setMessage("Verifikasi akun ini? Pengguna akan bisa login ke dashboard.")
            .setPositiveButton("Verifikasi") { _, _ ->
                FirebaseHelper.verifikasiAkun(uid,
                    onSuccess = {
                        context?.let { Toast.makeText(it, "Akun berhasil diverifikasi! ✅", Toast.LENGTH_SHORT).show() }
                        loadPendingUsers()
                        loadSemuaUsers()
                    },
                    onError = { err ->
                        context?.let { Toast.makeText(it, "Gagal: $err", Toast.LENGTH_SHORT).show() }
                    }
                )
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun showTolakDialog(uid: String) {
        val input = EditText(requireContext()).apply {
            hint = "Alasan penolakan (opsional)"
            setPadding(48, 24, 48, 24)
        }
        AlertDialog.Builder(requireContext())
            .setTitle("Tolak Pendaftaran")
            .setMessage("Yakin ingin menolak akun ini?")
            .setView(input)
            .setPositiveButton("Tolak") { _, _ ->
                val alasan = input.text.toString().trim().ifEmpty { "Tidak memenuhi syarat" }
                FirebaseHelper.tolakAkun(uid, alasan,
                    onSuccess = {
                        context?.let { Toast.makeText(it, "Akun berhasil ditolak", Toast.LENGTH_SHORT).show() }
                        loadPendingUsers()
                        loadSemuaUsers()
                    },
                    onError = { err ->
                        context?.let { Toast.makeText(it, "Gagal: $err", Toast.LENGTH_SHORT).show() }
                    }
                )
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    /**
     * Toggle akun antara aktif <-> nonaktif (untuk tab Semua Pengguna)
     */
    private fun konfirmasiToggleAktif(uid: String) {
        // Cek status akun dari list yang sudah diload
        val userData = semuaList.find { it.first == uid }?.second
        val statusAkun = userData?.get("status_akun") as? String ?: "aktif"

        if (statusAkun == "nonaktif") {
            AlertDialog.Builder(requireContext())
                .setTitle("Aktifkan Akun")
                .setMessage("Aktifkan kembali akun ini?")
                .setPositiveButton("Aktifkan") { _, _ ->
                    FirebaseHelper.aktifkanAkun(uid,
                        onSuccess = {
                            context?.let { Toast.makeText(it, "Akun berhasil diaktifkan ✅", Toast.LENGTH_SHORT).show() }
                            loadSemuaUsers()
                        },
                        onError = { err ->
                            context?.let { Toast.makeText(it, "Gagal: $err", Toast.LENGTH_SHORT).show() }
                        }
                    )
                }
                .setNegativeButton("Batal", null)
                .show()
        } else {
            AlertDialog.Builder(requireContext())
                .setTitle("Nonaktifkan Akun")
                .setMessage("Pengguna tidak bisa login setelah dinonaktifkan. Lanjutkan?")
                .setPositiveButton("Nonaktifkan") { _, _ ->
                    FirebaseHelper.nonaktifkanAkun(uid,
                        onSuccess = {
                            context?.let { Toast.makeText(it, "Akun dinonaktifkan", Toast.LENGTH_SHORT).show() }
                            loadSemuaUsers()
                        },
                        onError = { err ->
                            context?.let { Toast.makeText(it, "Gagal: $err", Toast.LENGTH_SHORT).show() }
                        }
                    )
                }
                .setNegativeButton("Batal", null)
                .show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
