package com.example.nutripandacare.fragment.guru

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.nutripandacare.R
import com.example.nutripandacare.databinding.FragmentAduanMbgBinding
import com.example.nutripandacare.firebase.FirebaseHelper
import com.google.firebase.Timestamp
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

class AduanMbgFragment : Fragment() {

    private var _binding: FragmentAduanMbgBinding? = null
    private val binding get() = _binding!!

    private var userNama: String = ""
    private var isLoading = false
    private var imageUri: Uri? = null
    private var cameraImageUri: Uri? = null

    // Launcher kamera
    private val takePictureLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success && cameraImageUri != null) {
                imageUri = cameraImageUri
                binding.ivAduan.visibility         = View.VISIBLE
                binding.layoutPlaceholder.visibility = View.GONE
                binding.ivAduan.setImageURI(imageUri)
            }
        }

    // Launcher galeri
    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                imageUri = it
                binding.ivAduan.visibility         = View.VISIBLE
                binding.layoutPlaceholder.visibility = View.GONE
                binding.ivAduan.setImageURI(it)
            }
        }

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
        // Load riwayat aduan saya
        loadRiwayatAduan()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupSpinner() {
        val kategori = arrayOf("Makanan", "Pelayanan", "Kebersihan", "Lainnya")
        val adapter  = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, kategori)
        binding.spinnerKategoriAduan.adapter = adapter
    }

    private fun loadUserData() {
        val uid = FirebaseHelper.uid
        if (uid.isEmpty()) return

        FirebaseHelper.getDataUser(uid,
            onSuccess = { data -> userNama = data["nama"] as? String ?: "Guru" },
            onError   = { }
        )
    }

    private fun setupClickListeners() {
        binding.cardPilihFoto.setOnClickListener {
            showImageSourceDialog()
        }

        binding.btnKirimAduan.setOnClickListener {
            val judul    = binding.etJudulAduan.text.toString().trim()
            val isi      = binding.etIsiAduan.text.toString().trim()
            val kategori = binding.spinnerKategoriAduan.selectedItem.toString()

            if (judul.isEmpty()) { binding.etJudulAduan.error = "Judul tidak boleh kosong"; return@setOnClickListener }
            if (isi.isEmpty())   { binding.etIsiAduan.error   = "Isi aduan tidak boleh kosong"; return@setOnClickListener }

            prepareKirimAduan(judul, isi, kategori)
        }
    }

    private fun showImageSourceDialog() {
        val options = arrayOf("📷  Ambil dari Kamera", "🖼️  Pilih dari Galeri")
        AlertDialog.Builder(requireContext())
            .setTitle("Pilih Sumber Foto Bukti")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> openCamera()
                    1 -> pickImageLauncher.launch("image/*")
                }
            }
            .show()
    }

    private fun openCamera() {
        val photoFile = File(requireContext().cacheDir, "aduan_foto_${System.currentTimeMillis()}.jpg")
        cameraImageUri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            photoFile
        )
        takePictureLauncher.launch(cameraImageUri)
    }

    private fun prepareKirimAduan(judul: String, isi: String, kategori: String) {
        if (isLoading) return
        isLoading = true
        binding.btnKirimAduan.isEnabled = false
        binding.btnKirimAduan.text      = "Mengirim..."

        if (imageUri != null) {
            val fileName = "aduan/${FirebaseHelper.uid}_${System.currentTimeMillis()}.jpg"
            FirebaseHelper.uploadImage(fileName, imageUri!!,
                onSuccess = { url -> executeKirimAduan(judul, isi, kategori, url) },
                onError   = { err ->
                    isLoading = false
                    binding.btnKirimAduan.isEnabled = true
                    binding.btnKirimAduan.text      = "Kirim Aduan Sekarang"
                    Toast.makeText(requireContext(), "Gagal upload foto: $err", Toast.LENGTH_SHORT).show()
                }
            )
        } else {
            executeKirimAduan(judul, isi, kategori, "")
        }
    }

    private fun executeKirimAduan(judul: String, isi: String, kategori: String, fotoUrl: String) {
        FirebaseHelper.kirimAduan(
            judul        = judul,
            isi          = isi,
            kategori     = kategori,
            pengirimNama = userNama.ifEmpty { "Guru" },
            pengirimRole = "guru",
            fotoAduan    = fotoUrl,
            onSuccess    = {
                isLoading = false
                if (_binding == null) return@kirimAduan
                Toast.makeText(requireContext(), "Aduan berhasil terkirim ✅", Toast.LENGTH_SHORT).show()
                // Reset form
                binding.etJudulAduan.text?.clear()
                binding.etIsiAduan.text?.clear()
                binding.ivAduan.visibility          = View.GONE
                binding.layoutPlaceholder.visibility = View.VISIBLE
                imageUri = null
                binding.btnKirimAduan.isEnabled = true
                binding.btnKirimAduan.text      = "Kirim Aduan Sekarang"
                // Refresh riwayat
                loadRiwayatAduan()
            },
            onError = { err ->
                isLoading = false
                if (_binding == null) return@kirimAduan
                binding.btnKirimAduan.isEnabled = true
                binding.btnKirimAduan.text      = "Kirim Aduan Sekarang"
                Toast.makeText(requireContext(), "Gagal: $err", Toast.LENGTH_LONG).show()
            }
        )
    }

    // ─── RIWAYAT ADUAN ───────────────────────────────────────────────────────

    private fun loadRiwayatAduan() {
        FirebaseHelper.getAduanSaya(
            onSuccess = { list ->
                if (_binding == null) return@getAduanSaya
                setupRiwayatRecyclerView(list)
                binding.tvRiwayatEmpty.visibility =
                    if (list.isEmpty()) View.VISIBLE else View.GONE
            },
            onError = { /* abaikan error riwayat */ }
        )
    }

    private fun setupRiwayatRecyclerView(list: List<Pair<String, Map<String, Any?>>>) {
        val rv = binding.rvRiwayatAduan
        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = RiwayatAduanAdapter(list)
    }

    // ─── ADAPTER RIWAYAT (inner) ─────────────────────────────────────────────

    inner class RiwayatAduanAdapter(
        private val data: List<Pair<String, Map<String, Any?>>>
    ) : RecyclerView.Adapter<RiwayatAduanAdapter.VH>() {

        private val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID"))

        inner class VH(view: View) : RecyclerView.ViewHolder(view) {
            val tvJudul    : TextView  = view.findViewById(R.id.tvRiwayatJudul)
            val tvKategori : TextView  = view.findViewById(R.id.tvRiwayatKategori)
            val tvStatus   : TextView  = view.findViewById(R.id.tvRiwayatStatus)
            val tvBalasan  : TextView  = view.findViewById(R.id.tvRiwayatBalasan)
            val tvTanggal  : TextView  = view.findViewById(R.id.tvRiwayatTanggal)
            val ivFoto     : ImageView = view.findViewById(R.id.ivRiwayatFoto)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_riwayat_aduan, parent, false)
            return VH(view)
        }

        override fun getItemCount() = data.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val (_, item) = data[position]

            holder.tvJudul.text    = item["judul_aduan"]    as? String ?: "-"
            holder.tvKategori.text = item["kategori_aduan"] as? String ?: "-"

            val status = item["status_aduan"] as? String ?: "menunggu"
            holder.tvStatus.text = when (status) {
                "selesai"  -> "✅ Selesai"
                "menunggu" -> "⏳ Menunggu"
                else       -> status.replaceFirstChar { it.uppercase() }
            }
            holder.tvStatus.setTextColor(
                requireContext().getColor(
                    if (status == "selesai") R.color.green_primary else R.color.pending_text
                )
            )

            val balasan = item["balasan"] as? String ?: ""
            if (balasan.isNotEmpty()) {
                holder.tvBalasan.visibility = View.VISIBLE
                holder.tvBalasan.text       = "Balasan: $balasan"
            } else {
                holder.tvBalasan.visibility = View.GONE
            }

            val ts = item["created_at"] as? Timestamp
            holder.tvTanggal.text = ts?.toDate()?.let { sdf.format(it) } ?: "-"

            val fotoUrl = item["foto_aduan"] as? String ?: ""
            if (fotoUrl.isNotEmpty()) {
                holder.ivFoto.visibility = View.VISIBLE
                Glide.with(holder.itemView.context)
                    .load(fotoUrl)
                    .placeholder(R.color.cream_medium)
                    .into(holder.ivFoto)
            } else {
                holder.ivFoto.visibility = View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
