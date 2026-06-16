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
        // ── Tab MENUNGGU VERIFIKASI ──────────────────────────────
        // Tombol "Verifikasi" → verifikasi akun (UAT D-1)
        // Tombol "Tolak"      → tolak dengan alasan (UAT D-2)
        pendingAdapter = UserAdapter(
            userList          = pendingList,
            showVerifikasiBtn = true,
            onVerifikasiClick = { uid -> showKonfirmasiVerifikasi(uid) },
            onTolakClick      = { uid -> showTolakDialog(uid) }
        )
        binding.rvPendingList.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPendingList.adapter       = pendingAdapter

        // ── Tab SEMUA PENGGUNA ───────────────────────────────────
        // Tombol "Nonaktifkan" / "Aktifkan" → toggle status (UAT D-3)
        semuaAdapter = UserAdapter(
            userList          = semuaList,
            showVerifikasiBtn = false,
            onVerifikasiClick = {},
            onTolakClick      = { uid -> showToggleAktifDialog(uid) }
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

    // ─── LOAD DATA ──────────────────────────────────────────────────────────

    private fun loadPendingUsers() {
        FirebaseHelper.getPendaftarBaru(
            onSuccess = { list ->
                if (_binding == null) return@getPendaftarBaru
                pendingAdapter.updateData(list)
                binding.tvPendingEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE

                // Update badge jumlah di tab label
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
        // Ambil semua user yg sudah verified (aktif maupun nonaktif)
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

    // ─── UAT D-1: VERIFIKASI AKUN ───────────────────────────────────────────
    // "Status is_verified akun berubah jadi true; pengguna berpindah ke tab Semua Pengguna"

    private fun showKonfirmasiVerifikasi(uid: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Verifikasi Akun")
            .setMessage("Verifikasi akun ini? Pengguna akan bisa login ke dashboard.")
            .setPositiveButton("Verifikasi") { _, _ ->
                FirebaseHelper.verifikasiAkun(uid,
                    onSuccess = {
                        if (_binding == null) return@verifikasiAkun
                        Toast.makeText(requireContext(), "Akun berhasil diverifikasi! ✅", Toast.LENGTH_SHORT).show()
                        // Reload kedua tab — user pindah dari pending ke semua (UAT D-1)
                        loadPendingUsers()
                        loadSemuaUsers()
                        // Pindah otomatis ke tab Semua Pengguna
                        binding.tabLayout.getTabAt(1)?.select()
                    },
                    onError = { err ->
                        if (_binding == null) return@verifikasiAkun
                        Toast.makeText(requireContext(), "Gagal: $err", Toast.LENGTH_SHORT).show()
                    }
                )
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    // ─── UAT D-2: TOLAK PENDAFTARAN ─────────────────────────────────────────
    // "Status akun berubah jadi 'ditolak'; pengguna menerima dialog penolakan"

    private fun showTolakDialog(uid: String) {
        val input = EditText(requireContext()).apply {
            hint = "Alasan penolakan (opsional)"
            setPadding(48, 24, 48, 24)
        }
        AlertDialog.Builder(requireContext())
            .setTitle("Tolak Pendaftaran")
            .setMessage("Yakin ingin menolak akun ini? Tindakan ini tidak dapat dibatalkan.")
            .setView(input)
            .setPositiveButton("Tolak") { _, _ ->
                val alasan = input.text.toString().trim().ifEmpty { "Tidak memenuhi syarat" }
                FirebaseHelper.tolakAkun(uid, alasan,
                    onSuccess = {
                        if (_binding == null) return@tolakAkun
                        Toast.makeText(requireContext(), "Akun berhasil ditolak", Toast.LENGTH_SHORT).show()
                        loadPendingUsers()
                        loadSemuaUsers()
                    },
                    onError = { err ->
                        if (_binding == null) return@tolakAkun
                        Toast.makeText(requireContext(), "Gagal: $err", Toast.LENGTH_SHORT).show()
                    }
                )
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    // ─── UAT D-3: TOGGLE NONAKTIF / AKTIFKAN ────────────────────────────────
    // "Status akun berubah jadi 'nonaktif'; pengguna tidak dapat login"
    // "Aktifkan kembali akun yang nonaktif"

    private fun showToggleAktifDialog(uid: String) {
        val userData   = semuaList.find { it.first == uid }?.second
        val statusAkun = userData?.get("status_akun") as? String ?: "aktif"
        val nama       = userData?.get("nama") as? String ?: "pengguna ini"

        if (statusAkun == "nonaktif") {
            // Aktifkan kembali
            AlertDialog.Builder(requireContext())
                .setTitle("Aktifkan Akun")
                .setMessage("Aktifkan kembali akun $nama? Pengguna akan bisa login kembali.")
                .setPositiveButton("Aktifkan") { _, _ ->
                    FirebaseHelper.aktifkanAkun(uid,
                        onSuccess = {
                            if (_binding == null) return@aktifkanAkun
                            Toast.makeText(requireContext(), "Akun $nama berhasil diaktifkan ✅", Toast.LENGTH_SHORT).show()
                            loadSemuaUsers()
                        },
                        onError = { err ->
                            if (_binding == null) return@aktifkanAkun
                            Toast.makeText(requireContext(), "Gagal: $err", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
                .setNegativeButton("Batal", null)
                .show()
        } else {
            // Nonaktifkan
            AlertDialog.Builder(requireContext())
                .setTitle("Nonaktifkan Akun")
                .setMessage("Pengguna $nama tidak dapat login setelah dinonaktifkan.\n\nLanjutkan?")
                .setPositiveButton("Nonaktifkan") { _, _ ->
                    FirebaseHelper.nonaktifkanAkun(uid,
                        onSuccess = {
                            if (_binding == null) return@nonaktifkanAkun
                            Toast.makeText(requireContext(), "Akun $nama dinonaktifkan", Toast.LENGTH_SHORT).show()
                            loadSemuaUsers()
                        },
                        onError = { err ->
                            if (_binding == null) return@nonaktifkanAkun
                            Toast.makeText(requireContext(), "Gagal: $err", Toast.LENGTH_SHORT).show()
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