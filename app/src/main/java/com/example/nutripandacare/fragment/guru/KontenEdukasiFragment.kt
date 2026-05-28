package com.example.nutripandacare.fragment.guru

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.nutripandacare.R
import com.example.nutripandacare.databinding.FragmentKontenEdukasiBinding
import com.example.nutripandacare.firebase.FirebaseHelper
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale

class KontenEdukasiFragment : Fragment() {

    private var _binding: FragmentKontenEdukasiBinding? = null
    private val binding get() = _binding!!

    private val daftarArtikel = mutableListOf<Pair<String, Map<String, Any?>>>()
    private lateinit var adapter: KontenAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentKontenEdukasiBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupClickListeners()
        loadArtikel()
    }

    override fun onResume() {
        super.onResume()
        loadArtikel()
    }

    private fun setupRecyclerView() {
        adapter = KontenAdapter(
            data = daftarArtikel,
            onPreview = { artikelId ->
                // Navigasi ke preview dengan kirim artikel_id sebagai argumen
                val args = bundleOf("artikel_id" to artikelId)
                findNavController().navigate(R.id.nav_preview_konten, args)
            },
            onKirimNotif = { _, judulArtikel ->
                // Kirim judul artikel sebagai pesan awal ke KirimPengumumanFragment
                val args = bundleOf("judul_artikel" to judulArtikel)
                findNavController().navigate(R.id.nav_buat_pengumuman, args)
            },
            onHapus = { artikelId ->
                konfirmasiHapus(artikelId)
            }
        )
        binding.rvKonten.layoutManager = LinearLayoutManager(requireContext())
        binding.rvKonten.adapter = adapter
    }

    private fun loadArtikel() {
        binding.progressBar.visibility = View.VISIBLE
        binding.rvKonten.visibility    = View.GONE
        binding.tvEmpty.visibility     = View.GONE

        FirebaseHelper.getArtikel(
            kategori = "Semua",
            onSuccess = { list ->
                if (_binding == null) return@getArtikel
                binding.progressBar.visibility = View.GONE

                daftarArtikel.clear()
                daftarArtikel.addAll(list)
                adapter.notifyDataSetChanged()

                if (list.isEmpty()) {
                    binding.tvEmpty.visibility  = View.VISIBLE
                    binding.rvKonten.visibility = View.GONE
                } else {
                    binding.tvEmpty.visibility  = View.GONE
                    binding.rvKonten.visibility = View.VISIBLE
                }
            },
            onError = { err ->
                if (_binding == null) return@getArtikel
                binding.progressBar.visibility = View.GONE
                Toast.makeText(requireContext(), "Gagal memuat konten: $err", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun konfirmasiHapus(artikelId: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Hapus Konten")
            .setMessage("Yakin ingin menghapus artikel ini? Tindakan ini tidak bisa dibatalkan.")
            .setPositiveButton("Hapus") { _, _ ->
                FirebaseHelper.hapusArtikel(
                    artikelId = artikelId,
                    onSuccess = {
                        Toast.makeText(requireContext(), "Artikel berhasil dihapus", Toast.LENGTH_SHORT).show()
                        loadArtikel()
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
        binding.ivBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        binding.btnTambah.setOnClickListener {
            findNavController().navigate(R.id.nav_buat_konten)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // ─────────────────────────────────────────────────
    // Inner Adapter
    // ─────────────────────────────────────────────────
    inner class KontenAdapter(
        private val data: List<Pair<String, Map<String, Any?>>>,
        private val onPreview: (artikelId: String) -> Unit,
        private val onKirimNotif: (artikelId: String, judulArtikel: String) -> Unit,
        private val onHapus: (artikelId: String) -> Unit
    ) : RecyclerView.Adapter<KontenAdapter.VH>() {

        private val sdf = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))

        inner class VH(view: View) : RecyclerView.ViewHolder(view) {
            val ivThumbnail: ImageView = view.findViewById(R.id.ivThumbnailKonten)
            val tvJudul: TextView      = view.findViewById(R.id.tvJudulKonten)
            val tvKategori: TextView   = view.findViewById(R.id.tvKategoriKonten)
            val tvWaktuMenit: TextView = view.findViewById(R.id.tvWaktuMenitKonten)
            val tvMenu: TextView       = view.findViewById(R.id.tvMenuKonten)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_konten, parent, false)
            return VH(view)
        }

        override fun getItemCount() = data.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val (artikelId, item) = data[position]

            holder.tvJudul.text    = item["judul"]    as? String ?: "-"
            holder.tvKategori.text = item["kategori"] as? String ?: "-"

            val tsPublish = item["waktu_publish"] as? Timestamp
            val tanggal   = tsPublish?.toDate()?.let { sdf.format(it) } ?: "-"
            val menitBaca = (item["menit_baca"] as? Number)?.toInt() ?: 0
            holder.tvWaktuMenit.text = "$tanggal • $menitBaca menit"

            val thumbUrl = item["thumbnail_url"] as? String ?: ""
            if (thumbUrl.isNotEmpty()) {
                Glide.with(holder.itemView.context)
                    .load(thumbUrl)
                    .placeholder(R.color.green_pastel)
                    .error(R.color.cream_medium)
                    .centerCrop()
                    .into(holder.ivThumbnail)
            } else {
                holder.ivThumbnail.setBackgroundColor(
                    requireContext().getColor(R.color.green_pastel)
                )
                holder.ivThumbnail.setImageDrawable(null)
            }

            // Tap card → preview
            holder.itemView.setOnClickListener { onPreview(artikelId) }

            // Tap menu (⋮)
            holder.tvMenu.setOnClickListener { anchor ->
                val popup = PopupMenu(anchor.context, anchor)
                popup.menu.add(0, 0, 0, "📤  Kirim Notifikasi ke Ortu")
                popup.menu.add(0, 1, 1, "🗑️  Hapus Artikel")
                popup.setOnMenuItemClickListener { menuItem ->
                    when (menuItem.itemId) {
                        0 -> {
                            val judul = item["judul"] as? String ?: ""
                            onKirimNotif(artikelId, judul)
                            true
                        }
                        1 -> {
                            onHapus(artikelId)
                            true
                        }
                        else -> false
                    }
                }
                popup.show()
            }
        }
    }
}